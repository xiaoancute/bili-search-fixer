package io.github.xiaoancute.bilisearch.json

import io.github.xiaoancute.bilisearch.filter.SearchFilter
import io.github.xiaoancute.bilisearch.filter.SearchItem
import org.json.JSONArray
import org.json.JSONObject

/**
 * Walks any search response and rewrites arrays whose elements look like
 * search cards. The shape varies between /x/v2/search, /x/v2/search/type and
 * the web all/v2 endpoint, so no fixed path such as data.item is assumed.
 */
object SearchJsonRewriter {

    private val EM_TAG = Regex("</?em[^>]*>", RegexOption.IGNORE_CASE)

    fun rewrite(query: String, rawJson: String, filter: SearchFilter): Result {
        val root = JSONObject(rawJson)
        val stats = Stats()
        val rewritten = rewriteValue(root, query, filter, stats) as JSONObject
        return Result(
            json = if (stats.removed == 0) rawJson else rewritten.toString(),
            kept = stats.kept,
            removed = stats.removed,
            removedTitles = stats.removedTitles
        )
    }

    private fun rewriteValue(value: Any?, query: String, filter: SearchFilter, stats: Stats): Any? {
        return when (value) {
            is JSONArray -> rewriteArray(value, query, filter, stats)
            is JSONObject -> {
                val out = JSONObject()
                val keys = value.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    out.put(key, rewriteValue(value.opt(key), query, filter, stats))
                }
                out
            }
            else -> value
        }
    }

    private fun rewriteArray(
        array: JSONArray,
        query: String,
        filter: SearchFilter,
        stats: Stats
    ): JSONArray {
        val isItemArray = (0 until array.length()).any { index ->
            val obj = array.optJSONObject(index) ?: return@any false
            obj.has("title") &&
                (obj.has("bvid") || obj.has("param") || obj.has("goto") || obj.has("linktype"))
        }

        val out = JSONArray()
        for (index in 0 until array.length()) {
            val element = array.opt(index)
            if (isItemArray && element is JSONObject) {
                val item = toSearchItem(element)
                val decision = filter.evaluate(query, item)
                if (decision.keep) {
                    stats.kept++
                    out.put(element)
                } else {
                    stats.removed++
                    if (stats.removedTitles.size < MAX_LOGGED_TITLES) {
                        stats.removedTitles += item.title
                    }
                }
            } else {
                out.put(rewriteValue(element, query, filter, stats))
            }
        }
        return out
    }

    private fun toSearchItem(obj: JSONObject): SearchItem {
        val rawTitle = obj.optString("title", "")
        return SearchItem(
            title = EM_TAG.replace(rawTitle, ""),
            tag = obj.optString("tag", "").ifEmpty { null },
            description = obj.optString("description", "").ifEmpty { null },
            bvid = obj.optString("bvid", "").ifEmpty { null },
            goto = obj.optString("goto", "").ifEmpty { null },
            cardType = obj.optString("card_type", "").ifEmpty { null },
            cardGoto = obj.optString("card_goto", "").ifEmpty { null },
            play = obj.optLong("play", -1L).takeIf { it >= 0 },
            pubdate = obj.optLong("pubdate", -1L).takeIf { it >= 0 },
            highlightCount = EM_TAG.findAll(rawTitle).count()
        )
    }

    data class Result(
        val json: String,
        val kept: Int,
        val removed: Int,
        val removedTitles: List<String>
    )

    private class Stats {
        var kept = 0
        var removed = 0
        val removedTitles = mutableListOf<String>()
    }

    private const val MAX_LOGGED_TITLES = 10
}
