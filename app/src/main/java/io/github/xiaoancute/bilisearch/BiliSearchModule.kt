package io.github.xiaoancute.bilisearch

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam
import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam
import io.github.xiaoancute.bilisearch.hook.SearchResultHook
import java.util.concurrent.atomic.AtomicBoolean

class BiliSearchModule : XposedModule() {

    private var processName: String = ""
    private var prefs: SharedPreferences? = null
    private val started = AtomicBoolean(false)

    override fun onModuleLoaded(param: ModuleLoadedParam) {
        processName = param.processName
    }

    override fun onPackageLoaded(param: PackageLoadedParam) {
        if (param.packageName != TARGET_PACKAGE) return
        if (processName.isNotEmpty() && processName != TARGET_PACKAGE) return

        prefs = getRemotePreferences(PREFS_NAME)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startOnce(param.defaultClassLoader)
        }
        installApplicationHook(param)
    }

    private fun installApplicationHook(param: PackageLoadedParam) {
        val fallbackClassLoader = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            runCatching { param.defaultClassLoader }.getOrNull()
        } else {
            null
        }

        val hooker = object : XposedInterface.Hooker {
            override fun intercept(chain: XposedInterface.Chain): Any? {
                val result = chain.proceed()
                val application = chain.thisObject as? Application
                val classLoader = application?.classLoader ?: fallbackClassLoader
                if (classLoader != null) startOnce(classLoader)
                return result
            }
        }

        for (methodName in listOf("attach", "attachBaseContext")) {
            val method = runCatching {
                Application::class.java
                    .getDeclaredMethod(methodName, Context::class.java)
                    .apply { isAccessible = true }
            }.getOrNull() ?: continue

            val installed = runCatching {
                hook(method)
                    .setExceptionMode(XposedInterface.ExceptionMode.PASSTHROUGH)
                    .intercept(hooker)
            }.isSuccess
            if (installed) return
        }

        log(Log.WARN, LOG_TAG, "Application.attach hook not installed")
    }

    private fun startOnce(classLoader: ClassLoader) {
        if (!started.compareAndSet(false, true)) return

        val preferences = prefs ?: getRemotePreferences(PREFS_NAME).also { prefs = it }
        val installed = runCatching {
            SearchResultHook(this, classLoader, preferences).install()
        }.onFailure {
            log(Log.ERROR, LOG_TAG, "search hook installation failed", it)
        }.getOrDefault(0)

        log(Log.INFO, LOG_TAG, "search hooks installed: " + installed)
        if (installed == 0) started.set(false)
    }
}
