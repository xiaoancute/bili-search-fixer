package io.github.xiaoancute.bilisearch

import android.content.SharedPreferences

/**
 * Runs inside the Bilibili process and reads the framework-backed remote
 * preferences written by the module app.
 */
internal class SettingsRepository(private val prefs: SharedPreferences?) {

    fun current(): Settings {
        val p = prefs ?: return Settings()
        return Settings(
            enabled = p.getBoolean(KEY_ENABLED, true),
            threshold = p.getInt(KEY_THRESHOLD, 50),
            dropAds = p.getBoolean(KEY_DROP_ADS, true),
            videoOnly = p.getBoolean(KEY_VIDEO_ONLY, false),
            hideRecommend = p.getBoolean(KEY_HIDE_RECOMMEND, false),
            dedupe = p.getBoolean(KEY_DEDUPE, false),
            rerank = p.getBoolean(KEY_RERANK, false),
            minPlay = p.getInt(KEY_MIN_PLAY, 0).coerceAtLeast(0),
            blockWords = splitList(p.getString(KEY_BLOCK_WORDS, "")),
            blockUps = splitList(p.getString(KEY_BLOCK_UPS, "")),
            whitelist = splitList(p.getString(KEY_WHITELIST, ""))
        )
    }

    private fun splitList(raw: String?): List<String> =
        raw.orEmpty()
            .split('\n', ',', '，', ';', '；')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
}
