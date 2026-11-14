package io.github.xiaoancute.bilisearch

import org.junit.Assert.assertEquals
import org.junit.Test

class FilterStrengthTest {

    @Test
    fun mapsStoredThresholdToNamedStrength() {
        assertEquals(FilterStrength.LOOSE, FilterStrength.fromThreshold(0))
        assertEquals(FilterStrength.LOOSE, FilterStrength.fromThreshold(35))
        assertEquals(FilterStrength.STANDARD, FilterStrength.fromThreshold(36))
        assertEquals(FilterStrength.STANDARD, FilterStrength.fromThreshold(65))
        assertEquals(FilterStrength.STRICT, FilterStrength.fromThreshold(66))
        assertEquals(FilterStrength.STRICT, FilterStrength.fromThreshold(100))
    }
}
