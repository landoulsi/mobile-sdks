package com.landoulsi.payment.shared.network.dto

import com.landoulsi.payment.shared.network.PaymentJson
import kotlinx.serialization.encodeToString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for all gateway DTO serialization and deserialization using [PaymentJson].
 */
class GatewayDtoSerializationTest {

    // ─────────────────────────────────────────────────────────
    //  GooglePayTokenizationData
    // ─────────────────────────────────────────────────────────

    @Test
    fun testGooglePayTokenizationData_deserializesCorrectly() {
        val json = """
            {
              "type": "PAYMENT_GATEWAY",
              "token": "{\"id\":\"tok_stripe_123\",\"object\":\"token\"}"
            }
        """.trimIndent()

        val dto = PaymentJson.decodeFromString<GooglePayTokenizationData>(json)

        assertEquals("PAYMENT_GATEWAY", dto.type)
        assertEquals("{\"id\":\"tok_stripe_123\",\"object\":\"token\"}", dto.token)
    }

    @Test
    fun testGooglePayTokenizationData_serializesCorrectly() {
        val dto = GooglePayTokenizationData(
            type = "DIRECT",
            token = "encrypted_payload"
        )

        val json = PaymentJson.encodeToString(dto)

        assertTrue(json.contains("\"type\":\"DIRECT\""))
        assertTrue(json.contains("\"token\":\"encrypted_payload\""))
    }

    @Test
    fun testGooglePayTokenizationData_ignoresUnknownKeys() {
        val json = """
            {
              "type": "PAYMENT_GATEWAY",
              "token": "some_token",
              "unknown_field": "should_be_ignored",
              "extra": 42
            }
        """.trimIndent()

        // Should not throw
        val dto = PaymentJson.decodeFromString<GooglePayTokenizationData>(json)
        assertEquals("PAYMENT_GATEWAY", dto.type)
    }

    // ─────────────────────────────────────────────────────────
    //  GooglePayGatewayToken
    // ─────────────────────────────────────────────────────────

    @Test
    fun testGooglePayGatewayToken_deserializesFullObject() {
        val json = """
            {
              "id": "tok_visa_4242",
              "object": "token"
            }
        """.trimIndent()

        val token = PaymentJson.decodeFromString<GooglePayGatewayToken>(json)

        assertEquals("tok_visa_4242", token.id)
        assertEquals("token", token.`object`)
    }

    @Test
    fun testGooglePayGatewayToken_deserializesWithNullObject() {
        val json = """
            {
              "id": "pm_adyen_abc123"
            }
        """.trimIndent()

        val token = PaymentJson.decodeFromString<GooglePayGatewayToken>(json)

        assertEquals("pm_adyen_abc123", token.id)
        assertNull(token.`object`)
    }

    @Test
    fun testGooglePayGatewayToken_serializesCorrectly() {
        val token = GooglePayGatewayToken(id = "tok_test_999", `object` = "token")
        val json = PaymentJson.encodeToString(token)

        assertTrue(json.contains("\"id\":\"tok_test_999\""))
        assertTrue(json.contains("\"object\":\"token\""))
    }

    // ─────────────────────────────────────────────────────────
    //  GooglePayDirectToken
    // ─────────────────────────────────────────────────────────

    @Test
    fun testGooglePayDirectToken_deserializesECv2Payload() {
        val json = """
            {
              "protocolVersion": "ECv2",
              "signature": "base64sig==",
              "signedMessage": "base64msg==",
              "intermediateSigningKey": {
                "signedKey": "base64key==",
                "signatures": ["sig1", "sig2"]
              }
            }
        """.trimIndent()

        val token = PaymentJson.decodeFromString<GooglePayDirectToken>(json)

        assertEquals("ECv2", token.protocolVersion)
        assertEquals("base64sig==", token.signature)
        assertEquals("base64msg==", token.signedMessage)
        assertNotNull(token.intermediateSigningKey)
        assertEquals("base64key==", token.intermediateSigningKey!!.signedKey)
        assertEquals(listOf("sig1", "sig2"), token.intermediateSigningKey.signatures)
    }

