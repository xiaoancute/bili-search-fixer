package io.github.xiaoancute.bilisearch.filter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchDiagnosticsTest {
    @Test
    fun countsEveryRemovalEvenAfterTheTitleSampleIsFull() {
        val tracker = RemovalTracker()
        tracker.record("ad")
        tracker.record("recommendation")
        tracker.record("non_video")
        repeat(10) { tracker.record("title_partial_below_threshold", "视频 $it") }
        val diagnostics = tracker.snapshot()

        assertEquals(13, diagnostics.totalRemoved)
        assertEquals(10, diagnostics.counts[RemovalCategory.RELEVANCE])
        assertEquals(1, diagnostics.counts[RemovalCategory.AD])
        assertEquals(1, diagnostics.counts[RemovalCategory.RECOMMENDATION])
        assertEquals(1, diagnostics.counts[RemovalCategory.NON_VIDEO])
        assertEquals(5, diagnostics.samples.size)
    }

    @Test
    fun distinguishesUserPoliciesFromSearchBoxRules() {
        val reasons = listOf(
            "blocked_word:抽卡", "blocked_up:123", "min_play", "duplicate",
            "excluded:入门", "tag_missing:ai", "title_missing:摄影", "author_mismatch",
            "description_partial_below_threshold", "future_reason"
        )
        val tracker = RemovalTracker()
        reasons.forEach { tracker.record(it) }
        val diagnostics = tracker.snapshot()

        assertEquals(10, diagnostics.totalRemoved)
        assertEquals(4, diagnostics.counts[RemovalCategory.QUERY_RULE])
        assertEquals(1, diagnostics.counts[RemovalCategory.BLOCK_WORD])
        assertEquals(1, diagnostics.counts[RemovalCategory.BLOCK_UP])
        assertEquals(1, diagnostics.counts[RemovalCategory.MIN_PLAY])
        assertEquals(1, diagnostics.counts[RemovalCategory.DUPLICATE])
        assertEquals(1, diagnostics.counts[RemovalCategory.RELEVANCE])
        assertEquals(1, diagnostics.counts[RemovalCategory.OTHER])
    }

    @Test
    fun sanitizesAndBoundsTitlesWithoutLosingCounts() {
        val tracker = RemovalTracker()
        tracker.record("excluded:抽卡", "  <em class=\"keyword\">原神</em>\n攻略  ")
        tracker.record("blocked_word:" + "词".repeat(300), "长".repeat(300))
        tracker.record("ad", "   ")
        val diagnostics = tracker.snapshot()

        assertEquals(3, diagnostics.totalRemoved)
        assertEquals("原神 攻略", diagnostics.samples[0].title)
        assertEquals(SearchDiagnostics.MAX_TITLE_LENGTH, diagnostics.samples[1].title.length)
        assertEquals(SearchDiagnostics.MAX_REASON_LENGTH, diagnostics.samples[1].reason.length)
        assertEquals(2, diagnostics.samples.size)
    }

    @Test
    fun toleratesUnknownCategoriesAndMismatchedPayloadArrays() {
        val diagnostics = SearchDiagnostics.fromPayload(
            listOf("ad", "future", "relevance", "min_play", "unpaired"),
            listOf(2, 100, 3, -1),
            listOf("  原神\n攻略  ", "unpaired"),
            listOf("excluded:抽卡")
        )
        assertEquals(5, diagnostics.totalRemoved)
        assertEquals(listOf(RemovalSample("原神 攻略", "excluded:抽卡")), diagnostics.samples)
    }

    @Test
    fun snapshotDoesNotChangeWhenTrackerRecordsMoreResults() {
        val tracker = RemovalTracker()
        val before = tracker.snapshot()
        tracker.record("ad")
        assertEquals(0, before.totalRemoved)
        assertTrue(before.samples.isEmpty())
    }
}
