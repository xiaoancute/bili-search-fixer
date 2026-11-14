package io.github.xiaoancute.bilisearch.filter

class SearchFilter(
    private val scorer: RelevanceScorer = RelevanceScorer(),
    private val threshold: Double = 0.5,
    private val dropAds: Boolean = true
) {

    fun evaluate(query: String, item: SearchItem): Decision {
        if (dropAds && item.isAd()) return Decision(false, 0.0, "ad")

        val score = scorer.score(query, item)
        val keep = score.value >= threshold
        val reason = if (keep) score.reason else score.reason + "_below_threshold"
        return Decision(keep, score.value, reason)
    }

    fun filter(query: String, items: List<SearchItem>): FilterResult {
        val kept = ArrayList<SearchItem>(items.size)
        val removed = ArrayList<RemovedItem>()

        items.forEach { item ->
            val decision = evaluate(query, item)
            if (decision.keep) kept += item else removed += RemovedItem(item, decision)
        }

        return FilterResult(kept, removed)
    }

    data class Decision(val keep: Boolean, val score: Double, val reason: String)
    data class RemovedItem(val item: SearchItem, val decision: Decision)
    data class FilterResult(val kept: List<SearchItem>, val removed: List<RemovedItem>)

    companion object {
        const val DEFAULT_THRESHOLD = 0.5
    }
}
