package com.landoulsi.payment.shared.network

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.plugins.api.ClientPlugin
import io.ktor.client.plugins.api.createClientPlugin
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

private val SENSITIVE_HEADERS = setOf(
    "authorization",
    "cookie",
    "set-cookie",
    "x-api-key",
    "api-key"
)

private val SENSITIVE_QUERY_PARAMS = setOf(
    "client_secret",
    "payment_intent",
    "token",
    "key",
    "api_key",
    "pan",
    "card",
    "cvc",
    "cvv"
)

/**
 * Sanitizes HTTP log messages to prevent credential, token, and secret leakage.
 *
 * Redacts sensitive HTTP headers and query parameters commonly found in payment URLs.
 */
internal fun sanitizeLogMessage(message: String): String {
    var sanitized = message
    for (header in SENSITIVE_HEADERS) {
        val pattern = Regex("(?i)($header:\\s*)(.+)")
        sanitized = pattern.replace(sanitized) { matchResult ->
            "${matchResult.groupValues[1]}[REDACTED]"
        }
    }
    for (param in SENSITIVE_QUERY_PARAMS) {
        val pattern = Regex("(?i)([?&]$param=)([^&\\s]+)")
        sanitized = pattern.replace(sanitized) { matchResult ->
            "${matchResult.groupValues[1]}[REDACTED]"
        }
    }
    return sanitized
}

/**
 * Default hosts preloaded with strict transport security for payment gateway traffic.
 *
 * These hosts must always be contacted over HTTPS. The list is configurable via
 * [HstsEnforcementConfig]; this set provides safe defaults for common Stripe endpoints.
 */
internal val DEFAULT_HSTS_PRELOAD_HOSTS = setOf(
    "api.stripe.com",
    "hooks.stripe.com"
)

/**
 * In-memory store for HSTS-covered hosts.
 *
 * Tracks exact hosts and parent domains marked with `includeSubDomains` so that
 * plain-HTTP requests can be rejected before they reach the network.
 */
internal class HstsHostStore(preloadHosts: Set<String>) {
    private val exactHosts = preloadHosts.toMutableSet()
    private val includedSubDomains = mutableSetOf<String>()

    fun addHost(host: String, includeSubDomains: Boolean) {
        exactHosts.add(host)
        if (includeSubDomains) {
            val parts = host.split(".")
            if (parts.size >= 2) {
                includedSubDomains.add(parts.takeLast(2).joinToString("."))
            }
        }
    }

    fun covers(host: String): Boolean {
        if (host in exactHosts) return true
        val parts = host.split(".")
        if (parts.size >= 2) {
            val parent = parts.takeLast(2).joinToString(".")
            if (parent in includedSubDomains) return true
        }
        return false
    }
}

/**
 * Configuration for the [HstsEnforcement] plugin.
 *
 * @property preloadHosts Hosts that are always required to use HTTPS.
 */
class HstsEnforcementConfig {
    var preloadHosts: Set<String> = DEFAULT_HSTS_PRELOAD_HOSTS
}

/**
 * Ktor client plugin that enforces HTTP Strict Transport Security (HSTS) for payment endpoints.
 *
 * - Rejects plain-HTTP requests to HSTS-covered hosts before they are sent.
 * - Records `Strict-Transport-Security` response headers to expand the HSTS host list dynamically.
 */
val HstsEnforcement: ClientPlugin<HstsEnforcementConfig> = createClientPlugin(
    name = "HstsEnforcement",
    createConfiguration = ::HstsEnforcementConfig
) {
    val store = HstsHostStore(pluginConfig.preloadHosts)

    onRequest { request, _ ->
        val url = request.url
        if (url.protocol.name != "https" && store.covers(url.host)) {
            throw IllegalArgumentException(
                "HSTS violation: plain-HTTP request to '${url.host}' is not allowed. " +
                    "Use HTTPS for payment operations."
            )
        }
    }

    onResponse { response ->
        val stsHeader = response.headers["Strict-Transport-Security"] ?: return@onResponse
        val host = response.call.request.url.host
        val maxAge = Regex("max-age=(\\d+)")
            .find(stsHeader)
            ?.groupValues
            ?.get(1)
            ?.toLongOrNull()
            ?: 0L
        if (maxAge > 0) {
            store.addHost(
                host,
                includeSubDomains = stsHeader.contains("includeSubDomains", ignoreCase = true)
            )
        }
    }
}

