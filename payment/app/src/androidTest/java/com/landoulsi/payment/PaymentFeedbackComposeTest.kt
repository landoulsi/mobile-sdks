package com.landoulsi.payment

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.landoulsi.payment.shared.model.CardNetwork
import com.landoulsi.payment.shared.model.PaymentErrorCode
import com.landoulsi.payment.shared.model.PaymentMethodType
import com.landoulsi.payment.shared.model.PaymentResult
import com.landoulsi.payment.ui.payment.PaymentCanceledCard
import com.landoulsi.payment.ui.payment.PaymentDetailRow
import com.landoulsi.payment.ui.payment.PaymentFailureCard
import com.landoulsi.payment.ui.payment.PaymentSuccessCard
import com.landoulsi.payment.ui.payment.ProcessingPaymentCard
import com.landoulsi.payment.ui.payment.TokenizationErrorBanner
import com.landoulsi.payment.ui.theme.PaymentsdkTheme
import org.junit.Rule
import org.junit.Test

class PaymentFeedbackComposeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // ──── ProcessingPaymentCard ─────────────────────────────

    @Test
    fun processingCard_googlePay_showsCorrectSubtitle() {
        composeTestRule.setContent {
            PaymentsdkTheme {
                ProcessingPaymentCard(methodType = PaymentMethodType.GOOGLE_PAY)
            }
        }

        composeTestRule.onNodeWithText("Processing payment…").assertIsDisplayed()
        composeTestRule.onNodeWithText("Authorizing Google Pay transaction…").assertIsDisplayed()
    }

    @Test
    fun processingCard_card_showsCorrectSubtitle() {
        composeTestRule.setContent {
            PaymentsdkTheme {
                ProcessingPaymentCard(methodType = PaymentMethodType.CARD)
            }
        }

        composeTestRule.onNodeWithText("Processing payment…").assertIsDisplayed()
        composeTestRule.onNodeWithText("Tokenizing card details…").assertIsDisplayed()
    }

    @Test
    fun processingCard_otherMethod_showsGenericSubtitle() {
        composeTestRule.setContent {
            PaymentsdkTheme {
                ProcessingPaymentCard(methodType = PaymentMethodType.PAYPAL)
            }
        }

        composeTestRule.onNodeWithText("Processing payment…").assertIsDisplayed()
    }

    // ──── PaymentSuccessCard ─────────────────────────────────

    @Test
    fun successCard_showsTitleAndTransactionId() {
        val result = PaymentResult.Success(
            transactionId = "tx_abc123",
            paymentMethodType = PaymentMethodType.GOOGLE_PAY
        )
        composeTestRule.setContent {
            PaymentsdkTheme {
                PaymentSuccessCard(result = result, onResetClick = {})
            }
        }

        composeTestRule.onNodeWithText("Payment Successful!").assertIsDisplayed()
        composeTestRule.onNodeWithText("tx_abc123").assertIsDisplayed()
    }

    @Test
    fun successCard_googlePay_showsGooglePayLabel() {
        val result = PaymentResult.Success(
            transactionId = "tx_001",
            paymentMethodType = PaymentMethodType.GOOGLE_PAY
        )
        composeTestRule.setContent {
            PaymentsdkTheme {
                PaymentSuccessCard(result = result, onResetClick = {})
            }
        }

        composeTestRule.onNodeWithText("Google Pay").assertIsDisplayed()
    }

    @Test
    fun successCard_card_showsCardLabel() {
        val result = PaymentResult.Success(
            transactionId = "tx_002",
            paymentMethodType = PaymentMethodType.CARD
        )
        composeTestRule.setContent {
            PaymentsdkTheme {
                PaymentSuccessCard(result = result, onResetClick = {})
            }
        }

        composeTestRule.onNodeWithText("Card").assertIsDisplayed()
    }

    @Test
    fun successCard_unknownMethod_showsMethodType() {
        val result = PaymentResult.Success(
            transactionId = "tx_003",
            paymentMethodType = PaymentMethodType.PAYPAL
        )
        composeTestRule.setContent {
            PaymentsdkTheme {
                PaymentSuccessCard(result = result, onResetClick = {})
            }
        }

        composeTestRule.onNodeWithText("PAYPAL").assertIsDisplayed()
    }

    @Test
    fun successCard_withCardDetails_showsLast4AndNetwork() {
        val result = PaymentResult.Success(
            transactionId = "tx_004",
            paymentMethodType = PaymentMethodType.CARD,
            last4 = "4242",
            cardNetwork = CardNetwork.VISA
        )
        composeTestRule.setContent {
            PaymentsdkTheme {
                PaymentSuccessCard(result = result, onResetClick = {})
            }
        }

        composeTestRule.onNodeWithText("VISA •••• 4242").assertIsDisplayed()
    }

    @Test
    fun successCard_withCardDetails_unknownNetwork_showsCardPrefix() {
        val result = PaymentResult.Success(
            transactionId = "tx_005",
            paymentMethodType = PaymentMethodType.CARD,
            last4 = "1234",
            cardNetwork = null
        )
        composeTestRule.setContent {
            PaymentsdkTheme {
                PaymentSuccessCard(result = result, onResetClick = {})
            }
        }

        composeTestRule.onNodeWithText("Card •••• 1234").assertIsDisplayed()
    }

    @Test
    fun successCard_withoutCardDetails_hidesCardRow() {
        val result = PaymentResult.Success(
            transactionId = "tx_006",
            paymentMethodType = PaymentMethodType.GOOGLE_PAY
        )
        composeTestRule.setContent {
            PaymentsdkTheme {
                PaymentSuccessCard(result = result, onResetClick = {})
            }
        }

        composeTestRule.onNodeWithText("Card ••••").assertDoesNotExist()
    }

    @Test
    fun successCard_withToken_showsTruncatedToken() {
        val token = "tok_1234567890abcdef"
        val result = PaymentResult.Success(
            transactionId = "tx_007",
            paymentMethodType = PaymentMethodType.CARD,
            token = token
        )
        composeTestRule.setContent {
            PaymentsdkTheme {
                PaymentSuccessCard(result = result, onResetClick = {})
            }
        }

        composeTestRule.onNodeWithText("tok_1234567890abcd…").assertIsDisplayed()
    }

    @Test
    fun successCard_withoutToken_hidesTokenRow() {
        val result = PaymentResult.Success(
            transactionId = "tx_008",
            paymentMethodType = PaymentMethodType.GOOGLE_PAY,
            token = null
        )
        composeTestRule.setContent {
            PaymentsdkTheme {
                PaymentSuccessCard(result = result, onResetClick = {})
            }
        }

        composeTestRule.onNodeWithText("Payment Token").assertDoesNotExist()
    }

    @Test
    fun successCard_resetButtonCallsCallback() {
        var resetClicked = false
        val result = PaymentResult.Success(
            transactionId = "tx_009",
            paymentMethodType = PaymentMethodType.CARD
        )
        composeTestRule.setContent {
            PaymentsdkTheme {
                PaymentSuccessCard(result = result, onResetClick = { resetClicked = true })
            }
        }

        composeTestRule.onNodeWithText("Reset Demo").performClick()
        assert(resetClicked) { "onResetClick should be invoked when reset button is tapped" }
    }

    @Test
    fun successCard_showsLabels() {
        val result = PaymentResult.Success(
            transactionId = "tx_010",
            paymentMethodType = PaymentMethodType.CARD,
            last4 = "9999",
            cardNetwork = CardNetwork.MASTERCARD,
            token = "tok_test"
        )
        composeTestRule.setContent {
            PaymentsdkTheme {
                PaymentSuccessCard(result = result, onResetClick = {})
            }
        }

        composeTestRule.onNodeWithText("Transaction ID").assertIsDisplayed()
        composeTestRule.onNodeWithText("Payment Method").assertIsDisplayed()
        composeTestRule.onNodeWithText("Card").assertIsDisplayed()
        composeTestRule.onNodeWithText("Payment Token").assertIsDisplayed()
    }

    // ──── PaymentFailureCard ─────────────────────────────────

    @Test
    fun failureCard_showsTitle() {
        val failure = PaymentResult.Failure(
            errorCode = PaymentErrorCode.NETWORK_ERROR,
            message = "Connection timed out"
        )
        composeTestRule.setContent {
            PaymentsdkTheme {
                PaymentFailureCard(failure = failure, onRetryClick = {})
            }
        }

        composeTestRule.onNodeWithText("Payment Failed").assertIsDisplayed()
    }

    @Test
    fun failureCard_networkError_showsCorrectLabel() {
        val failure = PaymentResult.Failure(
            errorCode = PaymentErrorCode.NETWORK_ERROR,
            message = "No internet"
        )
        composeTestRule.setContent {
            PaymentsdkTheme {
                PaymentFailureCard(failure = failure, onRetryClick = {})
            }
        }

        composeTestRule.onNodeWithText("Network error: No internet").assertIsDisplayed()
    }

    @Test
    fun failureCard_gatewayError_showsCorrectLabel() {
        val failure = PaymentResult.Failure(
            errorCode = PaymentErrorCode.GATEWAY_ERROR,
            message = "Server error"
        )
        composeTestRule.setContent {
            PaymentsdkTheme {
                PaymentFailureCard(failure = failure, onRetryClick = {})
            }
        }

        composeTestRule.onNodeWithText("Gateway error: Server error").assertIsDisplayed()
    }

    @Test
    fun failureCard_configurationError_showsCorrectLabel() {
        val failure = PaymentResult.Failure(
            errorCode = PaymentErrorCode.CONFIGURATION_ERROR,
            message = "Invalid API key"
        )
        composeTestRule.setContent {
            PaymentsdkTheme {
                PaymentFailureCard(failure = failure, onRetryClick = {})
            }
        }

        composeTestRule.onNodeWithText("Configuration error: Invalid API key").assertIsDisplayed()
    }

    @Test
    fun failureCard_cardDeclined_showsCorrectLabel() {
        val failure = PaymentResult.Failure(
            errorCode = PaymentErrorCode.CARD_DECLINED,
            message = "Do not honor"
        )
        composeTestRule.setContent {
            PaymentsdkTheme {
                PaymentFailureCard(failure = failure, onRetryClick = {})
            }
        }

        composeTestRule.onNodeWithText("Card declined: Do not honor").assertIsDisplayed()
    }

    @Test
    fun failureCard_expiredCard_showsCorrectLabel() {
        val failure = PaymentResult.Failure(
            errorCode = PaymentErrorCode.EXPIRED_CARD,
            message = "Card expired"
        )
        composeTestRule.setContent {
            PaymentsdkTheme {
                PaymentFailureCard(failure = failure, onRetryClick = {})
            }
        }

        composeTestRule.onNodeWithText("Expired card: Card expired").assertIsDisplayed()
    }

    @Test
    fun failureCard_insufficientFunds_showsCorrectLabel() {
        val failure = PaymentResult.Failure(
            errorCode = PaymentErrorCode.INSUFFICIENT_FUNDS,
            message = "Not enough money"
        )
        composeTestRule.setContent {
            PaymentsdkTheme {
                PaymentFailureCard(failure = failure, onRetryClick = {})
            }
        }

        composeTestRule.onNodeWithText("Insufficient funds: Not enough money").assertIsDisplayed()
    }

    @Test
    fun failureCard_authenticationFailed_showsCorrectLabel() {
        val failure = PaymentResult.Failure(
            errorCode = PaymentErrorCode.AUTHENTICATION_FAILED,
            message = "3DS failed"
        )
        composeTestRule.setContent {
            PaymentsdkTheme {
                PaymentFailureCard(failure = failure, onRetryClick = {})
            }
        }

        composeTestRule.onNodeWithText("Authentication failed: 3DS failed").assertIsDisplayed()
    }

    @Test
    fun failureCard_userCanceled_showsCorrectLabel() {
        val failure = PaymentResult.Failure(
            errorCode = PaymentErrorCode.USER_CANCELED,
            message = "User dismissed"
        )
        composeTestRule.setContent {
            PaymentsdkTheme {
                PaymentFailureCard(failure = failure, onRetryClick = {})
            }
        }

        composeTestRule.onNodeWithText("User canceled: User dismissed").assertIsDisplayed()
    }

    @Test
    fun failureCard_unknownError_showsCorrectLabel() {
        val failure = PaymentResult.Failure(
            errorCode = PaymentErrorCode.UNKNOWN,
            message = "Something went wrong"
        )
        composeTestRule.setContent {
            PaymentsdkTheme {
                PaymentFailureCard(failure = failure, onRetryClick = {})
            }
        }

        composeTestRule.onNodeWithText("Unknown error: Something went wrong").assertIsDisplayed()
    }

    @Test
    fun failureCard_paymentMethodUnavailable_showsCorrectLabel() {
        val failure = PaymentResult.Failure(
            errorCode = PaymentErrorCode.PAYMENT_METHOD_UNAVAILABLE,
            message = "Method not supported"
        )
        composeTestRule.setContent {
            PaymentsdkTheme {
                PaymentFailureCard(failure = failure, onRetryClick = {})
            }
        }

        composeTestRule.onNodeWithText("Payment method unavailable: Method not supported").assertIsDisplayed()
    }

    @Test
    fun failureCard_tryAgainButtonCallsCallback() {
        var retryClicked = false
        val failure = PaymentResult.Failure(
            errorCode = PaymentErrorCode.NETWORK_ERROR,
            message = "Timeout"
        )
        composeTestRule.setContent {
            PaymentsdkTheme {
                PaymentFailureCard(failure = failure, onRetryClick = { retryClicked = true })
            }
        }

        composeTestRule.onNodeWithText("Try Again").performClick()
        assert(retryClicked) { "onRetryClick should be invoked when try again button is tapped" }
    }

    // ──── PaymentCanceledCard ────────────────────────────────

    @Test
    fun canceledCard_showsTitleAndDescription() {
        composeTestRule.setContent {
            PaymentsdkTheme {
                PaymentCanceledCard(onRetryClick = {})
            }
        }

        composeTestRule.onNodeWithText("Payment Canceled").assertIsDisplayed()
        composeTestRule.onNodeWithText("The payment sheet was dismissed. You can retry when ready.").assertIsDisplayed()
    }

    @Test
    fun canceledCard_tryAgainButtonCallsCallback() {
        var retryClicked = false
        composeTestRule.setContent {
            PaymentsdkTheme {
                PaymentCanceledCard(onRetryClick = { retryClicked = true })
            }
        }

        composeTestRule.onNodeWithText("Try Again").performClick()
        assert(retryClicked) { "onRetryClick should be invoked when try again button is tapped" }
    }

    // ──── PaymentDetailRow ───────────────────────────────────

    @Test
    fun detailRow_showsLabelAndValue() {
        composeTestRule.setContent {
            PaymentsdkTheme {
                PaymentDetailRow(label = "Order ID", value = "12345")
            }
        }

        composeTestRule.onNodeWithText("Order ID").assertIsDisplayed()
        composeTestRule.onNodeWithText("12345").assertIsDisplayed()
    }

    // ──── TokenizationErrorBanner ────────────────────────────

    @Test
    fun tokenizationBanner_showsErrorMessage() {
        composeTestRule.setContent {
            PaymentsdkTheme {
                TokenizationErrorBanner(
                    error = RuntimeException("Network timeout"),
                    onDismiss = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Network timeout").assertIsDisplayed()
    }

    @Test
    fun tokenizationBanner_withDismiss_showsDismissButton() {
        composeTestRule.setContent {
            PaymentsdkTheme {
                TokenizationErrorBanner(
                    error = RuntimeException("Failed"),
                    onDismiss = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Dismiss").assertIsDisplayed()
    }

    @Test
    fun tokenizationBanner_dismissButtonCallsCallback() {
        var dismissed = false
        composeTestRule.setContent {
            PaymentsdkTheme {
                TokenizationErrorBanner(
                    error = RuntimeException("Error"),
                    onDismiss = { dismissed = true }
                )
            }
        }

        composeTestRule.onNodeWithText("Dismiss").performClick()
        assert(dismissed) { "onDismiss should be invoked when dismiss button is tapped" }
    }

    @Test
    fun tokenizationBanner_nullOnDismiss_hidesDismissButton() {
        composeTestRule.setContent {
            PaymentsdkTheme {
                TokenizationErrorBanner(
                    error = RuntimeException("Error"),
                    onDismiss = null
                )
            }
        }

        composeTestRule.onNodeWithText("Dismiss").assertDoesNotExist()
    }

    @Test
    fun tokenizationBanner_nullThrowable_showsDefaultMessage() {
        composeTestRule.setContent {
            PaymentsdkTheme {
                TokenizationErrorBanner(
                    error = null,
                    onDismiss = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Tokenization failed").assertIsDisplayed()
    }

    @Test
    fun tokenizationBanner_exceptionWithMessage_showsMessage() {
        composeTestRule.setContent {
            PaymentsdkTheme {
                TokenizationErrorBanner(
                    error = IllegalArgumentException("Bad request format"),
                    onDismiss = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Bad request format").assertIsDisplayed()
    }
}