    @Test
    fun testGooglePayDirectToken_deserializesWithoutIntermediateKey() {
        val json = """
            {
              "protocolVersion": "ECv1",
              "signature": "sigval",
              "signedMessage": "msgval"
            }
        """.trimIndent()

        val token = PaymentJson.decodeFromString<GooglePayDirectToken>(json)

        assertEquals("ECv1", token.protocolVersion)
        assertNull(token.intermediateSigningKey)
    }

    // ─────────────────────────────────────────────────────────
    //  GooglePayCardInfo
    // ─────────────────────────────────────────────────────────

    @Test
    fun testGooglePayCardInfo_deserializesWithBillingAddress() {
        val json = """
            {
              "cardNetwork": "VISA",
              "cardDetails": "1234",
              "billingAddress": {
                "name": "John Doe",
                "address1": "123 Main St",
                "locality": "San Francisco",
                "administrativeArea": "CA",
                "countryCode": "US",
                "postalCode": "94101"
              }
            }
        """.trimIndent()

        val info = PaymentJson.decodeFromString<GooglePayCardInfo>(json)

        assertEquals("VISA", info.cardNetwork)
        assertEquals("1234", info.cardDetails)
        assertNotNull(info.billingAddress)
        assertEquals("John Doe", info.billingAddress!!.name)
        assertEquals("123 Main St", info.billingAddress.address1)
        assertEquals("San Francisco", info.billingAddress.locality)
        assertEquals("CA", info.billingAddress.administrativeArea)
        assertEquals("US", info.billingAddress.countryCode)
        assertEquals("94101", info.billingAddress.postalCode)
    }

    @Test
    fun testGooglePayCardInfo_deserializesMinimal() {
        val json = "{}"

        val info = PaymentJson.decodeFromString<GooglePayCardInfo>(json)

        assertNull(info.cardNetwork)
        assertNull(info.cardDetails)
        assertNull(info.billingAddress)
    }

    // ─────────────────────────────────────────────────────────
    //  CardTokenRequest
    // ─────────────────────────────────────────────────────────

    @Test
    fun testCardTokenRequest_serializesWithRequiredFields() {
        val request = CardTokenRequest(
            number = "4242424242424242",
            expiryMonth = 12,
            expiryYear = 2028,
            cvc = "123"
        )

        val json = PaymentJson.encodeToString(request)

        assertTrue(json.contains("\"number\":\"4242424242424242\""))
        assertTrue(json.contains("\"exp_month\":12"))
        assertTrue(json.contains("\"exp_year\":2028"))
        assertTrue(json.contains("\"cvc\":\"123\""))
    }

    @Test
    fun testCardTokenRequest_serializesWithCardholderName() {
        val request = CardTokenRequest(
            number = "5500005555555559",
            expiryMonth = 8,
            expiryYear = 2029,
            cvc = "456",
            cardholderName = "Jane Smith"
        )

        val json = PaymentJson.encodeToString(request)

        assertTrue(json.contains("\"name\":\"Jane Smith\""))
    }

    @Test
    fun testCardTokenRequest_deserializesFromGatewayEcho() {
        // Some gateways echo back the request; ensure round-trip works
        val json = """
            {
              "number": "4111111111111111",
              "exp_month": 6,
              "exp_year": 2027,
              "cvc": "789",
              "name": "Alice Wonder"
            }
        """.trimIndent()

        val request = PaymentJson.decodeFromString<CardTokenRequest>(json)

        assertEquals("4111111111111111", request.number)
        assertEquals(6, request.expiryMonth)
        assertEquals(2027, request.expiryYear)
        assertEquals("789", request.cvc)
        assertEquals("Alice Wonder", request.cardholderName)
    }

    // ─────────────────────────────────────────────────────────
    //  CardTokenResponse
    // ─────────────────────────────────────────────────────────

