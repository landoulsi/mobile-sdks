package com.trackmit.location.network

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Lenient [Json] used to decode third-party IP-geolocation responses.
 *
 * - `ignoreUnknownKeys`: these APIs return large, evolving payloads; we only read a few fields.
 * - `isLenient` / `coerceInputValues`: tolerate slightly malformed JSON and `null`s from free tiers.
 * - `explicitNulls = false`: absent fields stay `null` rather than failing decoding.
 */
internal val LocationJson: Json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
    explicitNulls = false
}

/** Connect/socket/request ceilings (ms). IP geolocation is best-effort — never let it hang. */
private const val CONNECT_TIMEOUT_MS = 5_000L
private const val REQUEST_TIMEOUT_MS = 10_000L
private const val SOCKET_TIMEOUT_MS = 10_000L

private fun HttpClientConfig<*>.locationDefaults() {
    expectSuccess = false
    install(ContentNegotiation) {
        json(LocationJson)
    }
    install(HttpTimeout) {
        connectTimeoutMillis = CONNECT_TIMEOUT_MS
        requestTimeoutMillis = REQUEST_TIMEOUT_MS
        socketTimeoutMillis = SOCKET_TIMEOUT_MS
    }
}

/**
 * Builds the small [HttpClient] used by the IP-based location provider.
 *
 * Deliberately minimal — content negotiation with [LocationJson] and hard timeouts.
 * `expectSuccess = false` so non-2xx responses are inspected by the caller rather than thrown
 * as exceptions that would tear down the updates flow.
 */
internal fun locationHttpClient(engineFactory: HttpClientEngineFactory<*>): HttpClient =
    HttpClient(engineFactory) { locationDefaults() }

/**
 * Overload accepting an already-instantiated [engine] (used with Ktor's `MockEngine` in tests).
 */
internal fun locationHttpClient(engine: HttpClientEngine): HttpClient =
    HttpClient(engine) { locationDefaults() }
