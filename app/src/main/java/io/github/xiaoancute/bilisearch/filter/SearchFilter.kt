package io.github.xiaoancute.bilisearch.filter

class SearchFilter(
    private val scorer: RelevanceScorer = RelevanceScorer(),
    private val threshold: Double = 0.5,
    private val dropAds: Boolean = true
) {

    fun parse(query: String): SearchRule = QueryRuleParser.parse(query)

    fun evaluate(query: String, item: SearchItem): Decision = evaluate(parse(query), item)

    fun evaluate(rule: SearchRule, item: SearchItem): Decision {
        if (dropAds && item.isAd()) return Decision(false, 0.0, "ad")

        val title = QueryTokenizer.normalize(item.title)
        val tag = QueryTokenizer.normalize(item.tag.orEmpty())
        val author = QueryTokenizer.normalize(item.author.orEmpty())

        rule.excludedTitleTerms.firstOrNull { title.contains(it) }?.let {
            return Decision(false, 0.0, "excluded:$it")
        }
        rule.requiredTags.firstOrNull { !tag.contains(it) }?.let {
            return Decision(false, 0.0, "tag_missing:$it")
        }
        if (rule.allowedAuthors.isNotEmpty() && rule.allowedAuthors.none { author.contains(it) }) {
            return Decision(false, 0.0, "author_mismatch")
        }
        if (rule.strictTitle) {
            rule.requiredTitleTerms.firstOrNull { !title.contains(it) }?.let {
                return Decision(false, 0.0, "title_missing:$it")
            }
        }

        val score = if (rule.cleanQuery.isEmpty()) {
            RelevanceScorer.Score(1.0, "rules_only")
        } else {
            scorer.score(rule.cleanQuery, item)
        }

        val keep = score.value >= threshold
        val reason = if (keep) score.reason else score.reason + "_below_threshold"
        return Decision(keep, score.value, reason)
    }

    fun filter(query: String, items: List<SearchItem>): FilterResult {
        val rule = parse(query)
        val kept = ArrayList<SearchItem>(items.size)
        val removed = ArrayList<RemovedItem>()

        items.forEach { item ->
            val decision = evaluate(rule, item)
            if (decision.keep) kept += item else removed += RemovedItem(item, decision)
        }

        return FilterResult(kept, removed)
    }

    data class Decision(val keep: Boolean, val score: Double, val reason: String)
    data class RemovedItem(val item: SearchItem, val decision: Decision)
    data class FilterResult(val kept: List<SearchItem>, val removed: List<RemovedItem>)
}
