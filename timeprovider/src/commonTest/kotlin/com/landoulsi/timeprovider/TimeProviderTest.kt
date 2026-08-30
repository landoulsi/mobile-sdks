package com.landoulsi.timeprovider

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class TimeProviderTest {

    @Test
    fun testSystemTimeProviderReturnsPositiveTimestamp() {
        val provider = SystemTimeProvider()
        val time = provider.currentTimeMillis()
        assertTrue(time > 0L)
    }

    @Test
    fun testFakeTimeProviderManualControl() {
        val fakeTimeProvider = FakeTimeProvider(1_000L)
        assertEquals(1_000L, fakeTimeProvider.currentTimeMillis())

        fakeTimeProvider.advanceBy(500L)
        assertEquals(1_500L, fakeTimeProvider.currentTimeMillis())

        fakeTimeProvider.advanceBy(3.seconds)
        assertEquals(4_500L, fakeTimeProvider.currentTimeMillis())

        fakeTimeProvider.setTime(5_000L)
        assertEquals(5_000L, fakeTimeProvider.currentTimeMillis())
    }
}
