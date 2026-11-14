package io.github.xiaoancute.bilisearch

import android.os.ParcelFileDescriptor
import android.util.Log
import io.github.libxposed.api.XposedModule
import org.json.JSONObject
import java.util.concurrent.Executors

/**
 * Writes hook/search status into a framework remote file. The module app can
 * read the same file without a ContentProvider, so a force-stopped module app
 * no longer breaks settings or statistics.
 */
internal class StatusWriter(private val module: XposedModule) {

    private val executor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "BiliSearchStatus").apply { isDaemon = true }
    }

    private var loaded = false
    private var totalSearches = 0
    private var totalRemoved = 0
    private var hookInfo: JSONObject? = null
    private var searchInfo: JSONObject? = null

    @Synchronized
    fun hook(installed: Int, process: String, version: String) {
        ensureLoaded()
        hookInfo = JSONObject().apply {
            put("time", System.currentTimeMillis())
            put("installed", installed)
            put("process", process)
            put("version", version)
        }
        persist()
    }

    @Synchronized
    fun search(keyword: String, kept: Int, total: Int, removed: Int) {
        ensureLoaded()
        totalSearches++
        totalRemoved += removed
        searchInfo = JSONObject().apply {
            put("time", System.currentTimeMillis())
            put("keyword", keyword)
            put("kept", kept)
            put("total", total)
            put("removed", removed)
        }
        persist()
    }

    private fun ensureLoaded() {
        if (loaded) return
        loaded = true
        runCatching {
            val text = readText()
            if (text.isNullOrBlank()) return@runCatching
            val json = JSONObject(text)
            totalSearches = json.optInt("total_searches", 0)
            totalRemoved = json.optInt("total_removed", 0)
            hookInfo = json.optJSONObject("hook")
            searchInfo = json.optJSONObject("search")
        }
    }

    private fun persist() {
        val payload = JSONObject().apply {
            put("total_searches", totalSearches)
            put("total_removed", totalRemoved)
            hookInfo?.let { put("hook", it) }
            searchInfo?.let { put("search", it) }
        }.toString()

        executor.execute {
            runCatching { writeText(payload) }
                .onFailure { Log.w(LOG_TAG, "status file write failed", it) }
        }
    }

    private fun readText(): String? = module.openRemoteFile(STATUS_FILE).use { pfd ->
        ParcelFileDescriptor.AutoCloseInputStream(pfd).bufferedReader().readText()
    }

    private fun writeText(text: String) {
        module.openRemoteFile(STATUS_FILE).use { pfd ->
            ParcelFileDescriptor.AutoCloseOutputStream(pfd).use { out ->
                out.channel.truncate(0)
                out.write(text.toByteArray(Charsets.UTF_8))
            }
        }
    }
}
