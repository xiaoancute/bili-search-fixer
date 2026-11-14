package io.github.xiaoancute.bilisearch

import de.robv.android.xposed.XposedBridge

internal object Log {
    fun i(message: String) {
        XposedBridge.log("[$LOG_TAG] $message")
    }

    fun e(message: String, throwable: Throwable? = null) {
        val detail = throwable?.stackTraceToString().orEmpty()
        XposedBridge.log("[$LOG_TAG] $message\n$detail")
    }
}