/**
 * Creates a platform-specific [HttpClient] using [engineFactory], configured with:
 *
 * - **HTTPS Enforcement**: Asserts [baseUrl] uses the `https://` scheme (unless empty or explicitly permitted for testing).
 * - **HSTS Enforcement**: Rejects plain-HTTP requests to HSTS-preloaded payment hosts and records
 *   `Strict-Transport-Security` response headers.
 * - **ContentNegotiation** using kotlinx.serialization JSON via [PaymentJson].
 * - **Logging** at [LogLevel.HEADERS] in debug builds with sensitive headers and URL parameters redacted.
 *   Request and response bodies are never logged.
 * - **Default request** headers: `Content-Type: application/json` and `Accept: application/json`.
 * - **Connect / request / socket timeouts** from [HttpTimeouts].
 *
 * @param engineFactory Platform engine factory (`OkHttp` on Android, `Darwin` on iOS).
 * @param baseUrl Optional base URL applied to every request via [defaultRequest]. Must begin with `https://`.
 * @param enableLogging Whether to install the [Logging] plugin (disable in production).
 * @param allowInsecureHttpForTesting Optional flag to allow `http://` for local testing/mock servers.
 * @param additionalConfig Optional block for customizing the client beyond the defaults.
 */
fun createPaymentHttpClient(
    engineFactory: HttpClientEngineFactory<*>,
    baseUrl: String = "",
    enableLogging: Boolean = false,
    allowInsecureHttpForTesting: Boolean = false,
    additionalConfig: HttpClientConfig<*>.() -> Unit = {}
): HttpClient {
    if (baseUrl.isNotEmpty()) {
        val lowerUrl = baseUrl.trim().lowercase()
        if (!allowInsecureHttpForTesting && !lowerUrl.startsWith("https://")) {
            throw IllegalArgumentException("Insecure HTTP endpoint rejected: '$baseUrl'. HTTPS is strictly required for payment operations.")
        }
    }

    return HttpClient(engineFactory) {
        expectSuccess = false

        install(ContentNegotiation) {
            json(PaymentJson)
        }

        install(HstsEnforcement)

        if (enableLogging) {
            install(Logging) {
                level = LogLevel.HEADERS
                logger = object : Logger {
                    override fun log(message: String) {
                        // Replace with platform-appropriate logging in production.
                        println("[PaymentSDK/HTTP] ${sanitizeLogMessage(message)}")
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
}

/**
 * Creates a platform-specific [HttpClient] using an instantiated [engine].
 */
fun createPaymentHttpClient(
    engine: io.ktor.client.engine.HttpClientEngine,
    baseUrl: String = "",
    enableLogging: Boolean = false,
    allowInsecureHttpForTesting: Boolean = false,
    additionalConfig: HttpClientConfig<*>.() -> Unit = {}
): HttpClient {
    if (baseUrl.isNotEmpty()) {
        val lowerUrl = baseUrl.trim().lowercase()
        if (!allowInsecureHttpForTesting && !lowerUrl.startsWith("https://")) {
            throw IllegalArgumentException("Insecure HTTP endpoint rejected: '$baseUrl'. HTTPS is strictly required for payment operations.")
        }
    }

    return HttpClient(engine) {
        expectSuccess = false

        install(ContentNegotiation) {
            json(PaymentJson)
        }

        install(HstsEnforcement)

        if (enableLogging) {
            install(Logging) {
                level = LogLevel.HEADERS
                logger = object : Logger {
                    override fun log(message: String) {
                        // Replace with platform-appropriate logging in production.
                        println("[PaymentSDK/HTTP] ${sanitizeLogMessage(message)}")
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
}
