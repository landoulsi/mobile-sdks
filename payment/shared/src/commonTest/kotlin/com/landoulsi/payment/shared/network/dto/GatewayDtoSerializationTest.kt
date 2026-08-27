package com.landoulsi.payment.shared.network.dto

import com.landoulsi.payment.shared.network.PaymentJson
import kotlinx.serialization.encodeToString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse

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

    // ─────────────────────────────────────────────────────────
    //  GatewayErrorResponse round-trip
    // ─────────────────────────────────────────────────────────

    @Test
    fun testGatewayErrorResponse_roundTripSerialization() {
        val original = GatewayErrorResponse(
            error = com.landoulsi.payment.shared.network.dto.GatewayError(
                type = "card_error",
                code = "card_declined",
                message = "Card was declined",
                param = "source",
                declineCode = "generic_decline"
            )
        )

        val json = PaymentJson.encodeToString(original)
        val deserialized = PaymentJson.decodeFromString<GatewayErrorResponse>(json)

        assertEquals(original.error?.type, deserialized.error?.type)
        assertEquals(original.error?.code, deserialized.error?.code)
        assertEquals(original.error?.message, deserialized.error?.message)
        assertEquals(original.error?.param, deserialized.error?.param)
        assertEquals(original.error?.declineCode, deserialized.error?.declineCode)
    }

    @Test
    fun testGatewayErrorResponse_roundTripWithNullError() {
        val original = GatewayErrorResponse(error = null)
        val json = PaymentJson.encodeToString(original)
        val deserialized = PaymentJson.decodeFromString<GatewayErrorResponse>(json)

        assertNull(deserialized.error)
    }

    // ─────────────────────────────────────────────────────────
    //  CardDetails serialization
    // ─────────────────────────────────────────────────────────

    @Test
    fun testCardDetails_serializesAndDeserializes() {
        val details = com.landoulsi.payment.shared.network.dto.CardDetails(
            id = "card_abc",
            brand = "visa",
            last4 = "4242",
            expMonth = 6,
            expYear = 2030,
            funding = "credit",
            country = "US"
        )

        val json = PaymentJson.encodeToString(details)
        val deserialized = PaymentJson.decodeFromString<com.landoulsi.payment.shared.network.dto.CardDetails>(json)

        assertEquals("card_abc", deserialized.id)
        assertEquals("visa", deserialized.brand)
        assertEquals("4242", deserialized.last4)
        assertEquals(6, deserialized.expMonth)
        assertEquals(2030, deserialized.expYear)
        assertEquals("credit", deserialized.funding)
        assertEquals("US", deserialized.country)
    }

    @Test
    fun testCardDetails_minimalDeserialization() {
        val json = "{}"
        val details = PaymentJson.decodeFromString<com.landoulsi.payment.shared.network.dto.CardDetails>(json)

        assertNull(details.id)
        assertNull(details.brand)
        assertNull(details.last4)
        assertNull(details.expMonth)
        assertNull(details.expYear)
        assertNull(details.funding)
        assertNull(details.country)
    }

    @Test
    fun testCardDetails_ignoresUnknownKeys() {
        val json = """
            {
              "id": "card_test",
              "brand": "mastercard",
              "last4": "5555",
              "future_field": "ignored",
              "metadata": {}
            }
        """.trimIndent()

        val details = PaymentJson.decodeFromString<com.landoulsi.payment.shared.network.dto.CardDetails>(json)
        assertEquals("card_test", details.id)
        assertEquals("mastercard", details.brand)
        assertEquals("5555", details.last4)
    }

    // ─────────────────────────────────────────────────────────
    //  GooglePayIntermediateSigningKey serialization
    // ─────────────────────────────────────────────────────────

    @Test
    fun testGooglePayIntermediateSigningKey_roundTrip() {
        val key = com.landoulsi.payment.shared.network.dto.GooglePayIntermediateSigningKey(
            signedKey = "base64_signed_key==",
            signatures = listOf("sig_a", "sig_b", "sig_c")
        )

        val json = PaymentJson.encodeToString(key)
        val deserialized = PaymentJson.decodeFromString<com.landoulsi.payment.shared.network.dto.GooglePayIntermediateSigningKey>(json)

        assertEquals("base64_signed_key==", deserialized.signedKey)
        assertEquals(3, deserialized.signatures.size)
        assertEquals("sig_a", deserialized.signatures[0])
        assertEquals("sig_c", deserialized.signatures[2])
    }

    @Test
    fun testGooglePayIntermediateSigningKey_emptySignatures() {
        val json = """{"signedKey": "key123", "signatures": []}"""
        val key = PaymentJson.decodeFromString<com.landoulsi.payment.shared.network.dto.GooglePayIntermediateSigningKey>(json)

        assertEquals("key123", key.signedKey)
        assertTrue(key.signatures.isEmpty())
    }

    // ─────────────────────────────────────────────────────────
    //  GooglePayBillingAddress serialization
    // ─────────────────────────────────────────────────────────

    @Test
    fun testGooglePayBillingAddress_fullSerialization() {
        val address = com.landoulsi.payment.shared.network.dto.GooglePayBillingAddress(
            name = "John Doe",
            address1 = "123 Main St",
            address2 = "Apt 4B",
            address3 = "Suite 200",
            locality = "San Francisco",
            administrativeArea = "CA",
            countryCode = "US",
            postalCode = "94101",
            sortingCode = "12345",
            phoneNumber = "+1-555-0100"
        )

        val json = PaymentJson.encodeToString(address)
        val deserialized = PaymentJson.decodeFromString<com.landoulsi.payment.shared.network.dto.GooglePayBillingAddress>(json)

        assertEquals("John Doe", deserialized.name)
        assertEquals("123 Main St", deserialized.address1)
        assertEquals("Apt 4B", deserialized.address2)
        assertEquals("Suite 200", deserialized.address3)
        assertEquals("San Francisco", deserialized.locality)
        assertEquals("CA", deserialized.administrativeArea)
        assertEquals("US", deserialized.countryCode)
        assertEquals("94101", deserialized.postalCode)
        assertEquals("12345", deserialized.sortingCode)
        assertEquals("+1-555-0100", deserialized.phoneNumber)
    }

    @Test
    fun testGooglePayBillingAddress_partialFields() {
        val json = """
            {
              "name": "Jane",
              "countryCode": "GB",
              "postalCode": "SW1A 1AA"
            }
        """.trimIndent()

        val address = PaymentJson.decodeFromString<com.landoulsi.payment.shared.network.dto.GooglePayBillingAddress>(json)
        assertEquals("Jane", address.name)
        assertEquals("GB", address.countryCode)
        assertEquals("SW1A 1AA", address.postalCode)
        assertNull(address.address1)
        assertNull(address.locality)
    }

    // ─────────────────────────────────────────────────────────
    //  CardTokenResponse with full CardDetails round-trip
    // ─────────────────────────────────────────────────────────

    @Test
    fun testCardTokenResponse_roundTripWithFullCardDetails() {
        val original = CardTokenResponse(
            id = "tok_rt_123",
            `object` = "token",
            created = 1700000000L,
            livemode = true,
            type = "card",
            card = com.landoulsi.payment.shared.network.dto.CardDetails(
                id = "card_rt_1",
                brand = "amex",
                last4 = "0005",
                expMonth = 3,
                expYear = 2031,
                funding = "credit",
                country = "US"
            )
        )

        val json = PaymentJson.encodeToString(original)
        val deserialized = PaymentJson.decodeFromString<CardTokenResponse>(json)

        assertEquals("tok_rt_123", deserialized.id)
        assertEquals("token", deserialized.`object`)
        assertEquals(1700000000L, deserialized.created)
        assertEquals(true, deserialized.livemode)
        assertEquals("card", deserialized.type)
        assertNotNull(deserialized.card)
        assertEquals("amex", deserialized.card!!.brand)
        assertEquals("0005", deserialized.card.last4)
        assertEquals(3, deserialized.card.expMonth)
        assertEquals(2031, deserialized.card.expYear)
        assertEquals("credit", deserialized.card.funding)
        assertEquals("US", deserialized.card.country)
    }

    // ─────────────────────────────────────────────────────────
    //  GooglePayGatewayToken round-trip
    // ─────────────────────────────────────────────────────────

    @Test
    fun testGooglePayGatewayToken_roundTrip() {
        val original = GooglePayGatewayToken(id = "tok_round_trip", `object` = "token")
        val json = PaymentJson.encodeToString(original)
        val deserialized = PaymentJson.decodeFromString<GooglePayGatewayToken>(json)

        assertEquals(original.id, deserialized.id)
        assertEquals(original.`object`, deserialized.`object`)
    }

    // ─────────────────────────────────────────────────────────
    //  GooglePayDirectToken round-trip
    // ─────────────────────────────────────────────────────────

    @Test
    fun testGooglePayDirectToken_roundTrip() {
        val original = GooglePayDirectToken(
            protocolVersion = "ECv2",
            signature = "sig_base64",
            signedMessage = "msg_base64",
            intermediateSigningKey = com.landoulsi.payment.shared.network.dto.GooglePayIntermediateSigningKey(
                signedKey = "key_base64",
                signatures = listOf("ecdsa_sig")
            )
        )

        val json = PaymentJson.encodeToString(original)
        val deserialized = PaymentJson.decodeFromString<GooglePayDirectToken>(json)

        assertEquals("ECv2", deserialized.protocolVersion)
        assertEquals("sig_base64", deserialized.signature)
        assertEquals("msg_base64", deserialized.signedMessage)
        assertNotNull(deserialized.intermediateSigningKey)
        assertEquals("key_base64", deserialized.intermediateSigningKey!!.signedKey)
        assertEquals(listOf("ecdsa_sig"), deserialized.intermediateSigningKey!!.signatures)
    }

    // ─────────────────────────────────────────────────────────
    //  GooglePayCardInfo round-trip
    // ─────────────────────────────────────────────────────────

    @Test
    fun testGooglePayCardInfo_roundTrip() {
        val original = GooglePayCardInfo(
            cardNetwork = "VISA",
            cardDetails = "4242",
            billingAddress = com.landoulsi.payment.shared.network.dto.GooglePayBillingAddress(
                name = "RT Test",
                countryCode = "DE",
                postalCode = "10115"
            )
        )

        val json = PaymentJson.encodeToString(original)
        val deserialized = PaymentJson.decodeFromString<GooglePayCardInfo>(json)

        assertEquals("VISA", deserialized.cardNetwork)
        assertEquals("4242", deserialized.cardDetails)
        assertNotNull(deserialized.billingAddress)
        assertEquals("RT Test", deserialized.billingAddress!!.name)
        assertEquals("DE", deserialized.billingAddress.countryCode)
    }

    // ─────────────────────────────────────────────────────────
    //  CardTokenRequest round-trip
    // ─────────────────────────────────────────────────────────

    @Test
    fun testCardTokenRequest_roundTrip() {
        val original = CardTokenRequest(
            number = "5105105105105100",
            expiryMonth = 11,
            expiryYear = 2032,
            cvc = "321",
            cardholderName = "Round Trip"
        )

        val json = PaymentJson.encodeToString(original)
        val deserialized = PaymentJson.decodeFromString<CardTokenRequest>(json)

        assertEquals("5105105105105100", deserialized.number)
        assertEquals(11, deserialized.expiryMonth)
        assertEquals(2032, deserialized.expiryYear)
        assertEquals("321", deserialized.cvc)
        assertEquals("Round Trip", deserialized.cardholderName)
    }

    // ─────────────────────────────────────────────────────────
    //  PaymentIntent & 3D Secure DTO serialization
    // ─────────────────────────────────────────────────────────

    @Test
    fun testPaymentIntentConfirmRequest_roundTrip() {
        val request = PaymentIntentConfirmRequest(
            paymentMethodId = "pm_card_test_123",
            clientSecret = "pi_sec_123",
            returnUrl = "paymentsdk://3ds-complete"
        )

        val json = PaymentJson.encodeToString(request)
        val deserialized = PaymentJson.decodeFromString<PaymentIntentConfirmRequest>(json)

        assertEquals("pm_card_test_123", deserialized.paymentMethodId)
        assertEquals("pi_sec_123", deserialized.clientSecret)
        assertEquals("paymentsdk://3ds-complete", deserialized.returnUrl)
    }

    @Test
    fun testPaymentIntentConfirmResponse_deserializesRequiresAction() {
        val json = """
            {
              "id": "pi_123_action",
              "object": "payment_intent",
              "status": "requires_action",
              "client_secret": "secret_abc",
              "amount": 5000,
              "currency": "eur",
              "next_action": {
                "type": "redirect_to_url",
                "redirect_to_url": {
                  "url": "https://hooks.stripe.com/redirect/3ds",
                  "return_url": "paymentsdk://3ds-complete"
                },
                "use_stripe_sdk": {
                  "acs_url": "https://acs.bank.com",
                  "creq": "creq_val_123",
                  "three_d_s_server_trans_id": "trans_3ds_001"
                }
              }
            }
        """.trimIndent()

        val response = PaymentJson.decodeFromString<PaymentIntentConfirmResponse>(json)

        assertEquals("pi_123_action", response.id)
        assertEquals("requires_action", response.status)
        assertEquals("secret_abc", response.clientSecret)
        assertEquals(5000L, response.amount)
        assertEquals("eur", response.currency)
        assertNotNull(response.nextAction)
        assertEquals("redirect_to_url", response.nextAction!!.type)
        assertEquals("https://hooks.stripe.com/redirect/3ds", response.nextAction.redirectToUrl?.url)
        assertEquals("paymentsdk://3ds-complete", response.nextAction.redirectToUrl?.returnUrl)
        assertEquals("https://acs.bank.com", response.nextAction.useStripeSdk?.acsUrl)
        assertEquals("creq_val_123", response.nextAction.useStripeSdk?.cReq)
        assertEquals("trans_3ds_001", response.nextAction.useStripeSdk?.threeDSServerTransId)
    }

    @Test
    fun testPaymentIntentConfirmResponse_deserializesErrorResponse() {
        val json = """
            {
              "id": "pi_declined_intent",
              "status": "requires_payment_method",
              "last_payment_error": {
                "type": "card_error",
                "code": "card_declined",
                "decline_code": "stolen_card",
                "message": "Card was declined as stolen"
              }
            }
        """.trimIndent()

        val response = PaymentJson.decodeFromString<PaymentIntentConfirmResponse>(json)

        assertEquals("pi_declined_intent", response.id)
        assertEquals("requires_payment_method", response.status)
        assertNotNull(response.lastPaymentError)
        assertEquals("card_error", response.lastPaymentError!!.type)
        assertEquals("card_declined", response.lastPaymentError.code)
        assertEquals("stolen_card", response.lastPaymentError.declineCode)
        assertEquals("Card was declined as stolen", response.lastPaymentError.message)
    }

    @Test
    fun testPaymentIntentConfirmResponse_roundTrip() {
        val original = PaymentIntentConfirmResponse(
            id = "pi_round_trip",
            `object` = "payment_intent",
            status = "succeeded",
            clientSecret = "sec_rt",
            paymentMethod = "pm_rt_card",
            amount = 1999L,
            currency = "usd"
        )

        val json = PaymentJson.encodeToString(original)
        val deserialized = PaymentJson.decodeFromString<PaymentIntentConfirmResponse>(json)

        assertEquals(original.id, deserialized.id)
        assertEquals(original.status, deserialized.status)
        assertEquals(original.clientSecret, deserialized.clientSecret)
        assertEquals(original.paymentMethod, deserialized.paymentMethod)
        assertEquals(original.amount, deserialized.amount)
        assertEquals(original.currency, deserialized.currency)
    }

    // ─────────────────────────────────────────────────────────
    //  Redaction / safe toString()
    // ─────────────────────────────────────────────────────────

    @Test
    fun testPaymentIntentConfirmRequestToStringRedactsClientSecret() {
        val request = PaymentIntentConfirmRequest(
            paymentMethodId = "pm_redact_123",
            clientSecret = "pi_redact_secret_xyz",
            returnUrl = "paymentsdk://3ds-complete"
        )

        val str = request.toString()

        assertTrue(str.contains("paymentMethodId=pm_redact_123"))
        assertTrue(str.contains("returnUrl=paymentsdk://3ds-complete"))
        assertFalse(str.contains("pi_redact_secret_xyz"))
        assertTrue(str.contains("clientSecret=[REDACTED]"))
    }

    @Test
    fun testThreeDSChallengeDataToStringRedactsCReq() {
        val data = ThreeDSChallengeData(
            acsUrl = "https://acs.example.com/challenge",
            cReq = "super_secret_creq_payload",
            threeDSServerTransId = "3ds_tx_123",
            stripeJs = "https://js.stripe.com/3ds"
        )

        val str = data.toString()

        assertTrue(str.contains("acsUrl=https://acs.example.com/challenge"))
        assertTrue(str.contains("threeDSServerTransId=3ds_tx_123"))
        assertTrue(str.contains("stripeJs=https://js.stripe.com/3ds"))
        assertFalse(str.contains("super_secret_creq_payload"))
        assertTrue(str.contains("cReq=[REDACTED]"))
    }
}

