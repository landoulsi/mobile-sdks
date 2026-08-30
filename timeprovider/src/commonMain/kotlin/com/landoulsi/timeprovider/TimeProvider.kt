package com.landoulsi.timeprovider

import kotlin.concurrent.Volatile
import kotlin.time.Duration

/**
 * Multiplatform contract for providing current epoch timestamps.
 */
interface TimeProvider {
    /**
     * Returns the current time in milliseconds since UNIX epoch (January 1, 1970 00:00:00 UTC).
     */
    fun currentTimeMillis(): Long
}

/**
 * Low-level platform function returning system epoch time in milliseconds.
 */
expect fun systemEpochMillis(): Long

/**
 * Standard implementation of [TimeProvider] backed by the platform's system clock.
 */
class SystemTimeProvider : TimeProvider {
    override fun currentTimeMillis(): Long = systemEpochMillis()
}

/**
 * Test/mock implementation of [TimeProvider] allowing manual control of time.
 */
class FakeTimeProvider(
    initialMillis: Long = 0L,
) : TimeProvider {

    @Volatile
    private var currentMillis: Long = initialMillis

    override fun currentTimeMillis(): Long = currentMillis

    fun advanceBy(millis: Long) {
        currentMillis += millis
    }

    fun advanceBy(duration: Duration) {
        advanceBy(duration.inWholeMilliseconds)
    }

    fun setTime(millis: Long) {
        currentMillis = millis
    }
}
