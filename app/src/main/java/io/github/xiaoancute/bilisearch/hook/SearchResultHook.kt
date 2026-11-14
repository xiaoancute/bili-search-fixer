package io.github.xiaoancute.bilisearch.hook

import android.util.Log
import io.github.libxposed.api.XposedInterface
import io.github.xiaoancute.bilisearch.LOG_TAG
import io.github.xiaoancute.bilisearch.SettingsRepository
import io.github.xiaoancute.bilisearch.filter.SearchFilter
import io.github.xiaoancute.bilisearch.filter.SearchItem
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
    private val settings: SettingsRepository
) {

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

        val threshold = current.threshold.coerceIn(0, 100) / 100.0
        val searchFilter = SearchFilter(threshold = threshold, dropAds = current.dropAds)
        val rule = searchFilter.parse(keyword)

        val kept = ArrayList<Any>(items.size)
        val removed = ArrayList<String>(MAX_LOGGED_TITLES)
        var removedCount = 0

        for (raw in items) {
            val item = raw ?: continue
            val case = item.method("getCardItemCase")?.toString()?.uppercase()

            if (case == "CM" || case == "PURCHASE") {
                removedCount++
                continue
            }

            if (case != "AV") {
                kept += item
                continue
            }

            val goto = item.stringMethod("getGoto")?.lowercase()
            if (goto == "ad" || goto == "ad_av") {
                removedCount++
                continue
            }

            val card = item.method("getAv")
            if (card == null) {
                kept += item
                continue
            }

            val rawTitle = card.stringMethod("getTitle").orEmpty()
            val searchItem = SearchItem(
                title = SearchText.strip(rawTitle),
                tag = null,
                description = card.stringMethod("getDesc"),
                author = card.stringMethod("getAuthor"),
                bvid = item.stringMethod("getParam"),
                goto = goto,
                play = (card.method("getPlay") as? Number)?.toLong(),
                highlightCount = SearchText.highlightCount(rawTitle)
            )

            val decision = searchFilter.evaluate(rule, searchItem)
            if (decision.keep) {
                kept += item
            } else {
                removedCount++
                if (removed.size < MAX_LOGGED_TITLES) {
                    val scoreText = "%.2f".format(decision.score)
                    removed += searchItem.title + " [" + decision.reason + " " + scoreText + "]"
                }
            }
        }

        if (removedCount == 0) return items

        xposed.log(
            Log.INFO,
            LOG_TAG,
            "search=\"" + keyword + "\" kept=" + kept.size + "/" + items.size +
                " removed=" + removedCount + " " + removed.joinToString(" | ")
        )
        return kept
    }

    private fun Any.method(name: String): Any? = runCatching {
        javaClass.methods
            .firstOrNull { it.name == name && it.parameterCount == 0 }
            ?.invoke(this)
    }.getOrNull()

    private fun Any.stringMethod(name: String): String? = method(name) as? String

    private companion object {
        const val ALL_RESPONSE = "com.bapis.bilibili.polymer.app.search.v1.SearchAllResponse"
        const val TYPE_RESPONSE = "com.bapis.bilibili.polymer.app.search.v1.SearchByTypeResponse"
        const val MAX_LOGGED_TITLES = 5
    }
}
