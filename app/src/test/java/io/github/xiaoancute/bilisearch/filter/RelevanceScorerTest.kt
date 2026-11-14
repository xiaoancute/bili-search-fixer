package io.github.xiaoancute.bilisearch.filter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RelevanceScorerTest {

    private val scorer = RelevanceScorer()

    @Test
    fun exactTitleMatchScoresFull() {
        val score = scorer.score("原神攻略", SearchItem(title = "原神攻略合集"))
        assertEquals(1.0, score.value, 0.001)
    }

    @Test
    fun unrelatedTitleScoresZero() {
        val score = scorer.score("原神攻略", SearchItem(title = "英雄联盟精彩集锦"))
        assertEquals(0.0, score.value, 0.001)
    }

    @Test
    fun partialTitleMatchKeepsMostOfTheQuery() {
        val score = scorer.score("原神攻略", SearchItem(title = "原神 新手攻略"))
        assertEquals(2.0 / 3.0, score.value, 0.001)
    }

    @Test
    fun tagMatchCountsButWeighsLessThanTitle() {
        val score = scorer.score("原神攻略", SearchItem(title = "随便聊聊", tag = "原神,攻略"))
        assertEquals(2.0 / 3.0 * 0.8, score.value, 0.001)
    }

    @Test
    fun latinQueryMatchesNormalizedTitle() {
        val score = scorer.score("bad apple", SearchItem(title = "Bad Apple!! 4K"))
        assertEquals(1.0, score.value, 0.001)
    }

    @Test
    fun adIsAlwaysZero() {
        val score = scorer.score("原神攻略", SearchItem(title = "原神攻略合集", goto = "ad"))
        assertEquals(0.0, score.value, 0.001)
        assertEquals("ad", score.reason)
    }
}
