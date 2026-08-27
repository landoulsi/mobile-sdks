package com.landoulsi.payment.shared.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import okhttp3.ConnectionSpec
import okhttp3.TlsVersion

/**
 * Android actual implementation of [createPlatformHttpClient].
 * Uses the OkHttp engine which is the recommended Android HTTP engine for Ktor.
 * Explicitly enforces modern TLS (TLS 1.2 and TLS 1.3) connection specifications.
 */
actual fun createPlatformHttpClient(
    baseUrl: String,
    enableLogging: Boolean
): HttpClient = createPaymentHttpClient(
    engine = OkHttp.create {
        config {
            val modernSpec = ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                .tlsVersions(TlsVersion.TLS_1_3, TlsVersion.TLS_1_2)
                .build()
            val restrictedSpec = ConnectionSpec.Builder(ConnectionSpec.RESTRICTED_TLS)
                .tlsVersions(TlsVersion.TLS_1_3, TlsVersion.TLS_1_2)
                .build()
            connectionSpecs(listOf(restrictedSpec, modernSpec))
        }
    },
    baseUrl = baseUrl,
    enableLogging = enableLogging
)
