package io.github.xiaoancute.bilisearch

import android.app.Application
import android.content.Context
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class XposedInit : IXposedHookLoadPackage {

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName != TARGET_PACKAGE) return

        val processName = lpparam.processName ?: lpparam.packageName
        if (processName != TARGET_PACKAGE) return

        Log.i("loading in $processName")

        val hook = object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val context = param.args.firstOrNull() as? Context ?: return
                BiliSearchHook.install(context.classLoader)
            }
        }

        var hooked = false
        for (methodName in listOf("attach", "attachBaseContext")) {
            try {
                XposedHelpers.findAndHookMethod(
                    Application::class.java,
                    methodName,
                    Context::class.java,
                    hook
                )
                hooked = true
                break
            } catch (_: Throwable) {
                // Try the next entry point.
            }
        }

        if (!hooked) Log.e("could not hook Application.attach")
    }
}
