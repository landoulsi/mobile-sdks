package com.landoulsi.payment.shared.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin

/**
 * iOS actual implementation of [createPlatformHttpClient].
 * Uses the Darwin (URLSession) engine which is the recommended iOS HTTP engine for Ktor.
 */
actual fun createPlatformHttpClient(
    baseUrl: String,
    enableLogging: Boolean
): HttpClient = createPaymentHttpClient(
    engineFactory = Darwin,
    baseUrl = baseUrl,
    enableLogging = enableLogging
)
