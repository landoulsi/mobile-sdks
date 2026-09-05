package com.landoulsi.integrity.mocklocation

import kotlinx.serialization.Serializable

/**
 * Signal identifiers and check tags emitted by the mock location detection vector.
 */
object MockLocationSignal {
    const val MOCK_FLAG_ACTIVE = "mock_location_flag_active"
    const val MOCK_PROVIDER_ACTIVE = "mock_location_provider_active"
    const val DEVELOPER_MOCK_SETTING = "mock_location_developer_setting"
    const val MOCK_APP_INSTALLED = "mock_location_app_installed"
    const val LOCATION_ANOMALY_VELOCITY = "mock_location_anomaly_velocity"
    const val LOCATION_ANOMALY_JUMP = "mock_location_anomaly_jump"
    const val LOCATION_ANOMALY_FROZEN = "mock_location_anomaly_frozen"

    /** Every signal id this vector can emit; used to seed the [com.landoulsi.integrity.IntegrityResult] catalog. */
    val all: Set<String> = setOf(
        MOCK_FLAG_ACTIVE,
        MOCK_PROVIDER_ACTIVE,
        DEVELOPER_MOCK_SETTING,
        MOCK_APP_INSTALLED,
        LOCATION_ANOMALY_VELOCITY,
        LOCATION_ANOMALY_JUMP,
        LOCATION_ANOMALY_FROZEN,
    )

    object Check {
        const val MOCK_FLAG = "mock_flag"
        const val MOCK_PROVIDER = "mock_provider"
        const val DEVELOPER_SETTING = "developer_setting"
        const val KNOWN_APP = "known_app"
        const val VELOCITY_ANOMALY = "velocity_anomaly"
        const val JUMP_ANOMALY = "jump_anomaly"
        const val FROZEN_ANOMALY = "frozen_anomaly"
    }
}

/**
 * Pure domain representation of a geographic location fix.
 *
 * @property latitude Latitude in degrees.
 * @property longitude Longitude in degrees.
 * @property altitude Altitude in meters above the WGS 84 reference ellipsoid, if available.
 * @property accuracy Estimated horizontal accuracy radius in meters, if available.
 * @property speed Instantaneous speed in meters per second, if available.
 * @property timestampMs Epoch timestamp in milliseconds when this fix was generated.
 * @property isMock Indicates whether the platform reported this fix as mock or simulated.
 */
@Serializable
data class LocationSample(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double? = null,
    val accuracy: Float? = null,
    val speed: Float? = null,
    val timestampMs: Long = 0L,
    val isMock: Boolean = false,
)

/**
 * Abstraction for platform-specific operations used by mock location detection heuristics.
 *
 * Decouples detection heuristics from Android framework types ([android.content.Context],
 * [android.location.LocationManager], [android.provider.Settings], [android.app.AppOpsManager])
 * to enable deterministic JVM host testing with fakes.
 */
interface MockLocationCheckContext {
    fun isMockLocationAppSet(): Boolean
    fun isMockProviderActive(): Boolean
    fun isDeveloperMockSettingEnabled(): Boolean
    fun isPackageInstalled(packageName: String): Boolean
    fun getRecentLocations(): List<LocationSample>
}
