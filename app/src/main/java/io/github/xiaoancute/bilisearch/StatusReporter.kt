package io.github.xiaoancute.bilisearch

import android.content.Context
import android.content.Intent
import android.os.Bundle

internal class StatusReporter(private val context: Context) {

    fun hook(installed: Int, process: String, version: String) {
        send {
            putString("type", "hook")
            putInt("installed", installed)
            putString("process", process)
            putString("version", version)
        }
    }

    fun search(keyword: String, kept: Int, total: Int, removed: Int) {
        send {
            putString("type", "search")
            putString("keyword", keyword)
            putInt("kept", kept)
            putInt("total", total)
            putInt("removed", removed)
        }
    }

    private fun send(fill: Bundle.() -> Unit) {
        val extras = Bundle().apply(fill)
        val intent = Intent(ACTION_STATUS)
            .setPackage(MODULE_PACKAGE)
            .putExtras(extras)
        runCatching { context.sendBroadcast(intent) }
    }
}
