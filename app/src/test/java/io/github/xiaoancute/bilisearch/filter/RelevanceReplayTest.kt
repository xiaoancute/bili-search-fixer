package io.github.xiaoancute.bilisearch.filter

import io.github.xiaoancute.bilisearch.FilterStrength
import org.junit.Assert.assertEquals
import org.junit.Test

/** Synthetic regression examples, not captures or measurements from a device. */
class RelevanceReplayTest {
    private data class Example(
        val query: String,
        val item: SearchItem,
        val loose: Boolean,
        val standard: Boolean,
        val strict: Boolean
    )

    @Test
    fun replaysExpectedDecisionsAcrossAllThreeStrengths() {
        val examples = listOf(
            Example("原神攻略", SearchItem("原神攻略合集"), true, true, true),
            Example("原神攻略", SearchItem("英雄联盟精彩集锦", highlightCount = 2), false, false, false),
            Example("原神攻略", SearchItem("原神日常", highlightCount = 2), true, false, false),
            Example("原神攻略", SearchItem("原神 新手攻略"), true, true, false),
            Example("原神攻略", SearchItem("原神 新手攻略", highlightCount = 2), true, true, true),
            Example("原神攻略", SearchItem("随便聊聊", description = "原神攻略"), true, false, false),
            Example("bad apple", SearchItem("Bad Apple!! 4K", highlightCount = 2), true, true, true),
            Example("bad apple", SearchItem("Bad Dongxi!!", highlightCount = 2), true, false, false),
            Example("bad apple", SearchItem("Apple pie recipe", highlightCount = 2), true, false, false),
            Example("bad apple", SearchItem("Apple and Bad live performance"), true, true, true),
            Example("命运石之门", SearchItem("石之门 动画解说"), true, true, false),
            Example("原神攻略 -抽卡", SearchItem("原神攻略 抽卡建议", highlightCount = 2), false, false, false),
            Example("原神攻略 #原神", SearchItem("原神 新手攻略", description = "原神", highlightCount = 2), false, false, false),
            Example("@影视飓风", SearchItem("这期随便聊聊", author = "影视飓风"), true, true, true),
            Example("原神攻略", SearchItem("原神攻略合集", goto = "ad", highlightCount = 2), false, false, false)
        )

        for (example in examples) {
            val expectations = listOf(example.loose, example.standard, example.strict)
            FilterStrength.entries.forEachIndexed { index, strength ->
                val decision = SearchFilter(threshold = strength.threshold / 100.0)
                    .evaluate(example.query, example.item)
                assertEquals(
                    "${strength.name}: ${example.query} / ${example.item.title} (${decision.reason})",
                    expectations[index], decision.keep
                )
            }
        }
    }
}
