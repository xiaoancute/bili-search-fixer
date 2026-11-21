package io.github.xiaoancute.bilisearch

import android.content.SharedPreferences
import io.github.xiaoancute.bilisearch.filter.RemovalCategory
import io.github.xiaoancute.bilisearch.filter.SearchDiagnostics

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
    val totalRemoved: Int = 0,
    val diagnostics: SearchDiagnostics? = null
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
        totalRemoved = prefs.getInt(KEY_TOTAL_REMOVED, 0),
        diagnostics = readDiagnostics()
    )

    fun recordHook(installed: Int, process: String?, version: String?) {
        prefs.edit()
            .putLong(KEY_STATUS_TIME, System.currentTimeMillis())
            .putInt(KEY_STATUS_INSTALLED, installed)
            .putString(KEY_STATUS_PROCESS, process)
            .putString(KEY_STATUS_VERSION, version)
            .apply()
    }

    fun recordSearch(
        keyword: String?, kept: Int, total: Int, removed: Int,
        diagnostics: SearchDiagnostics? = null
    ) {
        val editor = prefs.edit()
            .putLong(KEY_STATUS_SEARCH_TIME, System.currentTimeMillis())
            .putString(KEY_STATUS_SEARCH_KEYWORD, keyword)
            .putInt(KEY_STATUS_SEARCH_KEPT, kept)
            .putInt(KEY_STATUS_SEARCH_TOTAL, total)
            .putInt(KEY_STATUS_SEARCH_REMOVED, removed)
            .putInt(KEY_TOTAL_SEARCHES, prefs.getInt(KEY_TOTAL_SEARCHES, 0) + 1)
            .putInt(KEY_TOTAL_REMOVED, prefs.getInt(KEY_TOTAL_REMOVED, 0) + removed)
            .putBoolean(KEY_HAS_DIAGNOSTICS, diagnostics != null)

        RemovalCategory.entries.forEach { category ->
            editor.putInt(KEY_REASON_PREFIX + category.code, diagnostics?.counts?.get(category) ?: 0)
        }
        val samples = diagnostics?.samples.orEmpty().take(SearchDiagnostics.MAX_SAMPLES)
        editor.putInt(KEY_SAMPLE_COUNT, samples.size)
        for (index in 0 until SearchDiagnostics.MAX_SAMPLES) {
            val sample = samples.getOrNull(index)
            if (sample == null) {
                editor.remove(KEY_SAMPLE_TITLE_PREFIX + index)
                editor.remove(KEY_SAMPLE_REASON_PREFIX + index)
            } else {
                editor.putString(KEY_SAMPLE_TITLE_PREFIX + index, sample.title)
                editor.putString(KEY_SAMPLE_REASON_PREFIX + index, sample.reason)
            }
        }
        editor.apply()
    }

    private fun readDiagnostics(): SearchDiagnostics? {
        if (!prefs.getBoolean(KEY_HAS_DIAGNOSTICS, false)) return null
        val count = prefs.getInt(KEY_SAMPLE_COUNT, 0).coerceIn(0, SearchDiagnostics.MAX_SAMPLES)
        return SearchDiagnostics.fromPayload(
            RemovalCategory.entries.map { it.code },
            RemovalCategory.entries.map { prefs.getInt(KEY_REASON_PREFIX + it.code, 0) },
            (0 until count).map { prefs.getString(KEY_SAMPLE_TITLE_PREFIX + it, "").orEmpty() },
            (0 until count).map { prefs.getString(KEY_SAMPLE_REASON_PREFIX + it, "").orEmpty() }
        )
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
        const val KEY_HAS_DIAGNOSTICS = "hook_search_has_diagnostics"
        const val KEY_REASON_PREFIX = "hook_search_reason_"
        const val KEY_SAMPLE_COUNT = "hook_search_sample_count"
        const val KEY_SAMPLE_TITLE_PREFIX = "hook_search_sample_title_"
        const val KEY_SAMPLE_REASON_PREFIX = "hook_search_sample_reason_"
    }
}
