package io.github.xiaoancute.bilisearch.hook

import android.util.Log
import io.github.libxposed.api.XposedInterface
import io.github.xiaoancute.bilisearch.LOG_TAG
import io.github.xiaoancute.bilisearch.SettingsRepository
import io.github.xiaoancute.bilisearch.StatusWriter
import io.github.xiaoancute.bilisearch.filter.SearchFilter
import io.github.xiaoancute.bilisearch.filter.SearchItem
import io.github.xiaoancute.bilisearch.filter.SearchResultPolicy
import io.github.xiaoancute.bilisearch.filter.SearchText

/**
 * Bilibili 9.0+ delivers search results as Moss gRPC protobuf messages.
 * SearchAllResponse is the comprehensive tab, SearchByTypeResponse is the
 * typed tab (video / user / live / ...). Both expose a List<Item> getter,
 * which is the only place we touch.
 */
internal class SearchResultHook(
    private val xposed: XposedInterface,
    private val classLoader: ClassLoader,
    private val settings: SettingsRepository,
    private val status: StatusWriter
) {

    private val policy = SearchResultPolicy()

    fun install(): Int {
        var installed = 0
        installed += hookGetter(ALL_RESPONSE, "getItemList")
        installed += hookGetter(TYPE_RESPONSE, "getItemsList")
        return installed
    }

    private fun hookGetter(className: String, getterName: String): Int {
        val responseClass = runCatching {
            Class.forName(className, false, classLoader)
        }.getOrNull() ?: run {
            xposed.log(Log.WARN, LOG_TAG, "class not found: " + className)
            return 0
        }

        val getter = responseClass.methods.firstOrNull {
            it.name == getterName && it.parameterCount == 0
        } ?: run {
            xposed.log(Log.WARN, LOG_TAG, "getter not found: " + className + "." + getterName)
            return 0
        }

        xposed.hook(getter)
            .setExceptionMode(XposedInterface.ExceptionMode.PASSTHROUGH)
            .intercept { chain ->
                val result = chain.proceed()
                val items = result as? List<*> ?: return@intercept result
                val response = chain.thisObject ?: return@intercept result
                filter(response, items)
            }

        xposed.log(Log.INFO, LOG_TAG, "hooked " + className + "." + getterName)
        return 1
    }

    private fun filter(response: Any, items: List<*>): List<*> {
        val current = settings.current()
        if (!current.enabled) return items

        val keyword = response.stringMethod("getKeyword")?.trim().orEmpty()
        if (keyword.isEmpty()) return items
        if (policy.isWhitelisted(keyword, current)) return items

        val threshold = current.threshold.coerceIn(0, 100) / 100.0
        val searchFilter = SearchFilter(threshold = threshold, dropAds = current.dropAds)
        val rule = searchFilter.parse(keyword)
        val seen = HashSet<String>()

        val slots = ArrayList<Slot>(items.size)
        val removed = ArrayList<String>(MAX_LOGGED_TITLES)
        var removedCount = 0

        for (raw in items) {
            val item = raw ?: continue
            val case = item.method("getCardItemCase")?.toString()?.uppercase()

            if (current.dropAds && (case == "CM" || case == "PURCHASE")) {
                removedCount++
                continue
            }

            if (current.hideRecommend && case != null && case in RECOMMEND_CASES) {
                removedCount++
                continue
            }

            if (current.videoOnly && case != "AV") {
                removedCount++
                continue
            }

            if (case != "AV") {
                slots += Slot(item, false, 0.0, 0L)
                continue
            }

            val goto = item.stringMethod("getGoto")?.lowercase()
            if (current.dropAds && (goto == "ad" || goto == "ad_av")) {
                removedCount++
                continue
            }

            val card = item.method("getAv")
            if (card == null) {
                slots += Slot(item, true, 0.0, 0L)
                continue
            }

            val rawTitle = card.stringMethod("getTitle").orEmpty()
            val searchItem = SearchItem(
                title = SearchText.strip(rawTitle),
                tag = null,
                description = card.stringMethod("getDesc"),
                author = card.stringMethod("getAuthor"),
                mid = (card.method("getMid") as? Number)?.toLong(),
                bvid = item.stringMethod("getParam"),
                goto = goto,
                play = (card.method("getPlay") as? Number)?.toLong(),
                highlightCount = SearchText.highlightCount(rawTitle)
            )

            val policyReason = policy.dropReason(searchItem, current, seen)
            if (policyReason != null) {
                removedCount++
                if (removed.size < MAX_LOGGED_TITLES) {
                    removed += searchItem.title + " [" + policyReason + "]"
                }
                continue
            }

            val decision = searchFilter.evaluate(rule, searchItem)
            if (decision.keep) {
                slots += Slot(item, true, decision.score, searchItem.play ?: 0L)
            } else {
                removedCount++
                if (removed.size < MAX_LOGGED_TITLES) {
                    val scoreText = "%.2f".format(decision.score)
                    removed += searchItem.title + " [" + decision.reason + " " + scoreText + "]"
                }
            }
        }

        status.search(keyword, slots.size, items.size, removedCount)

        if (removedCount == 0 && !current.rerank) return items

        if (current.rerank) {
            val videoIndices = slots.indices.filter { slots[it].isVideo }
            val ranked = policy.rerank(
                videoIndices.map { index ->
                    SearchResultPolicy.ScoredItem(
                        slots[index].item,
                        slots[index].score,
                        slots[index].play
                    )
                }
            )
            videoIndices.forEachIndexed { position, index ->
                slots[index] = slots[index].copy(item = ranked[position].item)
            }
        }

        if (removedCount > 0) {
            xposed.log(
                Log.INFO,
                LOG_TAG,
                "search=\"" + keyword + "\" kept=" + slots.size + "/" + items.size +
                    " removed=" + removedCount + " " + removed.joinToString(" | ")
            )
        }

        return slots.map { it.item }
    }

    private fun Any.method(name: String): Any? = runCatching {
        javaClass.methods
            .firstOrNull { it.name == name && it.parameterCount == 0 }
            ?.invoke(this)
    }.getOrNull()

    private fun Any.stringMethod(name: String): String? = method(name) as? String

    private data class Slot(
        val item: Any,
        val isVideo: Boolean,
        val score: Double,
        val play: Long
    )

    private companion object {
        const val ALL_RESPONSE = "com.bapis.bilibili.polymer.app.search.v1.SearchAllResponse"
        const val TYPE_RESPONSE = "com.bapis.bilibili.polymer.app.search.v1.SearchByTypeResponse"
        const val MAX_LOGGED_TITLES = 5

        val RECOMMEND_CASES = setOf(
            "RECOMMEND_WORD",
            "SUGGEST_KEYWORD",
            "TIPS",
            "RECOMMEND_TIPS",
            "FIND_MORE",
            "SPECIAL_GUIDE"
        )
    }
}