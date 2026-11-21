package io.github.xiaoancute.bilisearch

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.xiaoancute.bilisearch.filter.SearchDiagnostics

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
                removed = intent.getIntExtra("removed", 0),
                diagnostics = readDiagnostics(intent)
            )
        }
    }

    private fun readDiagnostics(intent: Intent): SearchDiagnostics? {
        if (!intent.hasExtra(SearchStatusExtras.REASON_CODES)) return null
        return SearchDiagnostics.fromPayload(
            intent.getStringArrayExtra(SearchStatusExtras.REASON_CODES)?.toList().orEmpty(),
            intent.getIntArrayExtra(SearchStatusExtras.REASON_COUNTS)?.toList().orEmpty(),
            intent.getStringArrayExtra(SearchStatusExtras.SAMPLE_TITLES)?.toList().orEmpty(),
            intent.getStringArrayExtra(SearchStatusExtras.SAMPLE_REASONS)?.toList().orEmpty()
        ).takeIf { it.totalRemoved == intent.getIntExtra("removed", 0) }
    }
}
