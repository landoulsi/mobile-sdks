package com.landoulsi.payment.threeds

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.landoulsi.payment.R
import com.landoulsi.payment.shared.model.Currency
import com.landoulsi.payment.shared.model.Money
import com.landoulsi.payment.shared.model.PaymentErrorCode
import com.landoulsi.payment.shared.model.PaymentRequest
import com.landoulsi.payment.shared.model.ThreeDSChallenge
import com.landoulsi.payment.shared.model.ThreeDSResult
import com.landoulsi.payment.ui.theme.PaymentsdkTheme
import com.landoulsi.payment.ui.threeds.ThreeDSChallengeCard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Compose UI tests for the 3D Secure challenge card and its user-visible controls.
 */
class ThreeDSChallengeComposeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val sampleRequest = PaymentRequest(
        id = "order_3ds_001",
        amount = Money.fromMajorUnits(99.99, Currency.USD),
        merchantName = "Test Merchant"
    )

    private val sampleChallenge = ThreeDSChallenge(
        paymentIntentId = "pi_3ds_001",
        clientSecret = "pi_3ds_001_secret",
        redirectUrl = "",
        returnUrl = "paymentsdk://3ds-complete"
    )

    @Test
    fun challengeCard_rendersHeaderAndPaymentSummary() {
        composeTestRule.setContent {
            PaymentsdkTheme {
                ThreeDSChallengeCard(
                    challenge = sampleChallenge,
                    request = sampleRequest,
                    onResult = {}
                )
            }
        }

        composeTestRule.onNodeWithText("3D Secure Verification").assertIsDisplayed()
        composeTestRule.onNodeWithText("Complete authentication with your issuing bank")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Verified by 3-D Secure").assertIsDisplayed()

        // Payment context summary
        composeTestRule.onNodeWithText("Merchant").assertIsDisplayed()
        composeTestRule.onNodeWithText("Test Merchant").assertIsDisplayed()
        composeTestRule.onNodeWithText("Total Amount").assertIsDisplayed()
        composeTestRule.onNodeWithText("$99.99").assertIsDisplayed()
        composeTestRule.onNodeWithText("Intent ID").assertIsDisplayed()
        composeTestRule.onNodeWithText("pi_3ds_001").assertIsDisplayed()

        composeTestRule.onNodeWithText(
            "Your bank requires additional authentication (SCA) to complete this transaction."
        ).assertIsDisplayed()

        // Action controls
        composeTestRule.onNodeWithText("Approve Challenge").assertIsDisplayed()
        composeTestRule.onNodeWithText("Fail Authentication").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cancel Challenge").assertIsDisplayed()
    }

    @Test
    fun challengeCard_usesDemoMerchantNameFallback() {
        val requestWithoutMerchant = sampleRequest.copy(merchantName = null)

        composeTestRule.setContent {
            PaymentsdkTheme {
                ThreeDSChallengeCard(
                    challenge = sampleChallenge,
                    request = requestWithoutMerchant,
                    onResult = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Demo Store").assertIsDisplayed()
    }

    @Test
    fun challengeCard_approveButtonEmitsCompletedResult() {
        val results = mutableListOf<ThreeDSResult>()

        composeTestRule.setContent {
            PaymentsdkTheme {
                ThreeDSChallengeCard(
                    challenge = sampleChallenge,
                    request = sampleRequest,
                    onResult = { results.add(it) }
                )
            }
        }

        composeTestRule.onNodeWithText("Approve Challenge").performClick()

        assertEquals(1, results.size)
        val result = results.first()
        assertTrue(result is ThreeDSResult.Completed)
        assertEquals(
            "paymentsdk://3ds-complete?payment_intent=pi_3ds_001&status=succeeded&transStatus=Y",
            (result as ThreeDSResult.Completed).returnPayload
        )
    }

    @Test
    fun challengeCard_approveButtonDisablesAllControlsAfterResult() {
        composeTestRule.setContent {
            PaymentsdkTheme {
                ThreeDSChallengeCard(
                    challenge = sampleChallenge,
                    request = sampleRequest,
                    onResult = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Approve Challenge").performClick()

        composeTestRule.onNodeWithText("Approve Challenge").assertIsNotEnabled()
        composeTestRule.onNodeWithText("Fail Authentication").assertIsNotEnabled()
        composeTestRule.onNodeWithText("Cancel Challenge").assertIsNotEnabled()
    }

    @Test
    fun challengeCard_declineButtonEmitsFailedResult() {
        val results = mutableListOf<ThreeDSResult>()

        composeTestRule.setContent {
            PaymentsdkTheme {
                ThreeDSChallengeCard(
                    challenge = sampleChallenge,
                    request = sampleRequest,
                    onResult = { results.add(it) }
                )
            }
        }

        composeTestRule.onNodeWithText("Fail Authentication").performClick()

        assertEquals(1, results.size)
        val result = results.first()
        assertTrue(result is ThreeDSResult.Failed)
        assertEquals(PaymentErrorCode.AUTHENTICATION_FAILED, (result as ThreeDSResult.Failed).errorCode)
        assertEquals("3D Secure authentication declined by issuer", result.message)
    }

    @Test
    fun challengeCard_cancelButtonEmitsCanceledResult() {
        val results = mutableListOf<ThreeDSResult>()

        composeTestRule.setContent {
            PaymentsdkTheme {
                ThreeDSChallengeCard(
                    challenge = sampleChallenge,
                    request = sampleRequest,
                    onResult = { results.add(it) }
                )
            }
        }

        composeTestRule.onNodeWithText("Cancel Challenge").performClick()

        assertEquals(1, results.size)
        assertEquals(ThreeDSResult.Canceled, results.first())
    }

    @Test
    fun challengeCard_resultIsOnlyDeliveredOnce() {
        val results = mutableListOf<ThreeDSResult>()

        composeTestRule.setContent {
            PaymentsdkTheme {
                ThreeDSChallengeCard(
                    challenge = sampleChallenge,
                    request = sampleRequest,
                    onResult = { results.add(it) }
                )
            }
        }

        // First tap delivers a result and disables the controls.
        composeTestRule.onNodeWithText("Approve Challenge").performClick()
        assertEquals(1, results.size)

        // The remaining buttons are now disabled, so a second tap cannot deliver another result.
        composeTestRule.onNodeWithText("Fail Authentication").assertIsNotEnabled()
        composeTestRule.onNodeWithText("Cancel Challenge").assertIsNotEnabled()
        assertEquals(1, results.size)
    }

    @Test
    fun challengeCard_unsupportedUrlShowsErrorMessage() {
        val challenge = sampleChallenge.copy(redirectUrl = "http://insecure-bank.com/acs")

        composeTestRule.setContent {
            PaymentsdkTheme {
                ThreeDSChallengeCard(
                    challenge = challenge,
                    request = sampleRequest,
                    onResult = {}
                )
            }
        }

        composeTestRule.onNodeWithText(
            "Unsupported authentication URL. Only HTTPS challenge URLs are allowed in production."
        ).assertIsDisplayed()

        // Controls must remain usable because no result has been delivered.
        composeTestRule.onNodeWithText("Approve Challenge").assertIsDisplayed()
        composeTestRule.onNodeWithText("Fail Authentication").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cancel Challenge").assertIsDisplayed()
    }

    @Test
    fun challengeCard_httpsRedirectShowsBrowserPrompt() {
        val challenge = sampleChallenge.copy(
            redirectUrl = "https://secure-bank.com/3ds-challenge"
        )

        composeTestRule.setContent {
            PaymentsdkTheme {
                ThreeDSChallengeCard(
                    challenge = challenge,
                    request = sampleRequest,
                    onResult = {}
                )
            }
        }

        composeTestRule.onNodeWithText(
            "3D Secure authentication has opened in your browser. Complete the challenge there, then return to this app."
        ).assertIsDisplayed()
    }

    @Test
    fun challengeCard_dataUrlInDebugDoesNotShowUnsupportedError() {
        val challenge = sampleChallenge.copy(
            redirectUrl = "data:text/html;charset=utf-8,<html><body>3DS</body></html>"
        )

        composeTestRule.setContent {
            PaymentsdkTheme {
                ThreeDSChallengeCard(
                    challenge = challenge,
                    request = sampleRequest,
                    onResult = {}
                )
            }
        }

        // The unsupported-URL error text should NOT be shown in the debug data: URL branch.
        composeTestRule.onNodeWithText(
            "Unsupported authentication URL. Only HTTPS challenge URLs are allowed in production."
        ).assertDoesNotExist()

        // The controls are still rendered below the WebView surface.
        composeTestRule.onNodeWithText("Approve Challenge").assertIsDisplayed()
    }
}
