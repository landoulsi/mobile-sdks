package com.trackflow.location

import dev.zacsweers.metro.Qualifier

@Qualifier
annotation class FusedProvider

@Qualifier
annotation class GpsProvider

/**
 * Marks the [LocationProvider] that derives an approximate, city-level fix from the caller's
 * public IP address — usable before the OS location permission has been granted.
 */
@Qualifier
annotation class IpProvider
