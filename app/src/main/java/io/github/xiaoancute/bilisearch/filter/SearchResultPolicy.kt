package io.github.xiaoancute.bilisearch.filter

import io.github.xiaoancute.bilisearch.Settings

/**
 * Non-relevance gates that run before the scorer: user block lists, minimum
 * play count, dedupe and the whitelist escape hatch.
 */
internal class SearchResultPolicy {

    fun isWhitelisted(keyword: String, settings: Settings): Boolean {
        return whitelistMatch(keyword, settings) != null
    }

    fun whitelistMatch(keyword: String, settings: Settings): String? {
        val normalized = QueryTokenizer.normalize(keyword)
        return settings.whitelist.firstOrNull { entry ->
            val token = QueryTokenizer.normalize(entry)
            token.isNotEmpty() && normalized.contains(token)
        }
    }

    fun blockedWordMatch(title: String, settings: Settings): String? {
        val normalizedTitle = QueryTokenizer.normalize(title)
        return settings.blockWords.firstOrNull { entry ->
            val token = QueryTokenizer.normalize(entry)
            token.isNotEmpty() && normalizedTitle.contains(token)
        }
    }

    fun blockedUploaderMatch(uploader: String?, mid: Long?, settings: Settings): String? {
        val normalizedAuthor = QueryTokenizer.normalize(uploader.orEmpty())
        return settings.blockUps.firstOrNull { entry ->
            val value = QueryTokenizer.normalize(entry)
            val isMid = QueryTokenizer.isDecimalIdentifier(entry)
            when {
                value.isEmpty() -> false
                isMid -> mid?.let {
                    QueryTokenizer.normalizeDecimalIdentifier(it.toString()) ==
                        QueryTokenizer.normalizeDecimalIdentifier(value)
                } == true
                else -> normalizedAuthor.contains(value)
            }
        }
    }

    fun dropReason(item: SearchItem, settings: Settings, seen: MutableSet<String>): String? {
        blockedWordMatch(item.title, settings)?.let { return "blocked_word:" + it }

        blockedUploaderMatch(item.author, item.mid, settings)?.let { return "blocked_up:" + it }

        if (settings.minPlay > 0 && (item.play ?: 0L) < settings.minPlay.toLong()) {
            return "min_play"
        }

        if (settings.dedupe) {
            val key = QueryTokenizer.normalize(item.title) + "|" +
                QueryTokenizer.normalize(item.author.orEmpty())
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
