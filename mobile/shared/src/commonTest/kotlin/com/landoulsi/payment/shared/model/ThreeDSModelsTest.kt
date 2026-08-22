package com.landoulsi.payment.shared.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for the 3D Secure model classes and the return-url parser introduced in
 * [com.landoulsi.payment.shared.model.ThreeDSModels].
 */
class ThreeDSModelsTest {

    // ─────────────────────────────────────────────────────────
    //  ThreeDSChallenge data class & defaults
    // ─────────────────────────────────────────────────────────

    @Test
    fun testThreeDSChallengeUsesDefaultReturnUrl() {
        val challenge = ThreeDSChallenge(
            paymentIntentId = "pi_default",
            clientSecret = "secret_default",
            redirectUrl = "https://bank.example.com/acs"
        )

        assertEquals("pi_default", challenge.paymentIntentId)
        assertEquals("secret_default", challenge.clientSecret)
        assertEquals("https://bank.example.com/acs", challenge.redirectUrl)
        assertEquals(ThreeDSChallenge.DEFAULT_RETURN_URL, challenge.returnUrl)
        assertEquals("paymentsdk://3ds-complete", challenge.returnUrl)
        assertNull(challenge.acsUrl)
        assertNull(challenge.cReq)
        assertNull(challenge.threeDSServerTransId)
    }

    @Test
    fun testThreeDSChallengeAcceptsCustomReturnUrlAnd3DS2Fields() {
        val challenge = ThreeDSChallenge(
            paymentIntentId = "pi_native",
            clientSecret = "secret_native",
            redirectUrl = "https://acs.example.com/challenge",
            returnUrl = "myapp://3ds/return",
            acsUrl = "https://acs.example.com/native",
            cReq = "base64_creq_payload",
            threeDSServerTransId = "3ds_server_tx_123"
        )

        assertEquals("myapp://3ds/return", challenge.returnUrl)
        assertEquals("https://acs.example.com/native", challenge.acsUrl)
        assertEquals("base64_creq_payload", challenge.cReq)
        assertEquals("3ds_server_tx_123", challenge.threeDSServerTransId)
    }

    @Test
    fun testThreeDSChallengeToStringRedactsSecrets() {
        val challenge = ThreeDSChallenge(
            paymentIntentId = "pi_redact",
            clientSecret = "super_secret_client_secret",
            redirectUrl = "https://bank.example.com/acs",
            returnUrl = "paymentsdk://3ds-complete",
            cReq = "super_secret_creq"
        )

        val str = challenge.toString()

        assertTrue(str.contains("paymentIntentId=pi_redact"))
        assertTrue(str.contains("redirectUrl=https://bank.example.com/acs"))
        assertTrue(str.contains("returnUrl=paymentsdk://3ds-complete"))
        assertFalse(str.contains("super_secret_client_secret"), "clientSecret must be redacted")
        assertFalse(str.contains("super_secret_creq"), "cReq must be redacted")
        assertTrue(str.contains("clientSecret=[REDACTED]"))
        assertTrue(str.contains("cReq=[REDACTED]"))
    }

    // ─────────────────────────────────────────────────────────
    //  ThreeDSResult sealed interface variants
    // ─────────────────────────────────────────────────────────

    @Test
    fun testThreeDSResultCompletedHoldsPayload() {
        val result = ThreeDSResult.Completed(returnPayload = "paymentsdk://done?status=succeeded")
        assertEquals("paymentsdk://done?status=succeeded", result.returnPayload)
    }

    @Test
    fun testThreeDSResultFailedHoldsErrorDetails() {
        val result = ThreeDSResult.Failed(
            errorCode = PaymentErrorCode.AUTHENTICATION_FAILED,
            message = "Issuer rejected"
        )
        assertEquals(PaymentErrorCode.AUTHENTICATION_FAILED, result.errorCode)
        assertEquals("Issuer rejected", result.message)
    }

    @Test
    fun testThreeDSResultCanceledIsSingleton() {
        assertEquals(ThreeDSResult.Canceled, ThreeDSResult.Canceled)
    }

