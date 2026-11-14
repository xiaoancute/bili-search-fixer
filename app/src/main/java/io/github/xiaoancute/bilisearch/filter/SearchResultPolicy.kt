package io.github.xiaoancute.bilisearch.filter

import io.github.xiaoancute.bilisearch.Settings

/**
 * Non-relevance gates that run before the scorer: user block lists, minimum
 * play count, dedupe and the whitelist escape hatch.
 */
internal class SearchResultPolicy {

    fun isWhitelisted(keyword: String, settings: Settings): Boolean {
        val normalized = QueryTokenizer.normalize(keyword)
        return settings.whitelist.any {
            it.isNotBlank() && normalized.contains(QueryTokenizer.normalize(it))
        }
    }

    fun dropReason(item: SearchItem, settings: Settings, seen: MutableSet<String>): String? {
        val title = QueryTokenizer.normalize(item.title)
        val author = QueryTokenizer.normalize(item.author.orEmpty())

        settings.blockWords.firstOrNull {
            it.isNotBlank() && title.contains(QueryTokenizer.normalize(it))
        }?.let { return "blocked_word:" + it }

        settings.blockUps.firstOrNull { entry ->
            val value = entry.trim()
            if (value.isEmpty()) {
                false
            } else if (value.all { it.isDigit() }) {
                item.mid?.toString() == value
            } else {
                author.contains(QueryTokenizer.normalize(value))
            }
        }?.let { return "blocked_up:" + it }

        if (settings.minPlay > 0 && (item.play ?: 0L) < settings.minPlay.toLong()) {
            return "min_play"
        }

        if (settings.dedupe) {
            val key = title + "|" + author
            if (!seen.add(key)) return "duplicate"
        }

        return null
    }

    data class ScoredItem(val item: Any, val score: Double, val play: Long)

    fun rerank(items: List<ScoredItem>): List<ScoredItem> =
        items.sortedWith(
            compareByDescending<ScoredItem> { it.score }
                .thenByDescending { it.play }
        )
}
