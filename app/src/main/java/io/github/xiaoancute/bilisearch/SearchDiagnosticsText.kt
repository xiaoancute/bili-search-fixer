package io.github.xiaoancute.bilisearch

import io.github.xiaoancute.bilisearch.filter.RemovalCategory
import io.github.xiaoancute.bilisearch.filter.SearchDiagnostics

/** User-facing explanations stay separate from the filter's machine reasons. */
internal object SearchDiagnosticsText {
    fun summary(diagnostics: SearchDiagnostics): String =
        RemovalCategory.entries.mapNotNull { category ->
            val count = diagnostics.counts[category] ?: return@mapNotNull null
            if (count <= 0) null else "${label(category)} $count 条"
        }.joinToString(" · ")

    fun samples(diagnostics: SearchDiagnostics): String =
        diagnostics.samples.joinToString("\n\n") { sample ->
            sample.title + "\n" + explain(sample.reason)
        }

    private fun label(category: RemovalCategory): String = when (category) {
        RemovalCategory.AD -> "广告 / 商品"
        RemovalCategory.RECOMMENDATION -> "推荐卡"
        RemovalCategory.NON_VIDEO -> "非视频卡"
        RemovalCategory.BLOCK_WORD -> "屏蔽词"
        RemovalCategory.BLOCK_UP -> "屏蔽 UP"
        RemovalCategory.MIN_PLAY -> "播放量不足"
        RemovalCategory.DUPLICATE -> "重复标题"
        RemovalCategory.QUERY_RULE -> "搜索框规则"
        RemovalCategory.RELEVANCE -> "相关性不足"
        RemovalCategory.OTHER -> "其他"
    }

    fun explain(reason: String): String = when {
        reason.startsWith("blocked_word:") -> "标题命中屏蔽词「${reason.substringAfter(':')}」"
        reason.startsWith("blocked_up:") -> "命中屏蔽 UP「${reason.substringAfter(':')}」"
        reason.startsWith("excluded:") -> "标题包含排除词「${reason.substringAfter(':')}」"
        reason.startsWith("tag_missing:") -> "简介未包含 # 规则要求的「${reason.substringAfter(':')}」"
        reason.startsWith("title_missing:") -> "标题未包含必含词「${reason.substringAfter(':')}」"
        reason == "author_mismatch" -> "UP 主不符合 @ 规则"
        reason == "min_play" -> "播放量低于设置的最低值"
        reason == "duplicate" -> "与前面的结果标题和 UP 主相同"
        reason.startsWith("description_partial") -> "主要在简介中匹配，未达到当前过滤强度"
        reason.endsWith("_below_threshold") -> "标题关键词匹配不足，未达到当前过滤强度"
        else -> label(RemovalCategory.forReason(reason))
    }
}
