package io.github.xiaoancute.bilisearch

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import java.util.concurrent.Executors

/**
 * Runs inside the Bilibili process. Settings live in the module app and are
 * read through its exported ContentProvider, so changes do not require a
 * module reinstall.
 */
internal class SettingsRepository(private val context: Context) {

    private val uri = Uri.parse("content://" + SETTINGS_AUTHORITY)
    private val executor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "BiliSearchStatus").apply { isDaemon = true }
    }

    @Volatile
    private var cached = Settings()

    @Volatile
    private var cachedAt = 0L

    @Volatile
    private var lastSearchKeyword: String? = null

    @Volatile
    private var lastSearchAt = 0L

    fun preload() {
        executor.execute { current() }
    }

    fun current(): Settings {
        val now = SystemClock.elapsedRealtime()
        if (now - cachedAt < CACHE_MS) return cached
        val fresh = read()
        if (fresh != null) {
            cached = fresh
            cachedAt = now
        }
        return cached
    }

    private fun read(): Settings? = runCatching {
        val bundle = context.contentResolver.call(uri, "get", null, null)
            ?: return null
        Settings(
            enabled = bundle.getBoolean(KEY_ENABLED, true),
            threshold = bundle.getInt(KEY_THRESHOLD, 50),
            dropAds = bundle.getBoolean(KEY_DROP_ADS, true),
            videoOnly = bundle.getBoolean(KEY_VIDEO_ONLY, false),
            hideRecommend = bundle.getBoolean(KEY_HIDE_RECOMMEND, false),
            dedupe = bundle.getBoolean(KEY_DEDUPE, false),
            rerank = bundle.getBoolean(KEY_RERANK, false),
            minPlay = bundle.getInt(KEY_MIN_PLAY, 0).coerceAtLeast(0),
            blockWords = splitList(bundle.getString(KEY_BLOCK_WORDS)),
            blockUps = splitList(bundle.getString(KEY_BLOCK_UPS)),
            whitelist = splitList(bundle.getString(KEY_WHITELIST))
        )
    }.getOrNull()

    private fun splitList(raw: String?): List<String> =
        raw.orEmpty()
            .split('\n', ',', '，', ';', '；')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()

    fun reportStatusAsync(installed: Int, process: String, version: String) {
        executor.execute {
            runCatching {
                val extras = Bundle().apply {
                    putInt("installed", installed)
                    putString("process", process)
                    putString("version", version)
                }
                context.contentResolver.call(uri, "status", null, extras)
            }
        }
    }

    fun reportSearchAsync(keyword: String, kept: Int, total: Int, removed: Int) {
        val now = SystemClock.elapsedRealtime()
        if (keyword == lastSearchKeyword && now - lastSearchAt < SEARCH_REPORT_INTERVAL_MS) return
        lastSearchKeyword = keyword
        lastSearchAt = now

        executor.execute {
            runCatching {
                val extras = Bundle().apply {
                    putString("keyword", keyword)
                    putInt("kept", kept)
                    putInt("total", total)
                    putInt("removed", removed)
                }
                context.contentResolver.call(uri, "search", null, extras)
            }
        }
    }

    private companion object {
        const val CACHE_MS = 2_000L
        const val SEARCH_REPORT_INTERVAL_MS = 1_500L
    }
}