    // ─────────────────────────────────────────────────────────
    //  parseThreeDSReturnUrl – blank / malformed inputs
    // ─────────────────────────────────────────────────────────

    @Test
    fun testParseThreeDSReturnUrlReturnsNullForBlankInputs() {
        val expected = "paymentsdk://3ds-complete"

        assertNull(parseThreeDSReturnUrl("", expected))
        assertNull(parseThreeDSReturnUrl("   ", expected))
        assertNull(parseThreeDSReturnUrl(expected, ""))
        assertNull(parseThreeDSReturnUrl(expected, "   "))
        assertNull(parseThreeDSReturnUrl("", ""))
    }

    @Test
    fun testParseThreeDSReturnUrlReturnsNullForSchemeMismatch() {
        val expected = "paymentsdk://3ds-complete"

        // Same host/path but different scheme
        assertNull(parseThreeDSReturnUrl("https://3ds-complete", expected))
        assertNull(parseThreeDSReturnUrl("myotherapp://3ds-complete", expected))
    }

    @Test
    fun testParseThreeDSReturnUrlReturnsNullForBasePathMismatch() {
        val expected = "paymentsdk://3ds-complete"

        assertNull(parseThreeDSReturnUrl("paymentsdk://3ds-complete/extra", expected))
        assertNull(parseThreeDSReturnUrl("paymentsdk://3ds-complete/", expected))
        assertNull(parseThreeDSReturnUrl("paymentsdk://other-path", expected))
    }

    // ─────────────────────────────────────────────────────────
    //  parseThreeDSReturnUrl – success indicators
    // ─────────────────────────────────────────────────────────

    @Test
    fun testParseThreeDSReturnUrlSucceedsForTransStatusYAndA() {
        val expected = "paymentsdk://3ds-complete"

        val yResult = parseThreeDSReturnUrl(
            "paymentsdk://3ds-complete?transStatus=Y&payment_intent=pi_123",
            expected
        )
        assertTrue(yResult is ThreeDSResult.Completed)
        assertEquals(
            "paymentsdk://3ds-complete?transStatus=Y&payment_intent=pi_123",
            (yResult as ThreeDSResult.Completed).returnPayload
        )

        val aResult = parseThreeDSReturnUrl("paymentsdk://3ds-complete?transStatus=A", expected)
        assertTrue(aResult is ThreeDSResult.Completed)
    }

    @Test
    fun testParseThreeDSReturnUrlSucceedsForStatusSucceededAndSuccess() {
        val expected = "paymentsdk://3ds-complete"

        assertTrue(
            parseThreeDSReturnUrl("paymentsdk://3ds-complete?status=succeeded", expected)
                is ThreeDSResult.Completed
        )
        assertTrue(
            parseThreeDSReturnUrl("paymentsdk://3ds-complete?status=success", expected)
                is ThreeDSResult.Completed
        )
    }

    @Test
    fun testParseThreeDSReturnUrlIsCaseInsensitiveForSchemeAndHost() {
        val expected = "paymentsdk://3ds-complete"

        val result = parseThreeDSReturnUrl(
            "PAYMENTSDK://3DS-COMPLETE?transStatus=Y",
            expected
        )
        assertTrue(result is ThreeDSResult.Completed)
    }

    @Test
    fun testParseThreeDSReturnUrlTrimsTrailingSlashesBeforeComparison() {
        val expected = "paymentsdk://3ds-complete/"

        val result = parseThreeDSReturnUrl(
            "paymentsdk://3ds-complete?status=succeeded",
            expected
        )
        assertTrue(result is ThreeDSResult.Completed)
    }

    // ─────────────────────────────────────────────────────────
    //  parseThreeDSReturnUrl – failure indicators
    // ─────────────────────────────────────────────────────────

    @Test
    fun testParseThreeDSReturnUrlFailsForTransStatusNAndRAndU() {
        val expected = "paymentsdk://3ds-complete"

        listOf("N", "R", "U").forEach { status ->
            val result = parseThreeDSReturnUrl(
                "paymentsdk://3ds-complete?transStatus=$status",
                expected
            )
            assertTrue(result is ThreeDSResult.Failed, "transStatus=$status should fail")
            assertEquals(
                PaymentErrorCode.AUTHENTICATION_FAILED,
                (result as ThreeDSResult.Failed).errorCode
            )
        }
    }

