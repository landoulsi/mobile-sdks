package com.trackflow.location

import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationCoordinate2DMake
import platform.Foundation.NSDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@OptIn(ExperimentalForeignApi::class)
class IosLocationConversionTest {

    private val time = object : TimeProvider {
        override fun currentTimeMillis(): Long = 1_778_570_040_000L // 2026-05-12T07:14:00.000Z
    }

    @Test
    fun validFixConvertsAndUsesTheSdkTimestamp() {
        val loc = assertNotNull(CLLocation(latitude = 48.8566, longitude = 2.3522).toLocationOrNull(time))

        assertEquals(48.8566, loc.latitude, 1e-9)
        assertEquals(2.3522, loc.longitude, 1e-9)
        // CLLocation(latitude:longitude:) leaves speed/course at -1 -> normalized to null.
        assertNull(loc.speed)
        assertNull(loc.bearing)
        assertEquals("2026-05-12T07:14:00.000Z", loc.timestamp)
    }

    @Test
    fun negativeHorizontalAccuracyIsRejected() {
        val loc = CLLocation(
            coordinate = CLLocationCoordinate2DMake(48.8566, 2.3522),
            altitude = 0.0,
            horizontalAccuracy = -1.0,
            verticalAccuracy = -1.0,
            timestamp = NSDate(),
        ).toLocationOrNull(time)

        assertNull(loc, "a negative horizontalAccuracy means the coordinate is invalid")
    }
}
