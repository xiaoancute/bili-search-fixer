package io.github.xiaoancute.bilisearch.filter

internal enum class RemovalCategory(val code: String) {
    AD("ad"),
    RECOMMENDATION("recommendation"),
    NON_VIDEO("non_video"),
    BLOCK_WORD("block_word"),
    BLOCK_UP("block_up"),
    MIN_PLAY("min_play"),
    DUPLICATE("duplicate"),
    QUERY_RULE("query_rule"),
    RELEVANCE("relevance"),
    OTHER("other");

    companion object {
        fun forReason(reason: String): RemovalCategory = when {
            reason == "ad" -> AD
            reason == "recommendation" -> RECOMMENDATION
            reason == "non_video" -> NON_VIDEO
            reason.startsWith("blocked_word:") -> BLOCK_WORD
            reason.startsWith("blocked_up:") -> BLOCK_UP
            reason == "min_play" -> MIN_PLAY
            reason == "duplicate" -> DUPLICATE
            reason.startsWith("excluded:") || reason.startsWith("tag_missing:") ||
                reason.startsWith("title_missing:") || reason == "author_mismatch" -> QUERY_RULE
            reason.endsWith("_below_threshold") -> RELEVANCE
            else -> OTHER
        }
    }
}

internal data class RemovalSample(val title: String, val reason: String)

/** One response's complete counts and a bounded sample of removed titles. */
internal data class SearchDiagnostics(
    val counts: Map<RemovalCategory, Int> = emptyMap(),
    val samples: List<RemovalSample> = emptyList()
) {
    val totalRemoved: Int get() = counts.values.sum()

    companion object {
        const val MAX_SAMPLES = 5
        const val MAX_TITLE_LENGTH = 120
        const val MAX_REASON_LENGTH = 160
        private val WHITESPACE = Regex("\\s+")

        fun sample(title: String, reason: String): RemovalSample = RemovalSample(
            SearchText.strip(title).replace(WHITESPACE, " ").trim().take(MAX_TITLE_LENGTH),
            reason.replace(WHITESPACE, " ").trim().take(MAX_REASON_LENGTH)
        )

        /** Also used for old/incomplete broadcasts: never zip mismatched arrays blindly. */
        fun fromPayload(
            codes: List<String>,
            counts: List<Int>,
            titles: List<String>,
            reasons: List<String>
        ): SearchDiagnostics {
            val categories = RemovalCategory.entries.associateBy { it.code }
            val validCounts = codes.zip(counts).mapNotNull { (code, count) ->
                val category = categories[code] ?: return@mapNotNull null
                if (count > 0) category to count else null
            }.toMap()
            val validSamples = titles.asSequence().zip(reasons.asSequence())
                .map { (title, reason) -> sample(title, reason) }
                .filter { it.title.isNotEmpty() && it.reason.isNotEmpty() }
                .take(MAX_SAMPLES)
                .toList()
            return SearchDiagnostics(validCounts, validSamples)
        }
    }
}

/** Counts every removal, including card types that have no video title. */
internal class RemovalTracker {
    private val counts = LinkedHashMap<RemovalCategory, Int>()
    private val samples = ArrayList<RemovalSample>(SearchDiagnostics.MAX_SAMPLES)

    fun record(reason: String, title: String? = null) {
        val category = RemovalCategory.forReason(reason)
        counts[category] = (counts[category] ?: 0) + 1
        if (title != null && samples.size < SearchDiagnostics.MAX_SAMPLES) {
            val sample = SearchDiagnostics.sample(title, reason)
            if (sample.title.isNotEmpty()) samples += sample
        }
    }

    fun snapshot(): SearchDiagnostics = SearchDiagnostics(counts.toMap(), samples.toList())
}
