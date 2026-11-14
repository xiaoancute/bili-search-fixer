package io.github.xiaoancute.bilisearch

import android.content.Context
import android.os.ParcelFileDescriptor
import org.json.JSONObject
import java.io.File

internal object StatusReader {

    fun read(context: Context): JSONObject? {
        val text = runCatching {
            val pfd = BiliSearchApplication.service?.openRemoteFile(STATUS_FILE)
            if (pfd != null) {
                ParcelFileDescriptor.AutoCloseInputStream(pfd).bufferedReader().readText()
            } else {
                File(context.filesDir, STATUS_FILE).readText()
            }
        }.getOrNull()

        return runCatching { text?.let(::JSONObject) }.getOrNull()
    }
}
