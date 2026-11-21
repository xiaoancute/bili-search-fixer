package io.github.xiaoancute.bilisearch.filter

/**
 * Text-only relevance. UP name is intentionally excluded: matching the
 * uploader says nothing about whether the video matches the query.
 */
class RelevanceScorer {

    fun score(query: String, item: SearchItem): Score {
        val normalizedQuery = QueryTokenizer.normalize(query)
        if (normalizedQuery.isEmpty()) return Score(1.0, "empty_query")
        if (item.isAd()) return Score(0.0, "ad")

        val title = QueryTokenizer.normalize(item.title)
        val tag = QueryTokenizer.normalize(item.tag.orEmpty())
        val description = QueryTokenizer.normalize(item.description.orEmpty())

        if (title.contains(normalizedQuery)) return Score(1.0, "title_exact")

        val tokens = QueryTokenizer.tokens(query).filter { it != normalizedQuery }
        if (tokens.isEmpty()) return Score(0.0, "no_tokens")

        val titleHits = tokens.count { title.contains(it) }
        val tagHits = tokens.count { tag.contains(it) }
        val descriptionHits = tokens.count { description.contains(it) }

        val latinOnly = tokens.all { token ->
            token.all { it in 'a'..'z' || it in '0'..'9' }
        }
        val rawTitleScore = titleHits.toDouble() / tokens.size
        val baseTitleScore = if (latinOnly && titleHits < tokens.size) {
            rawTitleScore * 0.6
        } else {
            rawTitleScore
        }
        // Server highlighting is supporting evidence, not a guarantee that
        // the title covers the whole query. Its bonus scales with coverage;
        // repeated highlights cannot promote a single hit into an exact match.
        val highlightBonus = if (item.highlightCount > 0) rawTitleScore * 0.1 else 0.0
        val titleScore = (baseTitleScore + highlightBonus).coerceAtMost(1.0)
        val tagScore = tagHits.toDouble() / tokens.size * 0.8
        // Description-only matches are a loose-mode fallback. They must not
        // reach the standard (0.5) or strict (0.7) thresholds.
        val descriptionScore = descriptionHits.toDouble() / tokens.size * 0.45

        return when {
            titleScore >= tagScore && titleScore >= descriptionScore ->
                Score(titleScore, if (highlightBonus > 0) "title_partial_highlight" else "title_partial")
            tagScore >= descriptionScore ->
                Score(tagScore, "tag_partial")
            else ->
                Score(descriptionScore, "description_partial")
        }
    }

    data class Score(val value: Double, val reason: String)
}
