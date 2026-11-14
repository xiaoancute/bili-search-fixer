package io.github.xiaoancute.bilisearch.filter

/**
 * Parsed form of a query that may contain the community filter syntax:
 *
 *   摄影 -入门 #ai @影视飓风
 *
 * plain terms are required title terms, "-" excludes, "#" requires a tag,
 * "@" allows any of the listed uploaders.
 */
data class SearchRule(
    val rawQuery: String,
    val cleanQuery: String,
    val requiredTitleTerms: List<String>,
    val excludedTitleTerms: List<String>,
    val requiredTags: List<String>,
    val allowedAuthors: List<String>,
    val hasModifiers: Boolean
) {
    /**
     * Strict "all plain terms must appear in the title" only applies when the
     * user opted into rule mode. Plain queries keep the tolerant bigram
     * scorer instead.
     */
    val strictTitle: Boolean get() = hasModifiers && requiredTitleTerms.isNotEmpty()
}
