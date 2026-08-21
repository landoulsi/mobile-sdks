package com.landoulsi.payment.shared.network

import com.landoulsi.payment.shared.network.dto.CardTokenRequest
import com.landoulsi.payment.shared.network.dto.GooglePayGatewayToken
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class KtorGatewayClientTest {

    private val baseUrl = "https://api.stripe.com/v1"
    private val publishableKey = "pk_test_abc123"

    private fun createMockClient(engine: MockEngine) = HttpClient(engine) {
        install(ContentNegotiation) {
            json(PaymentJson)
        }
        defaultRequest {
            contentType(ContentType.Application.Json)
        }
        expectSuccess = false
    }

    @Test
    fun testTokenizeCardSuccess() = runTest {
        val responseBody = """
            {
              "id": "tok_visa_4242",
              "object": "token",
              "created": 1709123456,
              "livemode": false,
              "type": "card",
              "card": {
                "id": "card_123",
                "brand": "visa",
                "last4": "4242",
                "exp_month": 12,
                "exp_year": 2028,
                "funding": "credit",
                "country": "US"
              }
            }
        """.trimIndent()

        var capturedUrl: String? = null
        var capturedAuth: String? = null

        val mockEngine = MockEngine { request ->
            capturedUrl = request.url.toString()
            capturedAuth = request.headers[HttpHeaders.Authorization]
            respond(
                content = responseBody,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val client = KtorGatewayClient(
            httpClient = createMockClient(mockEngine),
            baseUrl = baseUrl,
            publishableKey = publishableKey
        )

        val request = CardTokenRequest(
            number = "4242424242424242",
            expiryMonth = 12,
            expiryYear = 2028,
            cvc = "123",
            cardholderName = "Test User"
        )

        val result = client.tokenizeCard(request)

        assertEquals("tok_visa_4242", result.id)
        assertEquals("token", result.`object`)
        assertEquals("card", result.type)
        assertNotNull(result.card)
        assertEquals("visa", result.card!!.brand)
        assertEquals("4242", result.card.last4)
        assertEquals(12, result.card.expMonth)
        assertEquals(2028, result.card.expYear)
        assertEquals("credit", result.card.funding)
        assertEquals("US", result.card.country)
        assertEquals(1709123456L, result.created)
        assertEquals(false, result.livemode)

        assertNotNull(capturedUrl)
        assertTrue(capturedUrl!!.endsWith("/tokens"))
        assertEquals("Bearer $publishableKey", capturedAuth)
    }

    @Test
    fun testTokenizeCardMinimalResponse() = runTest {
        val mockEngine = MockEngine {
            respond(
                content = """{"id": "tok_minimal"}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val client = KtorGatewayClient(createMockClient(mockEngine), baseUrl, publishableKey)
        val result = client.tokenizeCard(CardTokenRequest("4242424242424242", 12, 2028, "123"))

        assertEquals("tok_minimal", result.id)
        assertNull(result.`object`)
        assertNull(result.card)
        assertNull(result.created)
    }

    @Test
    fun testTokenizeCardSendsBearerAuthorization() = runTest {
        var capturedAuth: String? = null

        val mockEngine = MockEngine {
            capturedAuth = it.headers[HttpHeaders.Authorization]
            respond(
                content = """{"id": "tok_test"}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val client = KtorGatewayClient(createMockClient(mockEngine), baseUrl, "pk_live_secret_key")
        client.tokenizeCard(CardTokenRequest("4242424242424242", 12, 2028, "123"))

        assertEquals("Bearer pk_live_secret_key", capturedAuth)
    }

    @Test
    fun testTokenizeCardThrowsGatewayExceptionOn4xx() = runTest {
        val errorBody = """
            {
              "error": {
                "type": "card_error",
                "code": "card_declined",
                "message": "Your card was declined.",
                "decline_code": "insufficient_funds"
              }
            }
        """.trimIndent()

        val mockEngine = MockEngine {
            respond(
                content = errorBody,
                status = HttpStatusCode.PaymentRequired,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val client = KtorGatewayClient(createMockClient(mockEngine), baseUrl, publishableKey)
        val ex = assertFailsWith<GatewayException> {
            client.tokenizeCard(CardTokenRequest("4000000000000002", 12, 2028, "123"))
        }
        assertEquals(402, ex.statusCode)
        assertEquals("card_declined", ex.gatewayCode)
        assertEquals("insufficient_funds", ex.declineCode)
        assertEquals("Your card was declined.", ex.message)
    }

    @Test
    fun testTokenizeCardThrowsGatewayExceptionOn5xx() = runTest {
        val mockEngine = MockEngine {
            respond(
                content = """{"error": {"message": "Internal error"}}""",
                status = HttpStatusCode.InternalServerError,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val client = KtorGatewayClient(createMockClient(mockEngine), baseUrl, publishableKey)
        val ex = assertFailsWith<GatewayException> {
            client.tokenizeCard(CardTokenRequest("4242424242424242", 12, 2028, "123"))
        }
        assertEquals(500, ex.statusCode)
        assertEquals("Internal error", ex.message)
    }

    @Test
    fun testTokenizeGooglePaySuccess() = runTest {
        val responseBody = """
            {
              "id": "pm_google_pay_visa",
              "object": "payment_method"
            }
        """.trimIndent()

        var capturedUrl: String? = null
        var capturedAuth: String? = null

        val mockEngine = MockEngine { request ->
            capturedUrl = request.url.toString()
            capturedAuth = request.headers[HttpHeaders.Authorization]
            respond(
                content = responseBody,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val client = KtorGatewayClient(createMockClient(mockEngine), baseUrl, publishableKey)
        val result = client.tokenizeGooglePay("gpay_encrypted_token_string")

        assertEquals("pm_google_pay_visa", result.id)
        assertEquals("payment_method", result.`object`)
        assertTrue(capturedUrl!!.endsWith("/payment_methods"))
        assertEquals("Bearer $publishableKey", capturedAuth)
    }

    @Test
    fun testTokenizeGooglePayThrowsOn4xx() = runTest {
        val errorBody = """
            {
              "error": {
                "type": "invalid_request_error",
                "code": "parameter_missing",
                "message": "Missing token parameter."
              }
            }
        """.trimIndent()

        val mockEngine = MockEngine {
            respond(
                content = errorBody,
                status = HttpStatusCode.BadRequest,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val client = KtorGatewayClient(createMockClient(mockEngine), baseUrl, publishableKey)
        val ex = assertFailsWith<GatewayException> {
            client.tokenizeGooglePay("bad_token")
        }
        assertEquals(400, ex.statusCode)
        assertEquals("parameter_missing", ex.gatewayCode)
        assertEquals("Missing token parameter.", ex.message)
    }

    @Test
    fun testTokenizeGooglePayThrowsOn5xx() = runTest {
        val mockEngine = MockEngine {
            respond(
                content = """{"error": {"message": "Service unavailable"}}""",
                status = HttpStatusCode.ServiceUnavailable,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val client = KtorGatewayClient(createMockClient(mockEngine), baseUrl, publishableKey)
        val ex = assertFailsWith<GatewayException> {
            client.tokenizeGooglePay("token_val")
        }
        assertEquals(503, ex.statusCode)
    }

    @Test
    fun testTokenizeGooglePayNullObjectField() = runTest {
        val mockEngine = MockEngine {
            respond(
                content = """{"id": "pm_no_object"}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val client = KtorGatewayClient(createMockClient(mockEngine), baseUrl, publishableKey)
        val result = client.tokenizeGooglePay("token")
        assertEquals("pm_no_object", result.id)
        assertNull(result.`object`)
    }

    @Test
    fun testTokenizeGooglePayMinimalResponseBody() = runTest {
        val mockEngine = MockEngine {
            respond(
                content = """{"id": "pm_minimal"}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val client = KtorGatewayClient(createMockClient(mockEngine), baseUrl, publishableKey)
        val result = client.tokenizeGooglePay("tok")
        assertEquals("pm_minimal", result.id)
    }

    // ─────────────────────────────────────────────────────────
    //  Edge cases — request body serialization
    // ─────────────────────────────────────────────────────────

    private fun io.ktor.client.request.HttpRequestData.bodyAsString(): String {
        return when (val b = body) {
            is io.ktor.http.content.OutgoingContent.ByteArrayContent -> b.bytes().decodeToString()
            is io.ktor.http.content.TextContent -> b.text
            else -> b.toString()
        }
    }

    @Test
    fun testGooglePayRequestBodyContainsToken() = runTest {
        var capturedBody: String? = null

        val mockEngine = MockEngine { request ->
            capturedBody = request.bodyAsString()
            respond(
                content = """{"id": "pm_ok"}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val client = KtorGatewayClient(createMockClient(mockEngine), baseUrl, publishableKey)
        client.tokenizeGooglePay("gpay_tok_abc123")

        assertNotNull(capturedBody)
        assertTrue(capturedBody!!.contains("gpay_tok_abc123"))
        assertTrue(capturedBody!!.contains("google_pay_token"))
    }

    @Test
    fun testCardRequestBodyContainsAllFields() = runTest {
        var capturedBody: String? = null

        val mockEngine = MockEngine { request ->
            capturedBody = request.bodyAsString()
            respond(
                content = """{"id": "tok_ok"}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val client = KtorGatewayClient(createMockClient(mockEngine), baseUrl, publishableKey)
        client.tokenizeCard(
            CardTokenRequest(
                number = "4242424242424242",
                expiryMonth = 6,
                expiryYear = 2029,
                cvc = "999",
                cardholderName = "Jane Doe"
            )
        )

        assertNotNull(capturedBody)
        assertTrue(capturedBody!!.contains("4242424242424242"))
        assertTrue(capturedBody!!.contains("Jane Doe"))
    }

    @Test
    fun testCardRequestBodyOmitsNullCardholderName() = runTest {
        var capturedBody: String? = null

        val mockEngine = MockEngine { request ->
            capturedBody = request.bodyAsString()
            respond(
                content = """{"id": "tok_ok"}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val client = KtorGatewayClient(createMockClient(mockEngine), baseUrl, publishableKey)
        client.tokenizeCard(
            CardTokenRequest(
                number = "4242424242424242",
                expiryMonth = 12,
                expiryYear = 2030,
                cvc = "111"
            )
        )

        assertNotNull(capturedBody)
        assertTrue(capturedBody!!.contains("4242424242424242"))
        assertFalse(capturedBody!!.contains("cardholderName"))
        assertFalse(capturedBody!!.contains("name"))
    }

    // ─────────────────────────────────────────────────────────
    //  Edge cases — malformed / empty responses
    // ─────────────────────────────────────────────────────────

    @Test
    fun testTokenizeCardThrowsOnMalformedJsonSuccess() = runTest {
        val mockEngine = MockEngine {
            respond(
                content = "not json",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val client = KtorGatewayClient(createMockClient(mockEngine), baseUrl, publishableKey)
        assertFailsWith<Exception> {
            client.tokenizeCard(CardTokenRequest("4242424242424242", 12, 2028, "123"))
        }
    }

    @Test
    fun testTokenizeGooglePayThrowsOnMalformedJsonSuccess() = runTest {
        val mockEngine = MockEngine {
            respond(
                content = "<<<invalid>>>",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val client = KtorGatewayClient(createMockClient(mockEngine), baseUrl, publishableKey)
        assertFailsWith<Exception> {
            client.tokenizeGooglePay("token_val")
        }
    }

    // ─────────────────────────────────────────────────────────
    //  Edge cases — URL construction
    // ─────────────────────────────────────────────────────────

    @Test
    fun testBaseUrlWithTrailingSlash_tokenizesCorrectly() = runTest {
        var capturedUrl: String? = null

        val mockEngine = MockEngine { request ->
            capturedUrl = request.url.toString()
            respond(
                content = """{"id": "tok_123"}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val client = KtorGatewayClient(createMockClient(mockEngine), "$baseUrl/", publishableKey)
        client.tokenizeCard(CardTokenRequest("4242424242424242", 12, 2028, "123"))

        assertNotNull(capturedUrl)
        assertTrue(capturedUrl!!.endsWith("/tokens"))
    }

    @Test
    fun testBaseUrlWithTrailingSlash_googlePay() = runTest {
        var capturedUrl: String? = null

        val mockEngine = MockEngine { request ->
            capturedUrl = request.url.toString()
            respond(
                content = """{"id": "pm_123"}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val client = KtorGatewayClient(createMockClient(mockEngine), "$baseUrl/", publishableKey)
        client.tokenizeGooglePay("token")

        assertNotNull(capturedUrl)
        assertTrue(capturedUrl!!.endsWith("/payment_methods"))
    }

    // ─────────────────────────────────────────────────────────
    //  Edge cases — error propagation
    // ─────────────────────────────────────────────────────────

    @Test
    fun testTokenizeCardErrorPreservesDeclineCode() = runTest {
        val errorBody = """
            {
              "error": {
                "type": "card_error",
                "code": "card_declined",
                "decline_code": "stolen_card",
                "message": "This card has been reported stolen."
              }
            }
        """.trimIndent()

        val mockEngine = MockEngine {
            respond(
                content = errorBody,
                status = HttpStatusCode.PaymentRequired,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val client = KtorGatewayClient(createMockClient(mockEngine), baseUrl, publishableKey)
        val ex = assertFailsWith<GatewayException> {
            client.tokenizeCard(CardTokenRequest("4000000000000002", 12, 2028, "123"))
        }
        assertEquals("stolen_card", ex.declineCode)
        assertEquals("card_declined", ex.gatewayCode)
    }

    @Test
    fun testTokenizeGooglePay4xxPreservesGatewayMessage() = runTest {
        val errorBody = """
            {
              "error": {
                "type": "invalid_request_error",
                "message": "No such customer: cus_xxx"
              }
            }
        """.trimIndent()

        val mockEngine = MockEngine {
            respond(
                content = errorBody,
                status = HttpStatusCode.NotFound,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val client = KtorGatewayClient(createMockClient(mockEngine), baseUrl, publishableKey)
        val ex = assertFailsWith<GatewayException> {
            client.tokenizeGooglePay("token")
        }
        assertEquals("No such customer: cus_xxx", ex.message)
        assertEquals(404, ex.statusCode)
    }

    @Test
    fun testTokenizeCardOnUnauthorized_includesAuthHeader() = runTest {
        var capturedAuth: String? = null

        val mockEngine = MockEngine { request ->
            capturedAuth = request.headers[HttpHeaders.Authorization]
            respond(
                content = """{"error": {"message": "Invalid API key"}}""",
                status = HttpStatusCode.Unauthorized,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val client = KtorGatewayClient(createMockClient(mockEngine), baseUrl, "pk_bad_key")
        assertFailsWith<GatewayException> {
            client.tokenizeCard(CardTokenRequest("4242424242424242", 12, 2028, "123"))
        }
        assertEquals("Bearer pk_bad_key", capturedAuth)
    }

    // ─────────────────────────────────────────────────────────
    //  PaymentIntent & 3D Secure tests
    // ─────────────────────────────────────────────────────────

    @Test
    fun testConfirmPaymentSuccess() = runTest {
        val responseBody = """
            {
              "id": "pi_12345",
              "object": "payment_intent",
              "status": "succeeded",
              "amount": 2999,
              "currency": "usd",
              "payment_method": "pm_card_visa"
            }
        """.trimIndent()

        var capturedUrl: String? = null
        var capturedAuth: String? = null

        val mockEngine = MockEngine { request ->
            capturedUrl = request.url.toString()
            capturedAuth = request.headers[HttpHeaders.Authorization]
            respond(
                content = responseBody,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val client = KtorGatewayClient(createMockClient(mockEngine), baseUrl, publishableKey)
        val result = client.confirmPayment(
            paymentIntentId = "pi_12345",
            paymentMethodId = "pm_card_visa",
            clientSecret = "pi_12345_secret_abc"
        )

        assertEquals("pi_12345", result.id)
        assertEquals("succeeded", result.status)
        assertEquals(2999L, result.amount)
        assertEquals("usd", result.currency)
        assertEquals("pm_card_visa", result.paymentMethod)
        assertTrue(capturedUrl!!.endsWith("/payment_intents/pi_12345/confirm"))
        assertEquals("Bearer $publishableKey", capturedAuth)
    }

    @Test
    fun testConfirmPaymentRequiresAction3DS() = runTest {
        val responseBody = """
            {
              "id": "pi_3ds_req",
              "object": "payment_intent",
              "status": "requires_action",
              "client_secret": "pi_3ds_secret_xyz",
              "next_action": {
                "type": "redirect_to_url",
                "redirect_to_url": {
                  "url": "https://hooks.stripe.com/three_d_secure/authenticate",
                  "return_url": "paymentsdk://3ds-complete"
                }
              }
            }
        """.trimIndent()

        val mockEngine = MockEngine {
            respond(
                content = responseBody,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val client = KtorGatewayClient(createMockClient(mockEngine), baseUrl, publishableKey)
        val result = client.confirmPayment(
            paymentIntentId = "pi_3ds_req",
            paymentMethodId = "pm_3ds_card",
            clientSecret = "pi_3ds_secret_xyz",
            returnUrl = "paymentsdk://3ds-complete"
        )

        assertEquals("pi_3ds_req", result.id)
        assertEquals("requires_action", result.status)
        assertEquals("pi_3ds_secret_xyz", result.clientSecret)
        assertNotNull(result.nextAction)
        assertEquals("redirect_to_url", result.nextAction!!.type)
        assertNotNull(result.nextAction?.redirectToUrl)
        assertEquals("https://hooks.stripe.com/three_d_secure/authenticate", result.nextAction?.redirectToUrl?.url)
        assertEquals("paymentsdk://3ds-complete", result.nextAction?.redirectToUrl?.returnUrl)
    }

    @Test
    fun testConfirmPaymentThrowsGatewayExceptionOnDeclined() = runTest {
        val errorBody = """
            {
              "error": {
                "type": "card_error",
                "code": "card_declined",
                "decline_code": "generic_decline",
                "message": "The card has been declined."
              }
            }
        """.trimIndent()

        val mockEngine = MockEngine {
            respond(
                content = errorBody,
                status = HttpStatusCode.PaymentRequired,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val client = KtorGatewayClient(createMockClient(mockEngine), baseUrl, publishableKey)
        val ex = assertFailsWith<GatewayException> {
            client.confirmPayment("pi_declined", "pm_declined", "secret")
        }
        assertEquals(402, ex.statusCode)
        assertEquals("card_declined", ex.gatewayCode)
        assertEquals("generic_decline", ex.declineCode)
    }

    @Test
    fun testComplete3DSAuthenticationSuccess() = runTest {
        val responseBody = """
            {
              "id": "pi_3ds_completed",
              "object": "payment_intent",
              "status": "succeeded",
              "payment_method": "pm_3ds_authenticated"
            }
        """.trimIndent()

        var capturedUrl: String? = null
        var capturedBody: String? = null

        val mockEngine = MockEngine { request ->
            capturedUrl = request.url.toString()
            capturedBody = request.bodyAsString()
            respond(
                content = responseBody,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val client = KtorGatewayClient(createMockClient(mockEngine), baseUrl, publishableKey)
        val result = client.complete3DSAuthentication(
            paymentIntentId = "pi_3ds_completed",
            clientSecret = "secret_complete"
        )

        assertEquals("pi_3ds_completed", result.id)
        assertEquals("succeeded", result.status)
        assertTrue(capturedUrl!!.endsWith("/payment_intents/pi_3ds_completed/confirm"))
        assertNotNull(capturedBody)
        assertTrue(capturedBody!!.contains("secret_complete"))
    }

    @Test
    fun testComplete3DSAuthenticationThrowsOn5xx() = runTest {
        val mockEngine = MockEngine {
            respond(
                content = """{"error": {"message": "Gateway temporarily unavailable"}}""",
                status = HttpStatusCode.ServiceUnavailable,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val client = KtorGatewayClient(createMockClient(mockEngine), baseUrl, publishableKey)
        val ex = assertFailsWith<GatewayException> {
            client.complete3DSAuthentication("pi_503", "secret")
        }
        assertEquals(503, ex.statusCode)
    }

    // ─────────────────────────────────────────────────────────
    //  Security Tests: HTTPS Enforcement & Log Sanitization
    // ─────────────────────────────────────────────────────────

    @Test
    fun testKtorGatewayClientRejectsHttpUrl() {
        val mockEngine = MockEngine { respond("") }
        val ex = assertFailsWith<IllegalArgumentException> {
            KtorGatewayClient(createMockClient(mockEngine), "http://insecure.gateway.com", publishableKey)
        }
        assertTrue(ex.message!!.contains("Insecure HTTP endpoint rejected"))
    }

    @Test
    fun testKtorGatewayClientAllowsHttpWhenTestingFlagEnabled() {
        val mockEngine = MockEngine { respond("") }
        val client = KtorGatewayClient(
            httpClient = createMockClient(mockEngine),
            baseUrl = "http://localhost:8080",
            publishableKey = publishableKey,
            allowInsecureHttpForTesting = true
        )
        assertNotNull(client)
    }

    @Test
    fun testCreatePaymentHttpClientRejectsHttpUrl() {
        val mockEngine = MockEngine { respond("") }
        val ex = assertFailsWith<IllegalArgumentException> {
            createPaymentHttpClient(
                engineFactory = MockEngine,
                baseUrl = "http://api.insecure.com"
            )
        }
        assertTrue(ex.message!!.contains("Insecure HTTP endpoint rejected"))
    }

    @Test
    fun testSanitizeLogMessageRedactsSensitiveHeaders() {
        val rawLog = "REQUEST: https://api.stripe.com/v1/tokens\nAuthorization: Bearer pk_test_123456789\nX-Api-Key: secret_key\nCookie: session=abc\nContent-Type: application/json"
        val sanitized = sanitizeLogMessage(rawLog)

        assertFalse(sanitized.contains("pk_test_123456789"))
        assertFalse(sanitized.contains("secret_key"))
        assertFalse(sanitized.contains("session=abc"))
        assertTrue(sanitized.contains("Authorization: [REDACTED]"))
        assertTrue(sanitized.contains("X-Api-Key: [REDACTED]"))
        assertTrue(sanitized.contains("Cookie: [REDACTED]"))
        assertTrue(sanitized.contains("Content-Type: application/json"))
    }
}

