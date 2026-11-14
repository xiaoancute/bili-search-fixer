package io.github.xiaoancute.bilisearch

import android.app.Application
import android.content.SharedPreferences
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper
import java.util.concurrent.CopyOnWriteArraySet

class BiliSearchApplication : Application(), XposedServiceHelper.OnServiceListener {

    fun interface ServiceStateListener {
        fun onServiceStateChanged(service: XposedService?)
    }

    companion object {
        private val listeners = CopyOnWriteArraySet<ServiceStateListener>()

        @Volatile
        var service: XposedService? = null
            private set

        fun remotePreferences(): SharedPreferences? =
            runCatching { service?.getRemotePreferences(SETTINGS_PREFS) }.getOrNull()

        fun addServiceStateListener(listener: ServiceStateListener) {
            listeners.add(listener)
            listener.onServiceStateChanged(service)
        }

        fun removeServiceStateListener(listener: ServiceStateListener) {
            listeners.remove(listener)
        }
    }

    override fun onCreate() {
        super.onCreate()
        XposedServiceHelper.registerListener(this)
    }

    override fun onServiceBind(service: XposedService) {
        Companion.service = service
        listeners.forEach { it.onServiceStateChanged(service) }
    }

    override fun onServiceDied(service: XposedService) {
        if (Companion.service === service) {
            Companion.service = null
            listeners.forEach { it.onServiceStateChanged(null) }
        }
    }
}
