package io.github.xiaoancute.bilisearch

import android.content.SharedPreferences

internal data class StatusSnapshot(
    val hookTime: Long = 0L,
    val installed: Int = 0,
    val process: String? = null,
    val version: String? = null,
    val searchTime: Long = 0L,
    val keyword: String = "",
    val kept: Int = 0,
    val total: Int = 0,
    val removed: Int = 0,
    val totalSearches: Int = 0,
    val totalRemoved: Int = 0
) {
    val hasHook: Boolean get() = hookTime > 0L
    val hasSearch: Boolean get() = searchTime > 0L
}

/**
 * Owns the status key mapping and the cumulative counters. The broadcast
 * receiver writes through it; the settings screen reads through it.
 */
internal class StatusStore(private val prefs: SharedPreferences) {

    fun read(): StatusSnapshot = StatusSnapshot(
        hookTime = prefs.getLong(KEY_STATUS_TIME, 0L),
        installed = prefs.getInt(KEY_STATUS_INSTALLED, 0),
        process = prefs.getString(KEY_STATUS_PROCESS, null),
        version = prefs.getString(KEY_STATUS_VERSION, null),
        searchTime = prefs.getLong(KEY_STATUS_SEARCH_TIME, 0L),
        keyword = prefs.getString(KEY_STATUS_SEARCH_KEYWORD, "") ?: "",
        kept = prefs.getInt(KEY_STATUS_SEARCH_KEPT, 0),
        total = prefs.getInt(KEY_STATUS_SEARCH_TOTAL, 0),
        removed = prefs.getInt(KEY_STATUS_SEARCH_REMOVED, 0),
        totalSearches = prefs.getInt(KEY_TOTAL_SEARCHES, 0),
        totalRemoved = prefs.getInt(KEY_TOTAL_REMOVED, 0)
    )

    fun recordHook(installed: Int, process: String?, version: String?) {
        prefs.edit()
            .putLong(KEY_STATUS_TIME, System.currentTimeMillis())
            .putInt(KEY_STATUS_INSTALLED, installed)
            .putString(KEY_STATUS_PROCESS, process)
            .putString(KEY_STATUS_VERSION, version)
            .apply()
    }

    fun recordSearch(keyword: String?, kept: Int, total: Int, removed: Int) {
        prefs.edit()
            .putLong(KEY_STATUS_SEARCH_TIME, System.currentTimeMillis())
            .putString(KEY_STATUS_SEARCH_KEYWORD, keyword)
            .putInt(KEY_STATUS_SEARCH_KEPT, kept)
            .putInt(KEY_STATUS_SEARCH_TOTAL, total)
            .putInt(KEY_STATUS_SEARCH_REMOVED, removed)
            .putInt(KEY_TOTAL_SEARCHES, prefs.getInt(KEY_TOTAL_SEARCHES, 0) + 1)
            .putInt(KEY_TOTAL_REMOVED, prefs.getInt(KEY_TOTAL_REMOVED, 0) + removed)
            .apply()
    }

    private companion object {
        const val KEY_STATUS_TIME = "hook_status_time"
        const val KEY_STATUS_INSTALLED = "hook_status_installed"
        const val KEY_STATUS_PROCESS = "hook_status_process"
        const val KEY_STATUS_VERSION = "hook_status_version"
        const val KEY_STATUS_SEARCH_TIME = "hook_search_time"
        const val KEY_STATUS_SEARCH_KEYWORD = "hook_search_keyword"
        const val KEY_STATUS_SEARCH_KEPT = "hook_search_kept"
        const val KEY_STATUS_SEARCH_TOTAL = "hook_search_total"
        const val KEY_STATUS_SEARCH_REMOVED = "hook_search_removed"
        const val KEY_TOTAL_SEARCHES = "stats_total_searches"
        const val KEY_TOTAL_REMOVED = "stats_total_removed"
    }
}