    @Test
    fun testCardTokenResponse_deserializesStripeTokenResponse() {
        val json = """
            {
              "id": "tok_1OzKlm2eZvKYlo2C9bQ6xwzW",
              "object": "token",
              "created": 1709123456,
              "livemode": false,
              "type": "card",
              "card": {
                "id": "card_1OzKlm2eZvKYlo2C",
                "brand": "visa",
                "last4": "4242",
                "exp_month": 12,
                "exp_year": 2028,
                "funding": "credit",
                "country": "US"
              }
            }
        """.trimIndent()

        val response = PaymentJson.decodeFromString<CardTokenResponse>(json)

        assertEquals("tok_1OzKlm2eZvKYlo2C9bQ6xwzW", response.id)
        assertEquals("token", response.`object`)
        assertEquals(1709123456L, response.created)
        assertEquals(false, response.livemode)
        assertEquals("card", response.type)
        assertNotNull(response.card)
        assertEquals("visa", response.card!!.brand)
        assertEquals("4242", response.card.last4)
        assertEquals(12, response.card.expMonth)
        assertEquals(2028, response.card.expYear)
        assertEquals("credit", response.card.funding)
        assertEquals("US", response.card.country)
    }

    @Test
    fun testCardTokenResponse_deserializesWithMissingOptionalFields() {
        val json = """
            {
              "id": "tok_minimal_test"
            }
        """.trimIndent()

        val response = PaymentJson.decodeFromString<CardTokenResponse>(json)

        assertEquals("tok_minimal_test", response.id)
        assertNull(response.`object`)
        assertNull(response.created)
        assertEquals(false, response.livemode)
        assertNull(response.card)
    }

    @Test
    fun testCardTokenResponse_ignoresUnknownGatewayFields() {
        val json = """
            {
              "id": "tok_future_api",
              "object": "token",
              "new_field_v5": "future_value",
              "metadata": {"key": "value"},
              "card": {
                "id": "card_test",
                "brand": "mastercard",
                "last4": "5555",
                "new_card_field": "ignored"
              }
            }
        """.trimIndent()

        // Should not throw due to ignoreUnknownKeys
        val response = PaymentJson.decodeFromString<CardTokenResponse>(json)
        assertEquals("tok_future_api", response.id)
        assertEquals("mastercard", response.card?.brand)
        assertEquals("5555", response.card?.last4)
    }

    // ─────────────────────────────────────────────────────────
    //  GatewayErrorResponse
    // ─────────────────────────────────────────────────────────

    @Test
    fun testGatewayErrorResponse_deserializesCardDeclinedError() {
        val json = """
            {
              "error": {
                "type": "card_error",
                "code": "card_declined",
                "decline_code": "insufficient_funds",
                "message": "Your card has insufficient funds.",
                "param": null
              }
            }
        """.trimIndent()

        val errorResponse = PaymentJson.decodeFromString<GatewayErrorResponse>(json)

        assertNotNull(errorResponse.error)
        assertEquals("card_error", errorResponse.error!!.type)
        assertEquals("card_declined", errorResponse.error.code)
        assertEquals("insufficient_funds", errorResponse.error.declineCode)
        assertEquals("Your card has insufficient funds.", errorResponse.error.message)
    }

    @Test
    fun testGatewayErrorResponse_deserializesInvalidRequestError() {
        val json = """
            {
              "error": {
                "type": "invalid_request_error",
                "code": "parameter_missing",
                "message": "Missing required param: source.",
                "param": "source"
              }
            }
        """.trimIndent()

        val errorResponse = PaymentJson.decodeFromString<GatewayErrorResponse>(json)

        assertEquals("invalid_request_error", errorResponse.error?.type)
        assertEquals("parameter_missing", errorResponse.error?.code)
        assertEquals("source", errorResponse.error?.param)
        assertNull(errorResponse.error?.declineCode)
    }

    @Test
    fun testGatewayErrorResponse_deserializesEmptyError() {
        val json = """{"error": {}}"""
        val errorResponse = PaymentJson.decodeFromString<GatewayErrorResponse>(json)

        assertNotNull(errorResponse.error)
        assertNull(errorResponse.error!!.type)
        assertNull(errorResponse.error.code)
        assertNull(errorResponse.error.message)
    }
}
