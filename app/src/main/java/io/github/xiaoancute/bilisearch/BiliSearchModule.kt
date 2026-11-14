package io.github.xiaoancute.bilisearch

import android.app.Application
import android.content.Context
import android.util.Log
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam
import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam
import io.github.xiaoancute.bilisearch.hook.SearchResultHook
import java.util.concurrent.atomic.AtomicBoolean

class BiliSearchModule : XposedModule() {

    private var processName: String = ""
    private val started = AtomicBoolean(false)

    override fun onModuleLoaded(param: ModuleLoadedParam) {
        processName = param.processName
    }

    override fun onPackageLoaded(param: PackageLoadedParam) {
        if (param.packageName != TARGET_PACKAGE) return
        if (processName.isNotEmpty() && processName != TARGET_PACKAGE) return

        installApplicationHook()
        currentApplication()?.let { application ->
            startOnce(application, application.classLoader)
        }
    }

    private fun installApplicationHook() {
        val hooker = object : XposedInterface.Hooker {
            override fun intercept(chain: XposedInterface.Chain): Any? {
                val result = chain.proceed()
                val application = chain.thisObject as? Application
                if (application != null) {
                    startOnce(application, application.classLoader)
                }
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

    private fun startOnce(application: Application, classLoader: ClassLoader) {
        if (!started.compareAndSet(false, true)) return

        val settings = SettingsRepository(application)
        val installed = runCatching {
            SearchResultHook(this, classLoader, settings).install()
        }.onFailure {
            log(Log.ERROR, LOG_TAG, "search hook installation failed", it)
        }.getOrDefault(0)

        log(Log.INFO, LOG_TAG, "search hooks installed: " + installed)
        Thread {
            settings.reportStatus(installed, processName, moduleVersion(application))
        }.apply { isDaemon = true }.start()

        if (installed == 0) started.set(false)
    }

    private fun moduleVersion(context: Context): String = runCatching {
        context.packageManager.getPackageInfo(MODULE_PACKAGE, 0).versionName
    }.getOrNull() ?: "unknown"

    private fun currentApplication(): Application? = runCatching {
        Class.forName("android.app.ActivityThread")
            .getDeclaredMethod("currentApplication")
            .apply { isAccessible = true }
            .invoke(null) as? Application
    }.getOrNull()
}
