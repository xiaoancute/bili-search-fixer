package io.github.xiaoancute.bilisearch

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class StatusReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_STATUS) return

        val prefs = context.getSharedPreferences(STATUS_PREFS, Context.MODE_PRIVATE)
        when (intent.getStringExtra("type")) {
            "hook" -> prefs.edit()
                .putLong(KEY_STATUS_TIME, System.currentTimeMillis())
                .putInt(KEY_STATUS_INSTALLED, intent.getIntExtra("installed", 0))
                .putString(KEY_STATUS_PROCESS, intent.getStringExtra("process"))
                .putString(KEY_STATUS_VERSION, intent.getStringExtra("version"))
                .apply()

            "search" -> {
                val removed = intent.getIntExtra("removed", 0)
                prefs.edit()
                    .putLong(KEY_STATUS_SEARCH_TIME, System.currentTimeMillis())
                    .putString(KEY_STATUS_SEARCH_KEYWORD, intent.getStringExtra("keyword"))
                    .putInt(KEY_STATUS_SEARCH_KEPT, intent.getIntExtra("kept", 0))
                    .putInt(KEY_STATUS_SEARCH_TOTAL, intent.getIntExtra("total", 0))
                    .putInt(KEY_STATUS_SEARCH_REMOVED, removed)
                    .putInt(KEY_TOTAL_SEARCHES, prefs.getInt(KEY_TOTAL_SEARCHES, 0) + 1)
                    .putInt(KEY_TOTAL_REMOVED, prefs.getInt(KEY_TOTAL_REMOVED, 0) + removed)
                    .apply()
            }
        }
    }
}
