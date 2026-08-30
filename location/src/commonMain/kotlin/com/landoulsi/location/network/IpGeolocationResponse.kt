package com.landoulsi.location.network

import kotlinx.serialization.Serializable

/**
 * Subset of the JSON returned by keyless IP-geolocation services (default: `https://ipwho.is/`).
 *
 * Every field is nullable on purpose: these services answer with HTTP 200 even on failure
 * (`{"success": false, ...}` or `{"error": true, "reason": "RateLimited"}`), so the presence
 * and validity of [latitude] / [longitude] must be checked by the caller rather than assumed.
 */
@Serializable
internal data class IpGeolocationResponse(
    /** `ipwho.is` success flag. `false` on rate-limit / lookup failure; absent on some services. */
    val success: Boolean? = null,
    /** Generic error flag used by other services (e.g. `ipapi.co`). */
    val error: Boolean? = null,
    /** Human-readable failure text — `ipwho.is` uses `message`, `ipapi.co` uses `reason`. */
    val message: String? = null,
    val reason: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
) {
    /** True when the payload explicitly signals a failed lookup. */
    val isFailure: Boolean
        get() = success == false || error == true

    /** Best available human-readable failure text, or `null` if none was provided. */
    val failureText: String?
        get() = message ?: reason
}
