package com.landoulsi.payment.shared.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for the shared HTTP client factory and its security helpers.
 */
class HttpClientFactoryTest {

    // ─────────────────────────────────────────────────────────
    //  PaymentJson configuration
    // ─────────────────────────────────────────────────────────

    @Serializable
    private data class SampleConfig(
        val name: String,
        val enabled: Boolean = true,
        val count: Int = 0
    )

    @Test
    fun testPaymentJsonIgnoresUnknownKeys() {
        val json = """{"name":"test","enabled":false,"extra_field":"ignored","nested":{"x":1}}"""
        val parsed = PaymentJson.decodeFromString(SampleConfig.serializer(), json)

        assertEquals("test", parsed.name)
        assertEquals(false, parsed.enabled)
        assertEquals(0, parsed.count)
    }

    @Test
    fun testPaymentJsonEncodesDefaults() {
        val config = SampleConfig(name = "defaults")
        val json = PaymentJson.encodeToString(config)

        assertTrue(json.contains("\"name\":\"defaults\""))
        assertTrue(json.contains("\"enabled\":true"))
        assertTrue(json.contains("\"count\":0"))
    }

    @Test
    fun testPaymentJsonIsLenientWithUnquotedKeys() {
        val json = """{name:"lenient", enabled: true}"""
        val parsed = PaymentJson.decodeFromString(SampleConfig.serializer(), json)

        assertEquals("lenient", parsed.name)
        assertTrue(parsed.enabled)
    }

    @Test
    fun testPaymentJsonCoercesNullToDefault() {
        val json = """{"name":"coerce","enabled":null,"count":null}"""
        val parsed = PaymentJson.decodeFromString(SampleConfig.serializer(), json)

        assertEquals("coerce", parsed.name)
        assertTrue(parsed.enabled)
        assertEquals(0, parsed.count)
    }

    @Test
    fun testPaymentJsonOmitsExplicitNulls() {
        @Serializable
        data class NullableHolder(val value: String? = null)

        val json = PaymentJson.encodeToString(NullableHolder(value = null))
        assertFalse(json.contains("value"))
    }

    // ─────────────────────────────────────────────────────────
    //  sanitizeLogMessage
    // ─────────────────────────────────────────────────────────

    @Test
    fun testSanitizeLogMessageRedactsAllSensitiveHeaders() {
        val raw = buildString {
            appendLine("GET https://api.stripe.com/v1/payment_intents/pi_123 HTTP/1.1")
            appendLine("Authorization: Bearer pk_test_abc123")
            appendLine("X-Api-Key: secret_api_key")
            appendLine("api-key: another_key")
            appendLine("Cookie: session=super_secret")
            appendLine("Set-Cookie: session=super_secret; Path=/")
            appendLine("Content-Type: application/json")
        }

        val sanitized = sanitizeLogMessage(raw)

        assertFalse(sanitized.contains("pk_test_abc123"))
        assertFalse(sanitized.contains("secret_api_key"))
        assertFalse(sanitized.contains("another_key"))
        assertFalse(sanitized.contains("session=super_secret"))
        assertTrue(sanitized.contains("Authorization: [REDACTED]"))
        assertTrue(sanitized.contains("X-Api-Key: [REDACTED]"))
        assertTrue(sanitized.contains("api-key: [REDACTED]"))
        assertTrue(sanitized.contains("Cookie: [REDACTED]"))
        assertTrue(sanitized.contains("Set-Cookie: [REDACTED]"))
        assertTrue(sanitized.contains("Content-Type: application/json"))
    }

    @Test
    fun testSanitizeLogMessageRedactsSensitiveQueryParameters() {
        val raw = "https://api.stripe.com/v1/payment_intents?client_secret=pi_123_secret&" +
            "payment_intent=pi_123&token=tok_abc&key=pk_123&api_key=ak_123&" +
            "pan=4242424242424242&card=4111111111111111&cvc=123&cvv=456&safe=value"

        val sanitized = sanitizeLogMessage(raw)

        assertFalse(sanitized.contains("pi_123_secret"))
        assertFalse(sanitized.contains("tok_abc"))
        assertFalse(sanitized.contains("pk_123"))
        assertFalse(sanitized.contains("ak_123"))
        assertFalse(sanitized.contains("4242424242424242"))
        assertFalse(sanitized.contains("4111111111111111"))
        assertFalse(sanitized.contains("cvc=123"))
        assertFalse(sanitized.contains("cvv=456"))
        assertTrue(sanitized.contains("client_secret=[REDACTED]"))
        assertTrue(sanitized.contains("payment_intent=[REDACTED]"))
        assertTrue(sanitized.contains("safe=value"))
    }

