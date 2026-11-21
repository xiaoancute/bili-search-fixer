package io.github.xiaoancute.bilisearch

import android.content.SharedPreferences
import io.github.xiaoancute.bilisearch.filter.RemovalTracker
import io.github.xiaoancute.bilisearch.filter.SearchDiagnostics
import java.lang.reflect.Proxy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StatusStoreTest {
    @Test
    fun roundTripsReasonsAndTitlesWithExistingSearchCounters() {
        val store = StatusStore(memoryPreferences())
        val tracker = RemovalTracker()
        tracker.record("ad")
        tracker.record("excluded:抽卡", "原神攻略 抽卡建议")
        val diagnostics = tracker.snapshot()

        store.recordSearch("原神攻略 -抽卡", 18, 20, 2, diagnostics)
        val status = store.read()
        assertEquals(diagnostics, status.diagnostics)
        assertEquals(1, status.totalSearches)
        assertEquals(2, status.totalRemoved)
        assertEquals(18, status.kept)
        assertTrue(status.hasSearch)
    }

    @Test
    fun nextResponseReplacesReasonsAndClearsOldSamples() {
        val store = StatusStore(memoryPreferences())
        val tracker = RemovalTracker()
        repeat(5) { tracker.record("duplicate", "旧结果 $it") }
        store.recordSearch("旧查询", 15, 20, 5, tracker.snapshot())
        store.recordSearch("新查询", 20, 20, 0, SearchDiagnostics())

        val status = store.read()
        assertEquals(SearchDiagnostics(), status.diagnostics)
        assertEquals("新查询", status.keyword)
        assertEquals(2, status.totalSearches)
        assertEquals(5, status.totalRemoved)
    }

    @Test
    fun legacyReportsDoNotReuseNewerReportsDiagnostics() {
        val store = StatusStore(memoryPreferences())
        val tracker = RemovalTracker()
        tracker.record("min_play", "播放量不足的视频")
        store.recordSearch("新版本", 19, 20, 1, tracker.snapshot())
        store.recordSearch("旧版本", 18, 20, 2)

        assertNull(store.read().diagnostics)
        assertEquals(3, store.read().totalRemoved)
    }

    // A JVM-only preference interface double; no Android framework methods run.
    private fun memoryPreferences(): SharedPreferences {
        val values = HashMap<String, Any?>()
        fun editor(): SharedPreferences.Editor {
            val pending = HashMap<String, Any?>()
            return Proxy.newProxyInstance(
                SharedPreferences.Editor::class.java.classLoader,
                arrayOf(SharedPreferences.Editor::class.java)
            ) { proxy, method, args ->
                when {
                    method.name.startsWith("put") -> {
                        pending[args[0] as String] = args[1]
                        proxy
                    }
                    method.name == "remove" -> {
                        pending[args[0] as String] = null
                        proxy
                    }
                    method.name == "apply" || method.name == "commit" -> {
                        pending.forEach { (key, value) ->
                            if (value == null) values.remove(key) else values[key] = value
                        }
                        if (method.name == "commit") true else null
                    }
                    else -> error("Unexpected editor method: ${method.name}")
                }
            } as SharedPreferences.Editor
        }
        return Proxy.newProxyInstance(
            SharedPreferences::class.java.classLoader,
            arrayOf(SharedPreferences::class.java)
        ) { _, method, args ->
            when (method.name) {
                "getString", "getInt", "getLong", "getBoolean" -> values[args[0] as String] ?: args[1]
                "edit" -> editor()
                else -> error("Unexpected preference method: ${method.name}")
            }
        } as SharedPreferences
    }
}
