package com.landoulsi.payment.shared.network

import io.ktor.client.HttpClient

/**
 * Platform-specific factory for the Ktor [HttpClient].
 *
 * Each target source set provides an `actual` implementation that installs
 * the appropriate engine:
 * - **androidMain** → OkHttp engine
 * - **iosMain** → Darwin (URLSession) engine
 */
expect fun createPlatformHttpClient(
    baseUrl: String = "",
    enableLogging: Boolean = false
): HttpClient
