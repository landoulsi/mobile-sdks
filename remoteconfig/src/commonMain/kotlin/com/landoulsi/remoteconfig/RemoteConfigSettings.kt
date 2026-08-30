package com.landoulsi.remoteconfig

import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

/**
 * Configuration settings for remote config fetch intervals and network timeouts.
 */
data class RemoteConfigSettings(
    /**
     * Minimum interval between remote fetch requests.
     */
    val minimumFetchInterval: Duration = 12.hours,

    /**
     * Network timeout for fetch operations.
     */
    val fetchTimeout: Duration = 60.seconds,
)
