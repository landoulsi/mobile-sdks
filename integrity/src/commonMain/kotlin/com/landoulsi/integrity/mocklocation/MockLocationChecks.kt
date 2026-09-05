package com.landoulsi.integrity.mocklocation

import com.landoulsi.integrity.model.IntegrityCategory
import com.landoulsi.integrity.model.IntegritySignal
import com.landoulsi.integrity.model.SignalSeverity
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.time.Clock

/** Must stay 1:1 with the `<queries>` package visibility declarations in AndroidManifest.xml. */
internal val KNOWN_MOCK_LOCATION_PACKAGES = listOf(
    "com.lexa.fakegps",
    "com.rosteam.gpsemulator",
    "com.incorporateapps.fakegps.fre",
    "com.incorporateapps.fakegps",
    "com.fly.gps",
    "com.togglo.fakegps",
    "com.gsmartstudio.fakegps",
    "com.theappninjas.fakegpsjoystick",
    "com.fakegps.mock",
)

private const val EARTH_RADIUS_METERS = 6371000.0
private const val MAX_PLAUSIBLE_VELOCITY_MPS = 350.0 // ~1260 km/h
private const val MIN_VELOCITY_DELTA_SECONDS = 1.0
private const val MAX_JUMP_DISTANCE_METERS = 50000.0 // 50 km
private const val MAX_JUMP_TIME_SECONDS = 5.0
private const val MIN_FROZEN_SAMPLES = 5
private const val COORDINATE_EPSILON = 1e-6

internal fun haversineDistanceMeters(
    lat1: Double,
    lon1: Double,
    lat2: Double,
    lon2: Double,
): Double {
    val dLat = (lat2 - lat1) * (PI / 180.0)
    val dLon = (lon2 - lon1) * (PI / 180.0)
    val radLat1 = lat1 * (PI / 180.0)
    val radLat2 = lat2 * (PI / 180.0)

    val a = (sin(dLat / 2.0).pow(2) + cos(radLat1) * cos(radLat2) * sin(dLon / 2.0).pow(2)).coerceIn(0.0, 1.0)
    val c = 2.0 * atan2(sqrt(a), sqrt(1.0 - a))
    return EARTH_RADIUS_METERS * c
}

internal fun checkMockLocationFlag(context: MockLocationCheckContext): IntegritySignal? {
    val locations = try {
        context.getRecentLocations()
    } catch (_: Exception) {
        emptyList()
    }

    val mockSample = locations.firstOrNull { it.isMock } ?: return null

    return IntegritySignal(
        id = MockLocationSignal.MOCK_FLAG_ACTIVE,
        name = "Mock Location Flag Active",
        category = IntegrityCategory.MOCK_LOCATION,
        severity = SignalSeverity.HIGH,
        confidence = 1.0,
        details = "Platform location sample reported isMock flag as true",
        detectedAt = Clock.System.now().toEpochMilliseconds(),
        metadata = mapOf(
            "timestamp" to mockSample.timestampMs.toString(),
            "check" to MockLocationSignal.Check.MOCK_FLAG,
        ),
    )
}

internal fun checkMockProviderActive(context: MockLocationCheckContext): IntegritySignal? {
    val isMockActive = try {
        context.isMockProviderActive()
    } catch (_: Exception) {
        false
    }

    if (!isMockActive) return null

    return IntegritySignal(
        id = MockLocationSignal.MOCK_PROVIDER_ACTIVE,
        name = "Mock Location Provider Active",
        category = IntegrityCategory.MOCK_LOCATION,
        severity = SignalSeverity.HIGH,
        confidence = 0.95,
        details = "Active mock or test location provider registered in system",
        detectedAt = Clock.System.now().toEpochMilliseconds(),
        metadata = mapOf("check" to MockLocationSignal.Check.MOCK_PROVIDER),
    )
}

internal fun checkDeveloperMockSettings(context: MockLocationCheckContext): IntegritySignal? {
    val isSettingEnabled = try {
        context.isDeveloperMockSettingEnabled() || context.isMockLocationAppSet()
    } catch (_: Exception) {
        false
    }

    if (!isSettingEnabled) return null

    return IntegritySignal(
        id = MockLocationSignal.DEVELOPER_MOCK_SETTING,
        name = "Developer Mock Location Setting Enabled",
        category = IntegrityCategory.MOCK_LOCATION,
        severity = SignalSeverity.MEDIUM,
        confidence = 0.85,
        details = "Device has mock location developer options enabled or mock app selected",
        detectedAt = Clock.System.now().toEpochMilliseconds(),
        metadata = mapOf("check" to MockLocationSignal.Check.DEVELOPER_SETTING),
    )
}

