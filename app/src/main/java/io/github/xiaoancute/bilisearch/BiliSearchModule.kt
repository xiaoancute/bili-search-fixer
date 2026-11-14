package io.github.xiaoancute.bilisearch

import android.app.Application
import android.util.Log
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam
import io.github.xiaoancute.bilisearch.hook.SearchResultHook
import java.util.concurrent.atomic.AtomicBoolean

class BiliSearchModule : XposedModule() {

    private var processName: String = ""
    private val started = AtomicBoolean(false)

    override fun onModuleLoaded(param: ModuleLoadedParam) {
        processName = param.processName
    }

    override fun onPackageReady(param: PackageReadyParam) {
        if (param.packageName != TARGET_PACKAGE) return
        if (processName.isNotEmpty() && processName != TARGET_PACKAGE) return
        if (!param.isFirstPackage) return
        if (!started.compareAndSet(false, true)) return

        val prefs = runCatching { getRemotePreferences(SETTINGS_PREFS) }.getOrNull()
        if (prefs == null) {
            log(Log.WARN, LOG_TAG, "remote preferences unavailable; using defaults")
        }

        val settings = SettingsRepository(prefs)
        val reporter = currentApplication()?.let { StatusReporter(it) }
        if (reporter == null) {
            log(Log.WARN, LOG_TAG, "application context unavailable; status disabled")
        }

        val installed = runCatching {
            SearchResultHook(this, param.classLoader, settings, reporter).install()
        }.onFailure {
            log(Log.ERROR, LOG_TAG, "search hook installation failed", it)
        }.getOrDefault(0)

        reporter?.hook(installed, processName, MODULE_VERSION)
        log(Log.INFO, LOG_TAG, "search hooks installed: " + installed)
    }

    private fun currentApplication(): Application? = runCatching {
        Class.forName("android.app.ActivityThread")
            .getDeclaredMethod("currentApplication")
            .apply { isAccessible = true }
            .invoke(null) as? Application
    }.getOrNull()
}
