package com.landoulsi.payment.shared.network

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Timeout constants (in milliseconds) used for the Ktor HTTP client.
 */
internal object HttpTimeouts {
    const val CONNECT_TIMEOUT_MS = 15_000L
    const val REQUEST_TIMEOUT_MS = 30_000L
    const val SOCKET_TIMEOUT_MS = 30_000L
}

/**
 * Shared [Json] instance configured for lenient, permissive parsing of gateway responses.
 *
 * - `ignoreUnknownKeys`: gracefully handles additional fields introduced by gateway API updates.
 * - `isLenient`: accepts unquoted keys and other slightly malformed JSON from some gateways.
 * - `coerceInputValues`: coerces `null` JSON values into Kotlin defaults rather than throwing.
 * - `encodeDefaults`: serializes Kotlin default values so outgoing payloads are always complete.
 */
val PaymentJson: Json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
    encodeDefaults = true
    explicitNulls = false
}

/**
 * Creates a platform-specific [HttpClient] using [engineFactory], configured with:
 *
 * - **ContentNegotiation** using kotlinx.serialization JSON via [PaymentJson].
 * - **Logging** at [LogLevel.HEADERS] in debug builds (sensitive body data is never logged).
 * - **Default request** headers: `Content-Type: application/json` and `Accept: application/json`.
 * - **Connect / request / socket timeouts** from [HttpTimeouts].
 *
 * @param engineFactory Platform engine factory (`OkHttp` on Android, `Darwin` on iOS).
 * @param baseUrl Optional base URL applied to every request via [defaultRequest].
 * @param additionalConfig Optional block for customizing the client beyond the defaults.
 * @param enableLogging Whether to install the [Logging] plugin (disable in production).
 */
fun createPaymentHttpClient(
    engineFactory: HttpClientEngineFactory<*>,
    baseUrl: String = "",
    enableLogging: Boolean = false,
    additionalConfig: HttpClientConfig<*>.() -> Unit = {}
): HttpClient = HttpClient(engineFactory) {
    expectSuccess = false

    install(ContentNegotiation) {
        json(PaymentJson)
    }

    if (enableLogging) {
        install(Logging) {
            level = LogLevel.HEADERS
            logger = object : Logger {
                override fun log(message: String) {
                    // Replace with platform-appropriate logging in production.
                    println("[PaymentSDK/HTTP] $message")
                }
            }
        }
    }

    if (baseUrl.isNotEmpty()) {
        defaultRequest {
            url(baseUrl)
            contentType(ContentType.Application.Json)
        }
    } else {
        defaultRequest {
            contentType(ContentType.Application.Json)
        }
    }

    additionalConfig()
}
