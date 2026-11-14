package io.github.xiaoancute.bilisearch

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class StatusReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_STATUS) return

        val store = StatusStore(
            context.getSharedPreferences(STATUS_PREFS, Context.MODE_PRIVATE)
        )

        when (intent.getStringExtra("type")) {
            "hook" -> store.recordHook(
                installed = intent.getIntExtra("installed", 0),
                process = intent.getStringExtra("process"),
                version = intent.getStringExtra("version")
            )

            "search" -> store.recordSearch(
                keyword = intent.getStringExtra("keyword"),
                kept = intent.getIntExtra("kept", 0),
                total = intent.getIntExtra("total", 0),
                removed = intent.getIntExtra("removed", 0)
            )
        }
    }
}
