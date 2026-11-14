package io.github.xiaoancute.bilisearch

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock

/**
 * Runs inside the Bilibili process. Settings live in the module app and are
 * read through its exported ContentProvider, so changes do not require a
 * module reinstall.
 */
internal class SettingsRepository(private val context: Context) {

    private val uri = Uri.parse("content://" + SETTINGS_AUTHORITY)
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

    fun reportStatus(installed: Int, process: String, version: String) {
        runCatching {
            val extras = Bundle().apply {
                putInt("installed", installed)
                putString("process", process)
                putString("version", version)
            }
            context.contentResolver.call(uri, "status", null, extras)
        }
    }

    private companion object {
        const val CACHE_MS = 2_000L
    }
}
