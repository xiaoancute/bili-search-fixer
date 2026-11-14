package io.github.xiaoancute.bilisearch

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchUrlMatcherTest {

    @Test
    fun matchesAppComprehensiveSearch() {
        assertTrue(
            SearchUrlMatcher.isSearchResult(
                "https://app.bilibili.com/x/v2/search?keyword=%E5%8E%9F%E7%A5%9E&pn=1"
            )
        )
    }

    @Test
    fun matchesTypedSearch() {
        assertTrue(
            SearchUrlMatcher.isSearchResult(
                "https://app.bilibili.com/x/v2/search/type?search_type=video&keyword=test"
            )
        )
    }

    @Test
    fun matchesWebSearch() {
        assertTrue(
            SearchUrlMatcher.isSearchResult(
                "https://api.bilibili.com/x/web-interface/wbi/search/all/v2?keyword=test"
            )
        )
    }

    @Test
    fun rejectsSuggestions() {
        assertFalse(
            SearchUrlMatcher.isSearchResult(
                "https://app.bilibili.com/x/v2/search/suggest?keyword=test"
            )
        )
    }

    @Test
    fun rejectsHotWords() {
        assertFalse(
            SearchUrlMatcher.isSearchResult(
                "https://app.bilibili.com/x/v2/search/hot?limit=20"
            )
        )
    }
}