    @Test
    fun testSanitizeLogMessageIsCaseInsensitive() {
        val raw = "Authorization: bearer pk_lowercase\nCLIENT_SECRET=secret_value\nToken=Tok_value"
        val sanitized = sanitizeLogMessage(raw)

        assertFalse(sanitized.contains("pk_lowercase"))
        assertFalse(sanitized.contains("secret_value"))
        assertFalse(sanitized.contains("Tok_value"))
    }

    // ─────────────────────────────────────────────────────────
    //  HstsHostStore
    // ─────────────────────────────────────────────────────────

    @Test
    fun testHstsHostStoreCoversExactHost() {
        val store = HstsHostStore(emptySet())
        store.addHost("api.stripe.com", includeSubDomains = false)

        assertTrue(store.covers("api.stripe.com"))
        assertFalse(store.covers("hooks.stripe.com"))
        assertFalse(store.covers("example.com"))
    }

    @Test
    fun testHstsHostStoreCoversSubDomainsWhenEnabled() {
        val store = HstsHostStore(emptySet())
        store.addHost("stripe.com", includeSubDomains = true)

        assertTrue(store.covers("stripe.com"))
        assertTrue(store.covers("api.stripe.com"))
        assertTrue(store.covers("hooks.stripe.com"))
        assertFalse(store.covers("notstripe.com"))
        assertFalse(store.covers("com"))
    }

    @Test
    fun testHstsHostStorePreloadedDefaults() {
        val store = HstsHostStore(DEFAULT_HSTS_PRELOAD_HOSTS)

        assertTrue(store.covers("api.stripe.com"))
        assertTrue(store.covers("hooks.stripe.com"))
        assertFalse(store.covers("api.example.com"))
    }

    // ─────────────────────────────────────────────────────────
    //  HstsEnforcement plugin
    // ─────────────────────────────────────────────────────────

    @Test
    fun testHstsEnforcementRejectsHttpToPreloadedHost() = runTest {
        val client = HttpClient(MockEngine { respond("", HttpStatusCode.OK) }) {
            install(HstsEnforcement)
        }

        val ex = assertFailsWith<IllegalArgumentException> {
            client.get {
                url {
                    protocol = URLProtocol.HTTP
                    host = "api.stripe.com"
                }
            }
        }

        assertTrue(ex.message!!.contains("HSTS violation"))
    }

    @Test
    fun testHstsEnforcementAllowsHttpToNonCoveredHost() = runTest {
        var requestReceived = false
        val client = HttpClient(MockEngine { requestReceived = true; respond("", HttpStatusCode.OK) }) {
            install(HstsEnforcement)
        }

        client.get {
            url {
                protocol = URLProtocol.HTTP
                host = "insecure.example.com"
            }
        }

        assertTrue(requestReceived)
    }

    @Test
    fun testHstsEnforcementRecordsStrictTransportSecurityHeader() = runTest {
        val client = HttpClient(MockEngine { respond("", HttpStatusCode.OK, headersOf(
            "Strict-Transport-Security" to listOf("max-age=31536000; includeSubDomains")
        )) }) {
            install(HstsEnforcement)
        }

        // First request over HTTPS records the HSTS policy.
        client.get {
            url {
                protocol = URLProtocol.HTTPS
                host = "recorded.example.com"
            }
        }

        // Subsequent plain-HTTP request to the same host (or subdomain) must be rejected.
        val ex = assertFailsWith<IllegalArgumentException> {
            client.get {
                url {
                    protocol = URLProtocol.HTTP
                    host = "recorded.example.com"
                }
            }
        }
        assertTrue(ex.message!!.contains("HSTS violation"))

        // And a subdomain must be covered because includeSubDomains was set.
        val subEx = assertFailsWith<IllegalArgumentException> {
            client.get {
                url {
                    protocol = URLProtocol.HTTP
                    host = "sub.recorded.example.com"
                }
            }
        }
        assertTrue(subEx.message!!.contains("HSTS violation"))
    }