internal fun checkKnownMockApps(context: MockLocationCheckContext): List<IntegritySignal> {
    val currentTimestampMs = Clock.System.now().toEpochMilliseconds()
    return KNOWN_MOCK_LOCATION_PACKAGES.filter { packageName ->
        try {
            context.isPackageInstalled(packageName)
        } catch (_: Exception) {
            false
        }
    }.map { packageName ->
        IntegritySignal(
            id = MockLocationSignal.MOCK_APP_INSTALLED,
            name = "GPS Spoofing Application Detected",
            category = IntegrityCategory.MOCK_LOCATION,
            severity = SignalSeverity.HIGH,
            confidence = 0.9,
            details = "Known GPS spoofing package is installed: $packageName",
            detectedAt = currentTimestampMs,
            metadata = mapOf(
                "package" to packageName,
                "check" to MockLocationSignal.Check.KNOWN_APP,
            ),
        )
    }
}

internal fun checkVelocityAndJumpAnomalies(context: MockLocationCheckContext): List<IntegritySignal> {
    val locations = try {
        context.getRecentLocations()
    } catch (_: Exception) {
        return emptyList()
    }

    if (locations.size < 2) return emptyList()

    val sorted = locations.sortedBy { it.timestampMs }
    val signals = mutableListOf<IntegritySignal>()
    val currentTimestampMs = Clock.System.now().toEpochMilliseconds()

    var jumpFlagged = false
    var velocityFlagged = false

    for (i in 0 until sorted.size - 1) {
        val loc1 = sorted[i]
        val loc2 = sorted[i + 1]

        val deltaMs = loc2.timestampMs - loc1.timestampMs
        if (deltaMs <= 0L) continue

        val deltaSec = deltaMs / 1000.0
        val distMeters = haversineDistanceMeters(loc1.latitude, loc1.longitude, loc2.latitude, loc2.longitude)
        val calculatedSpeedMps = distMeters / deltaSec

        if (!jumpFlagged && distMeters > MAX_JUMP_DISTANCE_METERS && deltaSec <= MAX_JUMP_TIME_SECONDS) {
            jumpFlagged = true
            signals.add(
                IntegritySignal(
                    id = MockLocationSignal.LOCATION_ANOMALY_JUMP,
                    name = "Instantaneous Location Jump Detected",
                    category = IntegrityCategory.MOCK_LOCATION,
                    severity = SignalSeverity.CRITICAL,
                    confidence = 0.9,
                    details = "Impossible location jump: ${distMeters.roundToInt()}m in ${deltaSec}s",
                    detectedAt = currentTimestampMs,
                    metadata = mapOf(
                        "distance_meters" to distMeters.toString(),
                        "delta_seconds" to deltaSec.toString(),
                        "check" to MockLocationSignal.Check.JUMP_ANOMALY,
                    ),
                ),
            )
        }

        if (!velocityFlagged && deltaSec >= MIN_VELOCITY_DELTA_SECONDS && calculatedSpeedMps > MAX_PLAUSIBLE_VELOCITY_MPS) {
            velocityFlagged = true
            signals.add(
                IntegritySignal(
                    id = MockLocationSignal.LOCATION_ANOMALY_VELOCITY,
                    name = "Location Velocity Anomaly Detected",
                    category = IntegrityCategory.MOCK_LOCATION,
                    severity = SignalSeverity.HIGH,
                    confidence = 0.85,
                    details = "Unrealistic speed calculated between consecutive fixes: ${calculatedSpeedMps.roundToInt()} m/s",
                    detectedAt = currentTimestampMs,
                    metadata = mapOf(
                        "speed_mps" to calculatedSpeedMps.toString(),
                        "distance_meters" to distMeters.toString(),
                        "check" to MockLocationSignal.Check.VELOCITY_ANOMALY,
                    ),
                ),
            )
        }

        if (jumpFlagged && velocityFlagged) break
    }

    return signals
}

internal fun checkFrozenLocationAnomaly(context: MockLocationCheckContext): IntegritySignal? {
    val locations = try {
        context.getRecentLocations()
    } catch (_: Exception) {
        return null
    }

    if (locations.size < MIN_FROZEN_SAMPLES) return null

    val first = locations.first()
    val isAllSameCoordinates = locations.all { sample ->
        abs(sample.latitude - first.latitude) < COORDINATE_EPSILON &&
            abs(sample.longitude - first.longitude) < COORDINATE_EPSILON
    }

    if (!isAllSameCoordinates) return null

    // Check if speed is 0.0 or null and accuracy is zero or constant across all samples
    val isZeroSpeed = locations.all { (it.speed ?: 0.0f) == 0.0f }
    val isZeroOrConstantAccuracy = locations.all { it.accuracy == first.accuracy }

    if (isZeroSpeed && isZeroOrConstantAccuracy) {
        return IntegritySignal(
            id = MockLocationSignal.LOCATION_ANOMALY_FROZEN,
            name = "Frozen Location Stream Detected",
            category = IntegrityCategory.MOCK_LOCATION,
            severity = SignalSeverity.LOW,
            confidence = 0.7,
            details = "Zero-variance static coordinate updates detected across ${locations.size} fixes",
            detectedAt = Clock.System.now().toEpochMilliseconds(),
            metadata = mapOf(
                "sample_count" to locations.size.toString(),
                "check" to MockLocationSignal.Check.FROZEN_ANOMALY,
            ),
        )
    }

    return null
}
