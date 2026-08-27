package com.trackflow.location

import kotlinx.coroutines.flow.Flow

interface LocationProvider {
    fun startTracking()
    fun stopTracking()

    /**
     * Location fixes while tracking is enabled.
     *
     * A provider backed by a streaming source keeps this flow open until [stopTracking] or
     * collection is cancelled. A one-shot source ([IpLocationProvider]) instead emits its single
     * coarse fix and then completes, so collect it with that in mind (`combine`/`merge` are
     * fine; don't `zip` it against a live stream).
     */
    fun locationUpdates(): Flow<Location>

    /**
     * A best-effort, most-recent fix, or `null` if none is available.
     *
     * Returns quickly with whatever is already known rather than waiting for [locationUpdates]
     * to produce a live fix:
     * - the platform providers return the OS-cached last location (may be stale, may be `null`
     *   before any fix has ever been acquired, and still needs the location permission);
     * - [IpLocationProvider] returns a coarse IP-derived fix that needs **no** permission —
     *   the intended way to show an approximate starting position before permission is granted.
     */
    suspend fun lastKnownLocation(): Location?
}
