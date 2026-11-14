package io.github.xiaoancute.bilisearch

/**
 * Matches only result endpoints. Suggestions, hot words and search-box
 * recommendations share the /search prefix and must be left untouched.
 */
object SearchUrlMatcher {

    private val RESULT_MARKERS = listOf(
        "/x/v2/search",
        "/x/web-interface/wbi/search/",
        "/x/web-interface/search/"
    )

    private val EXCLUDED_MARKERS = listOf(
        "/suggest",
        "/hot",
        "/square",
        "/default_words",
        "/recommend"
    )

    fun isSearchResult(url: String): Boolean {
        val path = url.substringBefore('?')
        if (EXCLUDED_MARKERS.any { path.contains(it) }) return false
        return RESULT_MARKERS.any { path.contains(it) }
    }
}