    @Test
    fun testParseThreeDSReturnUrlFailsForNegativeStatusValues() {
        val expected = "paymentsdk://3ds-complete"

        listOf("failed", "declined", "error", "failure", "requires_payment_method").forEach { status ->
            val result = parseThreeDSReturnUrl(
                "paymentsdk://3ds-complete?status=$status",
                expected
            )
            assertTrue(result is ThreeDSResult.Failed, "status=$status should fail")
        }
    }

    @Test
    fun testParseThreeDSReturnUrlFailsForErrorParameters() {
        val expected = "paymentsdk://3ds-complete"

        assertTrue(
            parseThreeDSReturnUrl("paymentsdk://3ds-complete?error=invalid", expected)
                is ThreeDSResult.Failed
        )
        assertTrue(
            parseThreeDSReturnUrl("paymentsdk://3ds-complete?error_description=bad", expected)
                is ThreeDSResult.Failed
        )
        assertTrue(
            parseThreeDSReturnUrl("paymentsdk://3ds-complete?error_code=E123", expected)
                is ThreeDSResult.Failed
        )
    }

    @Test
    fun testParseThreeDSReturnUrlFailsClosedOnIndeterminateStatus() {
        val expected = "paymentsdk://3ds-complete"

        // Bare return URL
        val bare = parseThreeDSReturnUrl("paymentsdk://3ds-complete", expected)
        assertTrue(bare is ThreeDSResult.Failed)

        // Challenge-required / decoupled / informational statuses
        listOf("C", "D", "I").forEach { status ->
            val result = parseThreeDSReturnUrl(
                "paymentsdk://3ds-complete?transStatus=$status",
                expected
            )
            assertTrue(result is ThreeDSResult.Failed, "transStatus=$status should fail closed")
        }

        // Unknown parameters
        val unknown = parseThreeDSReturnUrl(
            "paymentsdk://3ds-complete?foo=bar&baz=qux",
            expected
        )
        assertTrue(unknown is ThreeDSResult.Failed)
    }

    // ─────────────────────────────────────────────────────────
    //  parseThreeDSReturnUrl – cancellation indicators
    // ─────────────────────────────────────────────────────────

    @Test
    fun testParseThreeDSReturnUrlCanceledVariants() {
        val expected = "paymentsdk://3ds-complete"

        assertEquals(
            ThreeDSResult.Canceled,
            parseThreeDSReturnUrl("paymentsdk://3ds-complete?status=canceled", expected)
        )
        assertEquals(
            ThreeDSResult.Canceled,
            parseThreeDSReturnUrl("paymentsdk://3ds-complete?status=cancelled", expected)
        )
        assertEquals(
            ThreeDSResult.Canceled,
            parseThreeDSReturnUrl("paymentsdk://3ds-complete?canceled=true", expected)
        )
        assertEquals(
            ThreeDSResult.Canceled,
            parseThreeDSReturnUrl("paymentsdk://3ds-complete?cancelled=true", expected)
        )
    }

    @Test
    fun testParseThreeDSReturnUrlCanceledTakesPrecedenceOverFailureFlags() {
        val expected = "paymentsdk://3ds-complete"

        // Even if an error parameter is present, explicit cancellation wins.
        val result = parseThreeDSReturnUrl(
            "paymentsdk://3ds-complete?status=canceled&error=oops",
            expected
        )
        assertEquals(ThreeDSResult.Canceled, result)
    }

    // ─────────────────────────────────────────────────────────
    //  parseThreeDSReturnUrl – query decoding & fragment handling
    // ─────────────────────────────────────────────────────────