    @Test
    fun testHstsEnforcementIgnoresInvalidMaxAge() = runTest {
        var requestReceived = false
        val client = HttpClient(MockEngine {
            requestReceived = true
            respond("", HttpStatusCode.OK, headersOf(
                "Strict-Transport-Security" to listOf("max-age=invalid")
            ))
        }) {
            install(HstsEnforcement)
        }

        client.get {
            url {
                protocol = URLProtocol.HTTPS
                host = "no-max-age.example.com"
            }
        }

        // Because max-age could not be parsed, the host should NOT be recorded,
        // so a subsequent plain-HTTP request must be allowed through.
        client.get {
            url {
                protocol = URLProtocol.HTTP
                host = "no-max-age.example.com"
            }
        }

        assertTrue(requestReceived, "HTTP request should reach the engine when max-age is invalid")
    }

    @Test
    fun testHstsEnforcementAllowsHttpsRequests() = runTest {
        var requestReceived = false
        val client = HttpClient(MockEngine { requestReceived = true; respond("", HttpStatusCode.OK) }) {
            install(HstsEnforcement)
        }

        client.get {
            url {
                protocol = URLProtocol.HTTPS
                host = "api.stripe.com"
            }
        }

        assertTrue(requestReceived)
    }

    // ─────────────────────────────────────────────────────────
    //  createPaymentHttpClient factory / configuration
    // ─────────────────────────────────────────────────────────

    @Test
    fun testCreatePaymentHttpClientRejectsHttpBaseUrl() {
        val ex = assertFailsWith<IllegalArgumentException> {
            createPaymentHttpClient(
                engineFactory = MockEngine,
                baseUrl = "http://api.insecure.com"
            )
        }
        assertTrue(ex.message!!.contains("Insecure HTTP endpoint rejected"))
    }

    @Test
    fun testCreatePaymentHttpClientAllowsHttpBaseUrlForTesting() {
        val client = createPaymentHttpClient(
            engineFactory = MockEngine,
            baseUrl = "http://localhost:8080",
            allowInsecureHttpForTesting = true
        ) {
            engine {
                addHandler { respond("") }
            }
        }
        assertNotNull(client)
    }

    @Test
    fun testCreatePaymentHttpClientAllowsEmptyBaseUrl() {
        val client = createPaymentHttpClient(
            engineFactory = MockEngine,
            baseUrl = ""
        ) {
            engine {
                addHandler { respond("") }
            }
        }
        assertNotNull(client)
    }

    @Test
    fun testCreatePaymentHttpClientEngineOverloadRejectsHttpBaseUrl() {
        val ex = assertFailsWith<IllegalArgumentException> {
            createPaymentHttpClient(
                engine = MockEngine { respond("") },
                baseUrl = "http://api.insecure.com"
            )
        }
        assertTrue(ex.message!!.contains("Insecure HTTP endpoint rejected"))
    }

    @Test
    fun testCreatePaymentHttpClientAppliesDefaultJsonContentType() = runTest {
        var capturedContentType: String? = null
        val mockEngine = MockEngine { request ->
            capturedContentType = request.headers[HttpHeaders.ContentType]
            respond("""{"id":"pi_test"}""", HttpStatusCode.OK, headersOf(
                HttpHeaders.ContentType to listOf("application/json")
            ))
        }

        val client = createPaymentHttpClient(
            engine = mockEngine,
            baseUrl = "https://api.stripe.com"
        )

        client.post("/payment_intents/pi_test/confirm") {
            // Empty body is fine for this test; we only care about headers.
        }

        assertEquals("application/json", capturedContentType)
    }

    @Test
    fun testCreatePaymentHttpClientInstallsHstsEnforcement() = runTest {
        val client = createPaymentHttpClient(
            engine = MockEngine { respond("", HttpStatusCode.OK) },
            baseUrl = "https://api.stripe.com"
        )

        val ex = assertFailsWith<IllegalArgumentException> {
            client.get {
                url {
                    protocol = URLProtocol.HTTP
                    host = "api.stripe.com"
                }
            }
        }
        assertTrue(ex.message!!.contains("HSTS violation"))
    }
}
