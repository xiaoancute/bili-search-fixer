package io.github.xiaoancute.bilisearch.filter

import io.github.xiaoancute.bilisearch.Settings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchRulePreviewTest {
    @Test
    fun detectsWhitelistBlockWordConflictAndUploaderMatch() {
        val preview = SearchRulePreview.evaluate(
            keyword = "榨菜肉丝面",
            title = "榨菜肉丝面探店",
            uploaderOrMid = "12345",
            settings = Settings(
                blockWords = listOf("肉丝", "!!!"),
                blockUps = listOf("12345"),
                whitelist = listOf("肉丝面")
            )
        )

        assertEquals("肉丝面", preview.whitelistMatch)
        assertEquals("肉丝", preview.blockedWordMatch)
        assertEquals("12345", preview.blockedUploaderMatch)
        assertEquals(listOf("肉丝"), preview.queryConflicts)
        assertEquals(listOf("!!!"), preview.invalidBlockWords)
        assertTrue(preview.filtersBypassed)
    }

    @Test
    fun ignoresPunctuationOnlyWhitelistAndBlockWords() {
        val preview = SearchRulePreview.evaluate(
            keyword = "肉丝面",
            title = "榨菜肉丝面",
            uploaderOrMid = "",
            settings = Settings(blockWords = listOf("!!!"), whitelist = listOf("###"))
        )

        assertEquals(null, preview.whitelistMatch)
        assertEquals(null, preview.blockedWordMatch)
        assertEquals(listOf("!!!"), preview.invalidBlockWords)
        assertEquals(listOf("###"), preview.invalidWhitelist)
        assertFalse(preview.filtersBypassed)
    }

    @Test
    fun previewsMatchTheRuntimeUploaderRuleForNamesAndNumericIds() {
        val byName = SearchRulePreview.evaluate(
            "视频", "标题", "影视飓风官方频道", Settings(blockUps = listOf("影视 飓风"))
        )
        val byMid = SearchRulePreview.evaluate(
            "视频", "标题", "123", Settings(blockUps = listOf("123"))
        )
        assertEquals("影视 飓风", byName.blockedUploaderMatch)
        assertEquals("123", byMid.blockedUploaderMatch)
    }

    @Test
    fun previewsFullWidthMidLikeRuntime() {
        val preview = SearchRulePreview.evaluate(
            "视频", "标题", "１２３", Settings(blockUps = listOf("123"))
        )
        assertEquals("123", preview.blockedUploaderMatch)
    }

    @Test
    fun doesNotTreatTextualNamesContainingDigitsAsUploaderIdsUnlessEntireEntryIsNumeric() {
        val preview = SearchRulePreview.evaluate(
            "视频", "标题", "up123", Settings(blockUps = listOf("up123"))
        )
        assertEquals("up123", preview.blockedUploaderMatch)
    }
}