    @Test
    fun testParseThreeDSReturnUrlDecodesUrlEncodedQuery() {
        val expected = "paymentsdk://3ds-complete"

        val result = parseThreeDSReturnUrl(
            "paymentsdk://3ds-complete?status=succeeded&message=Hello%20World",
            expected
        )
        assertTrue(result is ThreeDSResult.Completed)
        assertTrue(
            (result as ThreeDSResult.Completed).returnPayload!!.contains("Hello%20World")
        )
    }

    @Test
    fun testParseThreeDSReturnUrlDecodesPlusAsSpace() {
        val expected = "paymentsdk://3ds-complete"

        // Even though we don't expose the decoded value, the parser must not crash.
        val result = parseThreeDSReturnUrl(
            "paymentsdk://3ds-complete?status=succeeded&note=Hello+World",
            expected
        )
        assertTrue(result is ThreeDSResult.Completed)
    }

    @Test
    fun testParseThreeDSReturnUrlIgnoresFragment() {
        val expected = "paymentsdk://3ds-complete"

        val result = parseThreeDSReturnUrl(
            "paymentsdk://3ds-complete?status=succeeded#section",
            expected
        )
        assertTrue(result is ThreeDSResult.Completed)
    }

    @Test
    fun testParseThreeDSReturnUrlHandlesParametersWithoutValue() {
        val expected = "paymentsdk://3ds-complete"

        // `error` key present with no value should still be treated as a failure.
        val result = parseThreeDSReturnUrl(
            "paymentsdk://3ds-complete?error&status=succeeded",
            expected
        )
        assertTrue(result is ThreeDSResult.Failed)
    }

    // ─────────────────────────────────────────────────────────
    //  parseThreeDSReturnUrl – HTTP(S) return URLs
    // ─────────────────────────────────────────────────────────

    @Test
    fun testParseThreeDSReturnUrlWorksWithHttpsReturnUrl() {
        val expected = "https://example.com/checkout/3ds-return"

        val success = parseThreeDSReturnUrl(
            "https://example.com/checkout/3ds-return?status=succeeded",
            expected
        )
        assertTrue(success is ThreeDSResult.Completed)

        val failure = parseThreeDSReturnUrl(
            "https://example.com/checkout/3ds-return?transStatus=N",
            expected
        )
        assertTrue(failure is ThreeDSResult.Failed)

        assertNull(
            parseThreeDSReturnUrl("https://attacker.com/checkout/3ds-return?status=succeeded", expected)
        )
    }

    @Test
    fun testParseThreeDSReturnUrlMatchesPathExactly() {
        val expected = "https://example.com/checkout/3ds-return"

        // Same host, different path -> no match
        assertNull(
            parseThreeDSReturnUrl("https://example.com/other/3ds-return?status=succeeded", expected)
        )

        // Subpath under the expected path -> no match
        assertNull(
            parseThreeDSReturnUrl("https://example.com/checkout/3ds-return/extra?status=succeeded", expected)
        )
    }

    // ─────────────────────────────────────────────────────────
    //  parseThreeDSReturnUrl – mixed / real-world payloads
    // ─────────────────────────────────────────────────────────

    @Test
    fun testParseThreeDSReturnUrlRealWorldStripeStylePayload() {
        val expected = "paymentsdk://3ds-complete"

        val url = "paymentsdk://3ds-complete?payment_intent=pi_3ds&" +
            "payment_intent_client_secret=pi_3ds_secret_xyz&status=succeeded"

        val result = parseThreeDSReturnUrl(url, expected)
        assertTrue(result is ThreeDSResult.Completed)
        assertNotNull((result as ThreeDSResult.Completed).returnPayload)
    }

    @Test
    fun testParseThreeDSReturnUrlRealWorldFailurePayload() {
        val expected = "paymentsdk://3ds-complete"

        val url = "paymentsdk://3ds-complete?payment_intent=pi_3ds&" +
            "payment_intent_client_secret=pi_3ds_secret_xyz&status=requires_payment_method"

        val result = parseThreeDSReturnUrl(url, expected)
        assertTrue(result is ThreeDSResult.Failed)
        assertEquals(
            PaymentErrorCode.AUTHENTICATION_FAILED,
            (result as ThreeDSResult.Failed).errorCode
        )
    }
}
