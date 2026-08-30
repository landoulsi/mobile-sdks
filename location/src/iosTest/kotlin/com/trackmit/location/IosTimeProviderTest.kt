package com.trackmit.location

import kotlin.test.Test
import kotlin.test.assertTrue

class IosTimeProviderTest {
    @Test
    fun testCurrentTimeMillisReturnsValidTimestamp() {
        val provider = IosTimeProvider()
        val timestamp = provider.currentTimeMillis()

        assertTrue(timestamp > 0, "Timestamp should be positive")
        assertTrue(timestamp > 1_640_995_200_000, "Timestamp should be after 2022-01-01")
    }

    @Test
    fun testMultipleTimestampsAreMonotonic() {
        val provider = IosTimeProvider()
        val timestamps = (0..100).map { provider.currentTimeMillis() }

        for (i in 1 until timestamps.size) {
            assertTrue(timestamps[i] >= timestamps[i - 1], "Timestamps should be monotonically increasing")
        }
    }
}
