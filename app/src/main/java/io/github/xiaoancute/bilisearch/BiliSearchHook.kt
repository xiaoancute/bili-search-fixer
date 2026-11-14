package io.github.xiaoancute.bilisearch

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers

internal object BiliSearchHook {

    @Volatile
    private var installed = false

    fun install(classLoader: ClassLoader) {
        if (installed) return
        synchronized(this) {
            if (installed) return
            installed = true
            try {
                installOkHttpHook(classLoader)
            } catch (t: Throwable) {
                Log.e("hook installation failed", t)
            }
        }
    }

    private fun installOkHttpHook(classLoader: ClassLoader) {
        val clientClass = Class.forName("okhttp3.OkHttpClient", false, classLoader)
        val requestClass = Class.forName("okhttp3.Request", false, classLoader)

        XposedHelpers.findAndHookMethod(
            clientClass,
            "newCall",
            requestClass,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val request = param.args.firstOrNull() ?: return
                    val url = runCatching {
                        XposedHelpers.callMethod(request, "url")?.toString()
                    }.getOrNull() ?: return

                    if (SearchUrlMatcher.isSearchResult(url)) {
                        Log.i("search request: $url")
                    }
                }
            }
        )

        Log.i("okhttp3 newCall hook installed")
    }
}
