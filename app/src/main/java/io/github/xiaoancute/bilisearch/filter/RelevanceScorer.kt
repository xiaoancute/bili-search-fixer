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

        if (item.highlightCount > 0) return Score(0.95, "title_highlight")
        if (title.contains(normalizedQuery)) return Score(1.0, "title_exact")

        val tokens = QueryTokenizer.tokens(query).filter { it != normalizedQuery }
        if (tokens.isEmpty()) return Score(0.0, "no_tokens")

        val titleHits = tokens.count { title.contains(it) }
        val tagHits = tokens.count { tag.contains(it) }
        val descriptionHits = tokens.count { description.contains(it) }

        val titleScore = titleHits.toDouble() / tokens.size
        val tagScore = tagHits.toDouble() / tokens.size * 0.8
        val descriptionScore = descriptionHits.toDouble() / tokens.size * 0.5

        return when {
            titleScore >= tagScore && titleScore >= descriptionScore ->
                Score(titleScore, "title_partial")
            tagScore >= descriptionScore ->
                Score(tagScore, "tag_partial")
            else ->
                Score(descriptionScore, "description_partial")
        }
    }

    data class Score(val value: Double, val reason: String)
}
