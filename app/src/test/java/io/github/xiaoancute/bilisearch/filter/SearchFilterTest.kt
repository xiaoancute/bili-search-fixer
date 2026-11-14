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

    @Test
    fun excludedTermDropsEvenWhenEverythingElseMatches() {
        val decision = filter.evaluate(
            "原神攻略 -抽卡",
            SearchItem(title = "原神攻略 抽卡建议")
        )
        assertFalse(decision.keep)
        assertEquals("excluded:抽卡", decision.reason)
    }

    @Test
    fun requiredTagMustBePresent() {
        val item = SearchItem(title = "原神攻略", tag = "原神,攻略")

        assertTrue(filter.evaluate("原神攻略 #原神", item).keep)
        assertFalse(filter.evaluate("原神攻略 #崩坏", item).keep)
    }

    @Test
    fun authorMustMatchAnyAllowedName() {
        val item = SearchItem(title = "原神攻略", author = "影视飓风")

        assertTrue(filter.evaluate("原神攻略 @影视飓风 @其他", item).keep)
        assertFalse(filter.evaluate("原神攻略 @其他", item).keep)
    }

    @Test
    fun strictTitleOnlyAppliesWhenModifiersArePresent() {
        val item = SearchItem(title = "原神 新手攻略", tag = "原神")

        assertTrue(filter.evaluate("原神攻略", item).keep)
        assertFalse(filter.evaluate("原神攻略 #原神", item).keep)
    }

    @Test
    fun rulesOnlyQueryStillKeepsEverythingElse() {
        val decision = filter.evaluate("-抽卡", SearchItem(title = "随便聊聊"))
        assertTrue(decision.keep)
        assertEquals("rules_only", decision.reason)
    }
    @Test
    fun tagRuleFallsBackToDescriptionWhenTagsAreMissing() {
        val item = SearchItem(title = "原神攻略", description = "这期聊聊原神")

        assertTrue(filter.evaluate("原神攻略 #原神", item).keep)
        assertFalse(filter.evaluate("原神攻略 #崩坏", item).keep)
    }
}
