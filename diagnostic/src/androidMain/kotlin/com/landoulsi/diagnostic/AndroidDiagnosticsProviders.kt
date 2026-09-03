package com.landoulsi.diagnostic

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.telephony.TelephonyManager
import com.landoulsi.diagnostic.location.LocationDiagnosticsProvider
import com.landoulsi.diagnostic.location.LocationStatusSnapshot
import com.landoulsi.diagnostic.location.LocationProviderType
import com.landoulsi.diagnostic.network.NetworkDiagnosticsProvider
import com.landoulsi.diagnostic.network.NetworkStatusSnapshot
import com.landoulsi.diagnostic.network.NetworkTransportType

/**
 * Android implementation of [NetworkDiagnosticsProvider] using system services.
 */
class AndroidNetworkDiagnosticsProvider(
    private val context: Context
) : NetworkDiagnosticsProvider {

    @SuppressLint("MissingPermission")
    override suspend fun getNetworkSnapshot(): NetworkStatusSnapshot {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return NetworkStatusSnapshot(
                isConnected = false,
                transportType = NetworkTransportType.NONE,
                details = mapOf("error" to "ConnectivityManager unavailable")
            )

        val activeNetwork = connectivityManager.activeNetwork
        if (activeNetwork == null) {
            return NetworkStatusSnapshot(
                isConnected = false,
                isVpnActive = false,
                isProxyActive = checkProxyActive(connectivityManager),
                transportType = NetworkTransportType.NONE
            )
        }

        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
            ?: return NetworkStatusSnapshot(
                isConnected = false,
                transportType = NetworkTransportType.NONE
            )

        val isVpn = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
        val isProxy = checkProxyActive(connectivityManager)

        val transportType = when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkTransportType.WIFI
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkTransportType.CELLULAR
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkTransportType.ETHERNET
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> NetworkTransportType.BLUETOOTH
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> NetworkTransportType.VPN
            else -> NetworkTransportType.OTHER
        }

        val signalPercent = resolveSignalPercentage(capabilities, transportType)
        val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)

        return NetworkStatusSnapshot(
            isConnected = hasInternet,
            isVpnActive = isVpn,
            isProxyActive = isProxy,
            signalStrengthPercent = signalPercent,
            transportType = transportType,
            details = buildMap {
                put("validated", capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED).toString())
            }
        )
    }

    private fun checkProxyActive(connectivityManager: ConnectivityManager): Boolean {
        try {
            val httpProxy = System.getProperty("http.proxyHost")
            val httpsProxy = System.getProperty("https.proxyHost")
            if (!httpProxy.isNullOrBlank() || !httpsProxy.isNullOrBlank()) {
                return true
            }
            if (connectivityManager.defaultProxy != null) {
                return true
            }
        } catch (_: Exception) {}
        return false
    }

    @Suppress("DEPRECATION")
    private fun resolveSignalPercentage(
        capabilities: NetworkCapabilities,
        transportType: NetworkTransportType
    ): Int? {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val signalStrength = capabilities.signalStrength
                if (signalStrength != NetworkCapabilities.SIGNAL_STRENGTH_UNSPECIFIED && signalStrength in 0..4) {
                    return signalStrength * 25
                }
            }

            if (transportType == NetworkTransportType.WIFI) {
                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                val info = wifiManager?.connectionInfo
                val rssi = info?.rssi
                if (rssi != null && rssi != -127) {
                    val level = WifiManager.calculateSignalLevel(rssi, 100)
                    return level.coerceIn(0, 100)
                }
            } else if (transportType == NetworkTransportType.CELLULAR) {
                val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val level = telephonyManager?.signalStrength?.level
                    if (level != null && level >= 0) {
                        return (level * 25).coerceIn(0, 100)
                    }
                }
            }
        } catch (_: Exception) {}
        return null
    }
}

/**
 * Android implementation of [LocationDiagnosticsProvider] using system [LocationManager].
 */
class AndroidLocationDiagnosticsProvider(
    private val context: Context
) : LocationDiagnosticsProvider {

    @SuppressLint("MissingPermission")
    override suspend fun getLocationSnapshot(): LocationStatusSnapshot {
        val hasFineLocation = context.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarseLocation = context.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasPermission = hasFineLocation || hasCoarseLocation

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return LocationStatusSnapshot(
                isLocationServicesEnabled = false,
                isPermissionGranted = hasPermission,
                providerType = LocationProviderType.NONE,
                details = mapOf("error" to "LocationManager unavailable")
            )

        val isServicesEnabled = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                locationManager.isLocationEnabled
            } else {
                locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                    locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
            }
        } catch (_: Exception) {
            false
        }

        if (!hasPermission || !isServicesEnabled) {
            return LocationStatusSnapshot(
                isLocationServicesEnabled = isServicesEnabled,
                isPermissionGranted = hasPermission,
                accuracyMeters = null,
                providerType = if (isServicesEnabled) LocationProviderType.UNKNOWN else LocationProviderType.NONE
            )
        }

        var bestLocation: Location? = null
        val providers = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        )

        for (provider in providers) {
            try {
                if (locationManager.isProviderEnabled(provider)) {
                    val loc = locationManager.getLastKnownLocation(provider)
                    if (loc != null) {
                        if (bestLocation == null || (loc.hasAccuracy() && (!bestLocation.hasAccuracy() || loc.accuracy < bestLocation.accuracy))) {
                            bestLocation = loc
                        }
                    }
                }
            } catch (_: SecurityException) {
                // Ignore permission error during individual provider query
            } catch (_: Exception) {}
        }

        val accuracy = bestLocation?.takeIf { it.hasAccuracy() }?.accuracy
        val providerType = when (bestLocation?.provider?.lowercase()) {
            LocationManager.GPS_PROVIDER.lowercase() -> LocationProviderType.GPS
            LocationManager.NETWORK_PROVIDER.lowercase() -> LocationProviderType.NETWORK
            LocationManager.PASSIVE_PROVIDER.lowercase() -> LocationProviderType.PASSIVE
            "fused" -> LocationProviderType.FUSED
            null -> if (isServicesEnabled) LocationProviderType.UNKNOWN else LocationProviderType.NONE
            else -> LocationProviderType.UNKNOWN
        }

        return LocationStatusSnapshot(
            isLocationServicesEnabled = isServicesEnabled,
            isPermissionGranted = hasPermission,
            accuracyMeters = accuracy,
            providerType = providerType,
            details = buildMap {
                put("hasFineLocation", hasFineLocation.toString())
                put("hasCoarseLocation", hasCoarseLocation.toString())
                bestLocation?.time?.let { put("fixTime", it.toString()) }
            }
        )
    }
}
