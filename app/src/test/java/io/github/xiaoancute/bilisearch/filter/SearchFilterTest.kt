package io.github.xiaoancute.bilisearch.filter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchFilterTest {

    private val filter = SearchFilter()

    @Test
    fun keepsRelevantAndDropsTheRest() {
        val result = filter.filter(
            "原神攻略",
            listOf(
                SearchItem(title = "原神攻略合集"),
                SearchItem(title = "英雄联盟精彩集锦")
            )
        )

        assertEquals(1, result.kept.size)
        assertEquals(1, result.removed.size)
        assertEquals("原神攻略合集", result.kept.single().title)
    }

    @Test
    fun adsAreDroppedEvenWhenTheTitleMatches() {
        val decision = filter.evaluate(
            "原神攻略",
            SearchItem(title = "原神攻略合集", goto = "ad")
        )
        assertFalse(decision.keep)
        assertEquals("ad", decision.reason)
    }

    @Test
    fun emptyQueryKeepsEverything() {
        val decision = filter.evaluate("   ", SearchItem(title = "任何视频"))
        assertTrue(decision.keep)
    }
}
