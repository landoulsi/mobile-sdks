package com.landoulsi.diagnostic

import com.landoulsi.diagnostic.location.LocationDiagnosticsProvider
import com.landoulsi.diagnostic.location.LocationProviderType
import com.landoulsi.diagnostic.location.LocationStatusSnapshot
import com.landoulsi.diagnostic.network.NetworkDiagnosticsProvider
import com.landoulsi.diagnostic.network.NetworkStatusSnapshot
import com.landoulsi.diagnostic.network.NetworkTransportType
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CFNetwork.CFNetworkCopySystemProxySettings
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.Foundation.CFBridgingRelease
import platform.Foundation.NSDictionary

/**
 * iOS implementation of [NetworkDiagnosticsProvider].
 */
@OptIn(ExperimentalForeignApi::class)
class IosNetworkDiagnosticsProvider : NetworkDiagnosticsProvider {

    override suspend fun getNetworkSnapshot(): NetworkStatusSnapshot {
        val isProxy = checkProxyActive()
        return NetworkStatusSnapshot(
            isConnected = true,
            isVpnActive = false,
            isProxyActive = isProxy,
            signalStrengthPercent = null,
            transportType = NetworkTransportType.UNKNOWN
        )
    }

    private fun checkProxyActive(): Boolean {
        return try {
            val proxySettingsRef = CFNetworkCopySystemProxySettings() ?: return false
            val proxyDict = CFBridgingRelease(proxySettingsRef) as? NSDictionary ?: return false
            val httpProxy = proxyDict.objectForKey("HTTPProxy")
            val httpsProxy = proxyDict.objectForKey("HTTPSProxy")
            httpProxy != null || httpsProxy != null
        } catch (_: Exception) {
            false
        }
    }
}

/**
 * iOS implementation of [LocationDiagnosticsProvider] using [CLLocationManager].
 */
@OptIn(ExperimentalForeignApi::class)
class IosLocationDiagnosticsProvider : LocationDiagnosticsProvider {

    override suspend fun getLocationSnapshot(): LocationStatusSnapshot {
        return try {
            val isServicesEnabled = CLLocationManager.locationServicesEnabled()
            val authStatus = CLLocationManager.authorizationStatus()
            val isPermitted = authStatus == kCLAuthorizationStatusAuthorizedAlways ||
                authStatus == kCLAuthorizationStatusAuthorizedWhenInUse

            if (!isPermitted || !isServicesEnabled) {
                return LocationStatusSnapshot(
                    isLocationServicesEnabled = isServicesEnabled,
                    isPermissionGranted = isPermitted,
                    accuracyMeters = null,
                    providerType = if (isServicesEnabled) LocationProviderType.UNKNOWN else LocationProviderType.NONE
                )
            }

            val manager = CLLocationManager()
            val location = manager.location
            val accuracy = location?.horizontalAccuracy?.takeIf { it >= 0 }?.toFloat()

            LocationStatusSnapshot(
                isLocationServicesEnabled = true,
                isPermissionGranted = true,
                accuracyMeters = accuracy,
                providerType = LocationProviderType.GPS
            )
        } catch (_: Exception) {
            LocationStatusSnapshot(
                isLocationServicesEnabled = false,
                isPermissionGranted = false,
                accuracyMeters = null,
                providerType = LocationProviderType.NONE
            )
        }
    }
}
