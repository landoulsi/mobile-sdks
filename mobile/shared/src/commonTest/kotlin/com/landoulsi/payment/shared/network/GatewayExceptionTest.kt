package com.landoulsi.payment.shared.network

import com.landoulsi.payment.shared.model.PaymentErrorCode
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GatewayExceptionTest {

    // ─────────────────────────────────────────────────────────
    //  mapStatusToErrorCode
    // ─────────────────────────────────────────────────────────

    @Test
    fun testCardDeclinedGatewayCodeMapsToCardDeclined() {
        val errorCode = mapStatusToErrorCode(402, "card_declined")
        assertEquals(PaymentErrorCode.CARD_DECLINED, errorCode)
    }

    @Test
    fun testExpiredCardGatewayCodeMapsToExpiredCard() {
        val errorCode = mapStatusToErrorCode(402, "expired_card")
        assertEquals(PaymentErrorCode.EXPIRED_CARD, errorCode)
    }

    @Test
    fun testInsufficientFundsGatewayCodeMapsToInsufficientFunds() {
        val errorCode = mapStatusToErrorCode(402, "insufficient_funds")
        assertEquals(PaymentErrorCode.INSUFFICIENT_FUNDS, errorCode)
    }

    @Test
    fun testAuthenticationRequiredGatewayCodeMapsToAuthenticationFailed() {
        val errorCode = mapStatusToErrorCode(402, "authentication_required")
        assertEquals(PaymentErrorCode.AUTHENTICATION_FAILED, errorCode)
    }

    @Test
    fun testUnknownGatewayCodeFallsThroughToStatusCode() {
        val errorCode = mapStatusToErrorCode(402, "unknown_error_code")
        assertEquals(PaymentErrorCode.GATEWAY_ERROR, errorCode)
    }

    @Test
    fun testNullGatewayCodeFallsThroughToStatusCode() {
        val errorCode = mapStatusToErrorCode(402, null)
        assertEquals(PaymentErrorCode.GATEWAY_ERROR, errorCode)
    }

    @Test
    fun testUnauthorizedMapsToConfigurationError() {
        assertEquals(PaymentErrorCode.CONFIGURATION_ERROR, mapStatusToErrorCode(401))
        assertEquals(PaymentErrorCode.CONFIGURATION_ERROR, mapStatusToErrorCode(403))
    }

    @Test
    fun testUnprocessableEntityMapsToGatewayError() {
        assertEquals(PaymentErrorCode.GATEWAY_ERROR, mapStatusToErrorCode(422))
    }

    @Test
    fun testPaymentRequiredMapsToGatewayError() {
        assertEquals(PaymentErrorCode.GATEWAY_ERROR, mapStatusToErrorCode(402))
    }

    @Test
    fun testServerErrorMapsToGatewayError() {
        assertEquals(PaymentErrorCode.GATEWAY_ERROR, mapStatusToErrorCode(500))
        assertEquals(PaymentErrorCode.GATEWAY_ERROR, mapStatusToErrorCode(502))
        assertEquals(PaymentErrorCode.GATEWAY_ERROR, mapStatusToErrorCode(503))
        assertEquals(PaymentErrorCode.GATEWAY_ERROR, mapStatusToErrorCode(599))
    }

    @Test
    fun testClientErrorWithNoGatewayCodeMapsToUnknown() {
        assertEquals(PaymentErrorCode.UNKNOWN, mapStatusToErrorCode(400))
        assertEquals(PaymentErrorCode.UNKNOWN, mapStatusToErrorCode(404))
        assertEquals(PaymentErrorCode.UNKNOWN, mapStatusToErrorCode(418))
    }

    @Test
    fun testGatewayCodeTakesPrecedenceOverStatusCode() {
        val errorCode = mapStatusToErrorCode(401, "card_declined")
        assertEquals(PaymentErrorCode.CARD_DECLINED, errorCode)
    }

    @Test
    fun testGatewayCodeTakesPrecedenceOverServerErrorStatus() {
        val errorCode = mapStatusToErrorCode(503, "insufficient_funds")
        assertEquals(PaymentErrorCode.INSUFFICIENT_FUNDS, errorCode)
    }

    // ─────────────────────────────────────────────────────────
    //  throwIfError
    // ─────────────────────────────────────────────────────────

    @Test
    fun testThrowIfErrorDoesNotThrowForSuccessResponse() = runTest {
        val client = HttpClient(MockEngine {
            respond(
                content = """{"id":"tok_123","object":"token"}""",
                status = HttpStatusCode.OK,
                headers = headersOf("Content-Type", "application/json")
            )
        })

        val response = client.get("https://api.example.com/test")
        response.throwIfError()
    }

    @Test
    fun testThrowIfErrorThrowsFor4xxResponse() = runTest {
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

        val client = HttpClient(MockEngine {
            respond(
                content = errorBody,
                status = HttpStatusCode.PaymentRequired,
                headers = headersOf("Content-Type", "application/json")
            )
        })

        val response = client.get("https://api.example.com/test")
        val ex = assertFailsWith<GatewayException> {
            response.throwIfError()
        }

        assertEquals(402, ex.statusCode)
        assertEquals(PaymentErrorCode.CARD_DECLINED, ex.errorCode)
        assertEquals("Your card was declined.", ex.message)
        assertEquals("card_declined", ex.gatewayCode)
        assertEquals("insufficient_funds", ex.declineCode)
    }

    @Test
    fun testThrowIfErrorThrowsFor5xxResponse() = runTest {
        val errorBody = """{"error": {"message": "Internal server error"}}"""

        val client = HttpClient(MockEngine {
            respond(
                content = errorBody,
                status = HttpStatusCode.InternalServerError,
                headers = headersOf("Content-Type", "application/json")
            )
        })

        val response = client.get("https://api.example.com/test")
        val ex = assertFailsWith<GatewayException> {
            response.throwIfError()
        }

        assertEquals(500, ex.statusCode)
        assertEquals(PaymentErrorCode.GATEWAY_ERROR, ex.errorCode)
        assertEquals("Internal server error", ex.message)
    }

    @Test
    fun testThrowIfErrorHandlesEmptyBodyGracefully() = runTest {
        val client = HttpClient(MockEngine {
            respond(
                content = "",
                status = HttpStatusCode.InternalServerError,
                headers = headersOf("Content-Type", "application/json")
            )
        })

        val response = client.get("https://api.example.com/test")
        val ex = assertFailsWith<GatewayException> {
            response.throwIfError()
        }

        assertEquals(500, ex.statusCode)
        assertEquals(PaymentErrorCode.GATEWAY_ERROR, ex.errorCode)
        assertTrue(ex.message.contains("500"))
    }

    @Test
    fun testThrowIfErrorHandlesMalformedJsonBody() = runTest {
        val client = HttpClient(MockEngine {
            respond(
                content = "not json at all",
                status = HttpStatusCode.BadRequest,
                headers = headersOf("Content-Type", "application/json")
            )
        })

        val response = client.get("https://api.example.com/test")
        val ex = assertFailsWith<GatewayException> {
            response.throwIfError()
        }

        assertEquals(400, ex.statusCode)
        assertEquals(PaymentErrorCode.UNKNOWN, ex.errorCode)
        assertTrue(ex.message.contains("not json at all"))
    }

    @Test
    fun testThrowIfErrorHandlesJsonWithoutErrorField() = runTest {
        val client = HttpClient(MockEngine {
            respond(
                content = """{"status": "error", "detail": "Something went wrong"}""",
                status = HttpStatusCode.UnprocessableEntity,
                headers = headersOf("Content-Type", "application/json")
            )
        })

        val response = client.get("https://api.example.com/test")
        val ex = assertFailsWith<GatewayException> {
            response.throwIfError()
        }

        assertEquals(422, ex.statusCode)
        assertEquals(PaymentErrorCode.GATEWAY_ERROR, ex.errorCode)
        assertTrue(ex.message.contains("422"))
    }

    @Test
    fun testThrowIfErrorExtractsDeclineCode() = runTest {
        val errorBody = """
            {
              "error": {
                "type": "card_error",
                "code": "card_declined",
                "decline_code": "lost_card",
                "message": "This card has been reported lost."
              }
            }
        """.trimIndent()

        val client = HttpClient(MockEngine {
            respond(
                content = errorBody,
                status = HttpStatusCode.PaymentRequired,
                headers = headersOf("Content-Type", "application/json")
            )
        })

        val response = client.get("https://api.example.com/test")
        val ex = assertFailsWith<GatewayException> {
            response.throwIfError()
        }

        assertEquals("card_declined", ex.gatewayCode)
        assertEquals("lost_card", ex.declineCode)
    }

    @Test
    fun testThrowIfErrorSetsNullDeclineCodeWhenAbsent() = runTest {
        val errorBody = """
            {
              "error": {
                "type": "invalid_request_error",
                "code": "parameter_missing",
                "message": "Missing param: source."
              }
            }
        """.trimIndent()

        val client = HttpClient(MockEngine {
            respond(
                content = errorBody,
                status = HttpStatusCode.BadRequest,
                headers = headersOf("Content-Type", "application/json")
            )
        })

        val response = client.get("https://api.example.com/test")
        val ex = assertFailsWith<GatewayException> {
            response.throwIfError()
        }

        assertEquals("parameter_missing", ex.gatewayCode)
        assertNull(ex.declineCode)
        assertEquals(PaymentErrorCode.UNKNOWN, ex.errorCode)
    }

    @Test
    fun testThrowIfErrorUnauthorizedMapsToConfigurationError() = runTest {
        val errorBody = """{"error": {"message": "Invalid API key provided."}}"""

        val client = HttpClient(MockEngine {
            respond(
                content = errorBody,
                status = HttpStatusCode.Unauthorized,
                headers = headersOf("Content-Type", "application/json")
            )
        })

        val response = client.get("https://api.example.com/test")
        val ex = assertFailsWith<GatewayException> {
            response.throwIfError()
        }

        assertEquals(PaymentErrorCode.CONFIGURATION_ERROR, ex.errorCode)
    }

    // ─────────────────────────────────────────────────────────
    //  GatewayException properties
    // ─────────────────────────────────────────────────────────

    @Test
    fun testGatewayExceptionProperties() {
        val cause = RuntimeException("root cause")
        val ex = GatewayException(
            statusCode = 429,
            message = "Rate limited",
            errorCode = PaymentErrorCode.GATEWAY_ERROR,
            gatewayCode = "rate_limit",
            declineCode = null,
            cause = cause
        )

        assertEquals(429, ex.statusCode)
        assertEquals("Rate limited", ex.message)
        assertEquals(PaymentErrorCode.GATEWAY_ERROR, ex.errorCode)
        assertEquals("rate_limit", ex.gatewayCode)
        assertNull(ex.declineCode)
        assertEquals(cause, ex.cause)
    }

    @Test
    fun testGatewayExceptionDefaultOptionalFields() {
        val ex = GatewayException(
            statusCode = 500,
            message = "Server error",
            errorCode = PaymentErrorCode.GATEWAY_ERROR
        )

        assertNull(ex.gatewayCode)
        assertNull(ex.declineCode)
        assertNull(ex.cause)
    }
}
