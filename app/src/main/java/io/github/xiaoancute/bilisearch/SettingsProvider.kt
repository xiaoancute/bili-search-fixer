package io.github.xiaoancute.bilisearch

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Bundle

/**
 * Read-only settings bridge plus a tiny status sink used to prove from the
 * module app that the hook has actually run inside Bilibili.
 */
class SettingsProvider : ContentProvider() {

    override fun onCreate(): Boolean = true

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        val ctx = context ?: return null
        val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        return when (method) {
            "get" -> Bundle().apply {
                putBoolean(KEY_ENABLED, prefs.getBoolean(KEY_ENABLED, true))
                putInt(KEY_THRESHOLD, prefs.getInt(KEY_THRESHOLD, 50))
                putBoolean(KEY_DROP_ADS, prefs.getBoolean(KEY_DROP_ADS, true))
                putBoolean(KEY_VIDEO_ONLY, prefs.getBoolean(KEY_VIDEO_ONLY, false))
                putBoolean(KEY_HIDE_RECOMMEND, prefs.getBoolean(KEY_HIDE_RECOMMEND, false))
                putBoolean(KEY_DEDUPE, prefs.getBoolean(KEY_DEDUPE, false))
                putBoolean(KEY_RERANK, prefs.getBoolean(KEY_RERANK, false))
                putInt(KEY_MIN_PLAY, prefs.getInt(KEY_MIN_PLAY, 0))
                putString(KEY_BLOCK_WORDS, prefs.getString(KEY_BLOCK_WORDS, ""))
                putString(KEY_BLOCK_UPS, prefs.getString(KEY_BLOCK_UPS, ""))
                putString(KEY_WHITELIST, prefs.getString(KEY_WHITELIST, ""))
            }

            "status" -> {
                prefs.edit()
                    .putLong(KEY_STATUS_TIME, System.currentTimeMillis())
                    .putInt(KEY_STATUS_INSTALLED, extras?.getInt("installed", 0) ?: 0)
                    .putString(KEY_STATUS_PROCESS, extras?.getString("process"))
                    .putString(KEY_STATUS_VERSION, extras?.getString("version"))
                    .apply()
                Bundle().apply { putBoolean("ok", true) }
            }

            "search" -> {
                val removed = extras?.getInt("removed", 0) ?: 0
                prefs.edit()
                    .putLong(KEY_STATUS_SEARCH_TIME, System.currentTimeMillis())
                    .putString(KEY_STATUS_SEARCH_KEYWORD, extras?.getString("keyword"))
                    .putInt(KEY_STATUS_SEARCH_KEPT, extras?.getInt("kept", 0) ?: 0)
                    .putInt(KEY_STATUS_SEARCH_TOTAL, extras?.getInt("total", 0) ?: 0)
                    .putInt(KEY_STATUS_SEARCH_REMOVED, removed)
                    .putInt(KEY_TOTAL_SEARCHES, prefs.getInt(KEY_TOTAL_SEARCHES, 0) + 1)
                    .putInt(KEY_TOTAL_REMOVED, prefs.getInt(KEY_TOTAL_REMOVED, 0) + removed)
                    .apply()
                Bundle().apply { putBoolean("ok", true) }
            }

            else -> Bundle()
        }
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? = null

    override fun getType(uri: Uri): String? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int = 0
}
