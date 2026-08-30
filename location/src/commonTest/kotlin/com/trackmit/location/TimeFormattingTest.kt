package com.trackmit.location

import kotlin.test.Test
import kotlin.test.assertEquals

class TimeFormattingTest {
    @Test
    fun formatsUnixEpochZero() {
        assertEquals("1970-01-01T00:00:00.000Z", formatEpochMillisAsRfc3339(0L))
    }

    @Test
    fun formatsKnownTimestampWithMillisecondPrecision() {
        // 2026-05-12T07:14:00.000Z
        assertEquals("2026-05-12T07:14:00.000Z", formatEpochMillisAsRfc3339(1_778_570_040_000L))
    }

    @Test
    fun formatsSubSecondMillis() {
        assertEquals("1970-01-01T00:00:00.123Z", formatEpochMillisAsRfc3339(123L))
    }

    @Test
    fun formatsLeapDayCorrectly() {
        // 2024-02-29T23:59:59.000Z
        assertEquals("2024-02-29T23:59:59.000Z", formatEpochMillisAsRfc3339(1_709_251_199_000L))
    }

    @Test
    fun formatsTimestampsBeforeEpoch() {
        // 1969-12-31T23:59:59.500Z
        assertEquals("1969-12-31T23:59:59.500Z", formatEpochMillisAsRfc3339(-500L))
    }

    @Test
    fun currentTimestampDelegatesToTimeProviderAndFormatsRfc3339() {
        val fixedTimeProvider = object : TimeProvider {
            override fun currentTimeMillis(): Long = 1_778_570_040_000L
        }

        assertEquals("2026-05-12T07:14:00.000Z", fixedTimeProvider.currentTimestamp())
    }
}
