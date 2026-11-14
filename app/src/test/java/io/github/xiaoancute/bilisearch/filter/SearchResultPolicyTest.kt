package io.github.xiaoancute.bilisearch.filter

import io.github.xiaoancute.bilisearch.Settings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchResultPolicyTest {

    private val policy = SearchResultPolicy()

    @Test
    fun blocksWordsInTitle() {
        val reason = policy.dropReason(
            SearchItem(title = "原神攻略 抽卡建议"),
            Settings(blockWords = listOf("抽卡")),
            mutableSetOf()
        )
        assertEquals("blocked_word:抽卡", reason)
    }

    @Test
    fun blocksUploaderByNameOrMid() {
        val byName = policy.dropReason(
            SearchItem(title = "原神攻略", author = "影视飓风", mid = 123L),
            Settings(blockUps = listOf("影视飓风")),
            mutableSetOf()
        )
        val byMid = policy.dropReason(
            SearchItem(title = "原神攻略", author = "影视飓风", mid = 123L),
            Settings(blockUps = listOf("123")),
            mutableSetOf()
        )

        assertEquals("blocked_up:影视飓风", byName)
        assertEquals("blocked_up:123", byMid)
    }

    @Test
    fun dropsBelowMinimumPlayCount() {
        val reason = policy.dropReason(
            SearchItem(title = "原神攻略", play = 999L),
            Settings(minPlay = 1000),
            mutableSetOf()
        )
        assertEquals("min_play", reason)
    }

    @Test
    fun dedupesSameTitleAndAuthor() {
        val seen = mutableSetOf<String>()
        val settings = Settings(dedupe = true)
        val item = SearchItem(title = "原神攻略", author = "同一个UP")

        assertNull(policy.dropReason(item, settings, seen))
        assertEquals("duplicate", policy.dropReason(item, settings, seen))
    }

    @Test
    fun whitelistSkipsFiltering() {
        val settings = Settings(whitelist = listOf("教程"))
        assertTrue(policy.isWhitelisted("原神 教程", settings))
        assertFalse(policy.isWhitelisted("原神 攻略", settings))
    }

    @Test
    fun reranksByScoreThenPlay() {
        val lowScoreHighPlay = SearchResultPolicy.ScoredItem("a", 0.5, 1_000_000)
        val highScoreLowPlay = SearchResultPolicy.ScoredItem("b", 0.9, 10)
        val highScoreHighPlay = SearchResultPolicy.ScoredItem("c", 0.9, 500_000)

        val ranked = policy.rerank(listOf(lowScoreHighPlay, highScoreLowPlay, highScoreHighPlay))

        assertEquals(listOf("c", "b", "a"), ranked.map { it.item })
    }
}
