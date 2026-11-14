package io.github.xiaoancute.bilisearch

import android.content.SharedPreferences
import android.util.Log

/**
 * Owns the settings key mapping and serialization. Both the module app and
 * the hooked Bilibili process depend on this store; neither touches raw
 * preference keys.
 */
internal class SettingsStore(private val prefs: SharedPreferences?) {

    fun read(): Settings {
        val p = prefs ?: return Settings()
        return Settings(
            enabled = p.getBoolean(KEY_ENABLED, true),
            threshold = FilterStrength.fromThreshold(p.getInt(KEY_THRESHOLD, 50)).threshold,
            dropAds = p.getBoolean(KEY_DROP_ADS, true),
            videoOnly = p.getBoolean(KEY_VIDEO_ONLY, false),
            hideRecommend = p.getBoolean(KEY_HIDE_RECOMMEND, false),
            dedupe = p.getBoolean(KEY_DEDUPE, false),
            rerank = p.getBoolean(KEY_RERANK, false),
            minPlay = p.getInt(KEY_MIN_PLAY, 0).coerceAtLeast(0),
            blockWords = parseList(p.getString(KEY_BLOCK_WORDS, "")),
            blockUps = parseList(p.getString(KEY_BLOCK_UPS, "")),
            whitelist = parseList(p.getString(KEY_WHITELIST, ""))
        )
    }

    fun write(settings: Settings) {
        val editor = prefs?.edit() ?: return
        editor
            .putBoolean(KEY_ENABLED, settings.enabled)
            .putInt(KEY_THRESHOLD, settings.threshold.coerceIn(0, 100))
            .putBoolean(KEY_DROP_ADS, settings.dropAds)
            .putBoolean(KEY_VIDEO_ONLY, settings.videoOnly)
            .putBoolean(KEY_HIDE_RECOMMEND, settings.hideRecommend)
            .putBoolean(KEY_DEDUPE, settings.dedupe)
            .putBoolean(KEY_RERANK, settings.rerank)
            .putInt(KEY_MIN_PLAY, settings.minPlay.coerceAtLeast(0))
            .putString(KEY_BLOCK_WORDS, settings.blockWords.joinToString("\n"))
            .putString(KEY_BLOCK_UPS, settings.blockUps.joinToString("\n"))
            .putString(KEY_WHITELIST, settings.whitelist.joinToString("\n"))

        val committed = editor.commit()
        Log.d(LOG_TAG, "settings write threshold=" + settings.threshold + " committed=" + committed)
    }

    companion object {
        fun parseList(raw: String?): List<String> =
            raw.orEmpty()
                .split('\n', ',', '，', ';', '；')
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .distinct()

        private const val KEY_ENABLED = "search_filter_enabled"
        private const val KEY_THRESHOLD = "search_relevance_threshold"
        private const val KEY_DROP_ADS = "search_drop_ads"
        private const val KEY_VIDEO_ONLY = "search_video_only"
        private const val KEY_HIDE_RECOMMEND = "search_hide_recommend"
        private const val KEY_DEDUPE = "search_dedupe"
        private const val KEY_RERANK = "search_rerank"
        private const val KEY_MIN_PLAY = "search_min_play"
        private const val KEY_BLOCK_WORDS = "search_block_words"
        private const val KEY_BLOCK_UPS = "search_block_ups"
        private const val KEY_WHITELIST = "search_whitelist"
    }
}
