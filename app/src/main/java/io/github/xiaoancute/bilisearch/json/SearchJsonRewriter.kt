package io.github.xiaoancute.bilisearch.json

import io.github.xiaoancute.bilisearch.filter.SearchFilter
import io.github.xiaoancute.bilisearch.filter.SearchItem
import io.github.xiaoancute.bilisearch.filter.SearchRule
import org.json.JSONArray
import org.json.JSONObject

/**
 * Walks any search response and rewrites arrays whose elements look like
 * search cards. The shape varies between /x/v2/search, /x/v2/search/type and
 * the web all/v2 endpoint, so no fixed path such as data.item is assumed.
 */
object SearchJsonRewriter {

    private val EM_TAG = Regex("</?em[^>]*>", RegexOption.IGNORE_CASE)
    private val AUTHOR_KEYS = arrayOf("author", "up_name", "uname")

    fun rewrite(query: String, rawJson: String, filter: SearchFilter): Result {
        val root = JSONObject(rawJson)
        val stats = Stats()
        val rule = filter.parse(query)
        val rewritten = rewriteValue(root, rule, filter, stats) as JSONObject
        return Result(
            json = if (stats.removed == 0) rawJson else rewritten.toString(),
            kept = stats.kept,
            removed = stats.removed,
            removedTitles = stats.removedTitles
        )
    }

    private fun rewriteValue(
        value: Any?,
        rule: SearchRule,
        filter: SearchFilter,
        stats: Stats
    ): Any? {
        return when (value) {
            is JSONArray -> rewriteArray(value, rule, filter, stats)
            is JSONObject -> {
                val out = JSONObject()
                val keys = value.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    out.put(key, rewriteValue(value.opt(key), rule, filter, stats))
                }
                out
            }
            else -> value
        }
    }

    private fun rewriteArray(
        array: JSONArray,
        rule: SearchRule,
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
                val decision = filter.evaluate(rule, item)
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
                out.put(rewriteValue(element, rule, filter, stats))
            }
        }
        return out
    }

    private fun toSearchItem(obj: JSONObject): SearchItem {
        val rawTitle = obj.optString("title", "")
        return SearchItem(
            title = EM_TAG.replace(rawTitle, ""),
            tag = readTag(obj),
            description = obj.optString("description", "").ifEmpty { null },
            author = readAuthor(obj),
            bvid = obj.optString("bvid", "").ifEmpty { null },
            goto = obj.optString("goto", "").ifEmpty { null },
            cardType = obj.optString("card_type", "").ifEmpty { null },
            cardGoto = obj.optString("card_goto", "").ifEmpty { null },
            play = obj.optLong("play", -1L).takeIf { it >= 0 },
            pubdate = obj.optLong("pubdate", -1L).takeIf { it >= 0 },
            highlightCount = EM_TAG.findAll(rawTitle).count()
        )
    }

    private fun readTag(obj: JSONObject): String? {
        val tag = obj.optString("tag", "")
        if (tag.isNotEmpty()) return tag

        val tags = obj.optJSONArray("tags") ?: return null
        val values = ArrayList<String>(tags.length())
        for (index in 0 until tags.length()) {
            val value = tags.optString(index, "")
            if (value.isNotEmpty()) values += value
        }
        return values.joinToString(",").ifEmpty { null }
    }

    private fun readAuthor(obj: JSONObject): String? {
        for (key in AUTHOR_KEYS) {
            val value = obj.optString(key, "")
            if (value.isNotEmpty()) return value
        }
        return null
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
