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

    fun current(): Settings {
        val now = SystemClock.elapsedRealtime()
        if (now - cachedAt < CACHE_MS) return cached
        cached = read()
        cachedAt = now
        return cached
    }

    private fun read(): Settings = runCatching {
        val bundle = context.contentResolver.call(uri, "get", null, null)
            ?: return Settings()
        Settings(
            enabled = bundle.getBoolean(KEY_ENABLED, true),
            threshold = bundle.getInt(KEY_THRESHOLD, 50),
            dropAds = bundle.getBoolean(KEY_DROP_ADS, true)
        )
    }.getOrDefault(Settings())

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
    }
}
