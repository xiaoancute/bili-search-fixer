package io.github.xiaoancute.bilisearch.filter

import io.github.xiaoancute.bilisearch.Settings

internal data class SearchRulePreview(
    val whitelistMatch: String?,
    val blockedWordMatch: String?,
    val blockedUploaderMatch: String?,
    val queryConflicts: List<String>,
    val invalidBlockWords: List<String>,
    val invalidBlockUps: List<String>,
    val invalidWhitelist: List<String>
) {
    val filtersBypassed: Boolean get() = whitelistMatch != null

    companion object {
        fun evaluate(
            keyword: String,
            title: String,
            uploaderOrMid: String,
            settings: Settings
        ): SearchRulePreview {
            val normalizedKeyword = QueryTokenizer.normalize(keyword)
            val invalidBlockWords = invalidEntries(settings.blockWords)
            val invalidBlockUps = invalidEntries(settings.blockUps)
            val invalidWhitelist = invalidEntries(settings.whitelist)
            val policy = SearchResultPolicy()

            return SearchRulePreview(
                whitelistMatch = policy.whitelistMatch(keyword, settings),
                blockedWordMatch = policy.blockedWordMatch(title, settings),
                blockedUploaderMatch = policy.blockedUploaderMatch(
                    uploaderOrMid.takeUnless { QueryTokenizer.isDecimalIdentifier(it) },
                    uploaderOrMid.trim()
                        .takeIf { QueryTokenizer.isDecimalIdentifier(it) }
                        ?.let { QueryTokenizer.normalizeDecimalIdentifier(it).toLongOrNull() },
                    settings
                ),
                queryConflicts = settings.blockWords.filter { entry ->
                    val token = QueryTokenizer.normalize(entry)
                    token.isNotEmpty() && normalizedKeyword.contains(token)
                },
                invalidBlockWords = invalidBlockWords,
                invalidBlockUps = invalidBlockUps,
                invalidWhitelist = invalidWhitelist
            )
        }

        private fun invalidEntries(entries: List<String>): List<String> = entries.filter {
            QueryTokenizer.normalize(it).isEmpty()
        }
    }
}
