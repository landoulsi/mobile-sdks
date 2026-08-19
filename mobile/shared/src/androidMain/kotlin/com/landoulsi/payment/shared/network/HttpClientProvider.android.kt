package com.landoulsi.payment.shared.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp

/**
 * Android actual implementation of [createPlatformHttpClient].
 * Uses the OkHttp engine which is the recommended Android HTTP engine for Ktor.
 */
actual fun createPlatformHttpClient(
    baseUrl: String,
    enableLogging: Boolean
): HttpClient = createPaymentHttpClient(
    engineFactory = OkHttp,
    baseUrl = baseUrl,
    enableLogging = enableLogging
)
