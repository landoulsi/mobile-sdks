package com.landoulsi.integrity.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.provider.Settings
import java.net.NetworkInterface

/**
 * Android implementation of [NetworkCheckContext] querying platform connectivity services,
 * network interfaces, and system settings.
 *
 * Ensures [Context.getApplicationContext] is retained to prevent leaking short-lived
 * [android.app.Activity] contexts across background detection sweeps.
 *
 * @param context Android context used for system services; automatically coerced to application context.
 */
class AndroidNetworkCheckContext(
    context: Context,
) : NetworkCheckContext {

    private val applicationContext: Context = context.applicationContext ?: context

    override fun isVpnActive(): Boolean = try {
        val cm = applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

        val vpnViaConnectivity = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val activeNet = cm?.activeNetwork
            val activeCaps = activeNet?.let { cm.getNetworkCapabilities(it) }
            if (activeCaps?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true) {
                true
            } else {
                @Suppress("DEPRECATION")
                val networks = cm?.allNetworks
                networks?.any { net ->
                    cm.getNetworkCapabilities(net)?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true
                } ?: false
            }
        } else {
            @Suppress("DEPRECATION")
            cm?.getNetworkInfo(ConnectivityManager.TYPE_VPN)?.isConnectedOrConnecting == true
        }

        if (vpnViaConnectivity) {
            true
        } else {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            if (interfaces != null) {
                interfaces.asSequence().any { iface ->
                    try {
                        if (iface.isUp) {
                            val name = iface.name.lowercase()
                            name.startsWith("tun") ||
                                name.startsWith("tap") ||
                                name.startsWith("ppp")
                        } else {
                            false
                        }
                    } catch (_: Exception) {
                        false
                    }
                }
            } else {
                false
            }
        }
    } catch (_: Exception) {
        false
    }

    override fun isSystemProxyConfigured(): Boolean = try {
        // 1. Check JVM properties
        val httpHost = System.getProperty("http.proxyHost")
        val httpsHost = System.getProperty("https.proxyHost")
        if (!httpHost.isNullOrBlank() && httpHost != "0.0.0.0") return true
        if (!httpsHost.isNullOrBlank() && httpsHost != "0.0.0.0") return true

        // 2. Check ConnectivityManager proxy configurations
        val cm = applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val defaultProxy = cm?.defaultProxy
            if (defaultProxy != null && !defaultProxy.host.isNullOrBlank()) {
                return true
            }

            val activeNet = cm?.activeNetwork
            if (activeNet != null) {
                val linkProps = cm.getLinkProperties(activeNet)
                if (linkProps?.httpProxy != null && !linkProps.httpProxy?.host.isNullOrBlank()) {
                    return true
                }
            }
        }

        // 3. Check Settings.Global and Settings.Secure proxy properties
        val httpProxy = Settings.Global.getString(applicationContext.contentResolver, Settings.Global.HTTP_PROXY)
        if (isConfiguredProxy(httpProxy)) {
            return true
        }

        @Suppress("DEPRECATION")
        val secureHttpProxy = Settings.Secure.getString(applicationContext.contentResolver, Settings.Secure.HTTP_PROXY)
        if (isConfiguredProxy(secureHttpProxy)) {
            return true
        }

        false
    } catch (_: Exception) {
        false
    }

    private fun isConfiguredProxy(proxyString: String?): Boolean {
        if (proxyString.isNullOrBlank()) return false
        val entry = proxyString.split(',').firstOrNull()?.trim() ?: return false
        val host = if (':' in entry) {
            entry.substringBefore(':').trim()
        } else {
            entry
        }
        if (host.isEmpty() || host == "0.0.0.0") return false
        if (':' in entry) {
            val portStr = entry.substringAfter(':').trim()
            val port = portStr.toIntOrNull()
            if (port != null && port <= 0) return false
        }
        return true
    }

    override fun isAdbEnabled(): Boolean = try {
        val adbGlobal = Settings.Global.getInt(
            applicationContext.contentResolver,
            Settings.Global.ADB_ENABLED,
            0,
        )
        if (adbGlobal != 0) return true

        @Suppress("DEPRECATION")
        val adbSecure = Settings.Secure.getInt(
            applicationContext.contentResolver,
            Settings.Secure.ADB_ENABLED,
            0,
        )
        adbSecure != 0
    } catch (_: Exception) {
        false
    }
}
