package io.github.xiaoancute.bilisearch

internal data class Settings(
    val enabled: Boolean = true,
    val threshold: Int = 50,
    val dropAds: Boolean = true,
    val videoOnly: Boolean = false,
    val hideRecommend: Boolean = false,
    val dedupe: Boolean = false,
    val rerank: Boolean = false,
    val minPlay: Int = 0,
    val blockWords: List<String> = emptyList(),
    val blockUps: List<String> = emptyList(),
    val whitelist: List<String> = emptyList()
)
