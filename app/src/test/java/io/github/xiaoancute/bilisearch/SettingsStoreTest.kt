package io.github.xiaoancute.bilisearch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsStoreTest {

    @Test
    fun parsesNewlinesCommasAndSemicolons() {
        assertEquals(
            listOf("广告", "抽卡", "课程"),
            SettingsStore.parseList("广告\n 抽卡, 课程；广告")
        )
    }

    @Test
    fun trimsAndDropsEmptyEntries() {
        assertEquals(
            listOf("UP主", "123"),
            SettingsStore.parseList("  UP主  ; ; 123 ;")
        )
        assertTrue(SettingsStore.parseList("   ").isEmpty())
    }
}
