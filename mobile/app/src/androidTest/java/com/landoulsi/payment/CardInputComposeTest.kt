package com.landoulsi.payment

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.landoulsi.payment.shared.model.CardNetwork
import com.landoulsi.payment.shared.validation.CardFormState
import com.landoulsi.payment.shared.validation.CardNumberState
import com.landoulsi.payment.shared.validation.CvcState
import com.landoulsi.payment.shared.validation.ExpiryState
import com.landoulsi.payment.ui.card.CardInputForm
import com.landoulsi.payment.ui.card.CardNetworkIcon
import com.landoulsi.payment.ui.card.CardNumberInput
import com.landoulsi.payment.ui.card.CvcInput
import com.landoulsi.payment.ui.card.ExpiryInput
import com.landoulsi.payment.ui.theme.PaymentsdkTheme
import org.junit.Rule
import org.junit.Test

class CardInputComposeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testCardNetworkIconsDisplayCorrectTextAndContentDescription() {
        composeTestRule.setContent {
            PaymentsdkTheme {
                CardNetworkIcon(network = CardNetwork.VISA)
                CardNetworkIcon(network = CardNetwork.MASTERCARD)
                CardNetworkIcon(network = CardNetwork.AMEX)
                CardNetworkIcon(network = CardNetwork.DISCOVER)
                CardNetworkIcon(network = CardNetwork.JCB)
                CardNetworkIcon(network = CardNetwork.DINERS_CLUB)
                CardNetworkIcon(network = CardNetwork.UNION_PAY)
                CardNetworkIcon(network = CardNetwork.INTERAC)
            }
        }

        composeTestRule.onNodeWithText("VISA").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Network: Visa").assertIsDisplayed()

        composeTestRule.onNodeWithText("MC").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Network: Mastercard").assertIsDisplayed()

        composeTestRule.onNodeWithText("AMEX").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Network: American Express").assertIsDisplayed()

        composeTestRule.onNodeWithText("DISC").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Network: Discover").assertIsDisplayed()

        composeTestRule.onNodeWithText("JCB").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Network: JCB").assertIsDisplayed()

        composeTestRule.onNodeWithText("DINERS").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Network: Diners Club").assertIsDisplayed()

        composeTestRule.onNodeWithText("UPI").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Network: UnionPay").assertIsDisplayed()

        composeTestRule.onNodeWithText("INTERAC").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Network: Interac").assertIsDisplayed()
    }

    @Test
    fun testCardNumberInputInitialState() {
        composeTestRule.setContent {
            PaymentsdkTheme {
                CardNumberInput(
                    state = CardNumberState.initial(),
                    onValueChange = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Card number").assertIsDisplayed()
        composeTestRule.onNodeWithText("1234 5678 9012 3456").assertIsDisplayed()
    }

    @Test
    fun testCardNumberInputWithDetectedNetwork() {
        composeTestRule.setContent {
            PaymentsdkTheme {
                CardNumberInput(
                    state = CardNumberState(rawValue = "4242", network = CardNetwork.VISA),
                    onValueChange = {}
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Network: Visa").assertIsDisplayed()
        composeTestRule.onNodeWithText("VISA").assertIsDisplayed()
    }

    @Test
    fun testCardNumberInputShowsError() {
        val errorMessage = "Invalid card number"
        composeTestRule.setContent {
            PaymentsdkTheme {
                CardNumberInput(
                    state = CardNumberState(
                        rawValue = "4242424242424243",
                        isComplete = true,
                        isValid = false,
                        error = errorMessage
                    ),
                    isError = true,
                    supportingText = errorMessage,
                    onValueChange = {}
                )
            }
        }

        composeTestRule.onNodeWithText(errorMessage).assertIsDisplayed()
    }

    @Test
    fun testExpiryInputInitialState() {
        composeTestRule.setContent {
            PaymentsdkTheme {
                ExpiryInput(
                    state = ExpiryState.initial(),
                    onValueChange = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Expiry").assertIsDisplayed()
        composeTestRule.onNodeWithText("MM/YY").assertIsDisplayed()
    }

    @Test
    fun testExpiryInputValidTrailingIcon() {
        composeTestRule.setContent {
            PaymentsdkTheme {
                ExpiryInput(
                    state = ExpiryState(
                        rawValue = "1228",
                        month = 12,
                        year = 28,
                        isValid = true,
                        isComplete = true
                    ),
                    onValueChange = {}
                )
            }
        }

        composeTestRule.onNodeWithText("✓").assertIsDisplayed()
    }

    @Test
    fun testExpiryInputInvalidTrailingIcon() {
        composeTestRule.setContent {
            PaymentsdkTheme {
                ExpiryInput(
                    state = ExpiryState(
                        rawValue = "1328",
                        isValid = false,
                        isComplete = true
                    ),
                    isError = true,
                    onValueChange = {}
                )
            }
        }

        composeTestRule.onNodeWithText("!").assertIsDisplayed()
    }

    @Test
    fun testCvcInputInitialState() {
        composeTestRule.setContent {
            PaymentsdkTheme {
                CvcInput(
                    state = CvcState.initial(),
                    onValueChange = {}
                )
            }
        }

        composeTestRule.onNodeWithText("CVC").assertIsDisplayed()
    }

    @Test
    fun testCvcInputValidTrailingIcon() {
        composeTestRule.setContent {
            PaymentsdkTheme {
                CvcInput(
                    state = CvcState(
                        rawValue = "123",
                        isValid = true,
                        isComplete = true
                    ),
                    onValueChange = {}
                )
            }
        }

        composeTestRule.onNodeWithText("✓").assertIsDisplayed()
    }

    @Test
    fun testCvcInputInvalidTrailingIcon() {
        composeTestRule.setContent {
            PaymentsdkTheme {
                CvcInput(
                    state = CvcState(
                        rawValue = "12",
                        isValid = false,
                        isComplete = true
                    ),
                    isError = true,
                    onValueChange = {}
                )
            }
        }

        composeTestRule.onNodeWithText("!").assertIsDisplayed()
    }

    @Test
    fun testCardInputFormRendersAllThreeFields() {
        composeTestRule.setContent {
            PaymentsdkTheme {
                CardInputForm(
                    onFormReady = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Card number").assertIsDisplayed()
        composeTestRule.onNodeWithText("Expiry").assertIsDisplayed()
        composeTestRule.onNodeWithText("CVC").assertIsDisplayed()
    }

    @Test
    fun testInputsDisabledState() {
        composeTestRule.setContent {
            PaymentsdkTheme {
                CardInputForm(
                    onFormReady = {},
                    enabled = false
                )
            }
        }

        composeTestRule.onNodeWithText("Card number").assertIsNotEnabled()
        composeTestRule.onNodeWithText("Expiry").assertIsNotEnabled()
        composeTestRule.onNodeWithText("CVC").assertIsNotEnabled()
    }

    // ──── Error String Resource Mapping ──────────────────────

    @Test
    fun testCardInputForm_emptyAfterSubmission_showsRequiredErrors() {
        val formState = CardFormState.initial().markSubmissionAttempted()
        composeTestRule.setContent {
            PaymentsdkTheme {
                CardInputForm(
                    onFormReady = {},
                    initialState = formState
                )
            }
        }

        composeTestRule.onNodeWithText("This field is required").assertIsDisplayed()
    }

    @Test
    fun testCardInputForm_incompleteNumber_showsIncompleteError() {
        val formState = CardFormState(
            number = CardNumberState(rawValue = "4242", isComplete = false)
        ).markSubmissionAttempted()
        composeTestRule.setContent {
            PaymentsdkTheme {
                CardInputForm(
                    onFormReady = {},
                    initialState = formState
                )
            }
        }

        composeTestRule.onNodeWithText("Input is incomplete").assertIsDisplayed()
    }

    @Test
    fun testCardInputForm_invalidLuhn_showsInvalidCardNumberError() {
        val formState = CardFormState(
            number = CardNumberState(
                rawValue = "4242424242424243",
                isComplete = true,
                isValid = false,
                error = "Invalid card number"
            )
        )
        composeTestRule.setContent {
            PaymentsdkTheme {
                CardInputForm(
                    onFormReady = {},
                    initialState = formState
                )
            }
        }

        composeTestRule.onNodeWithText("Invalid card number").assertIsDisplayed()
    }

    @Test
    fun testCardInputForm_invalidExpiry_showsInvalidExpiryError() {
        val formState = CardFormState(
            expiry = ExpiryState(
                rawValue = "0120",
                isComplete = true,
                isValid = false,
                error = "Expired or invalid date"
            )
        )
        composeTestRule.setContent {
            PaymentsdkTheme {
                CardInputForm(
                    onFormReady = {},
                    initialState = formState
                )
            }
        }

        composeTestRule.onNodeWithText("Expired or invalid date").assertIsDisplayed()
    }

    @Test
    fun testCardInputForm_invalidCvc_showsInvalidCvcError() {
        val formState = CardFormState(
            cvc = CvcState(
                rawValue = "12",
                isComplete = true,
                isValid = false,
                error = "CVC is invalid"
            )
        )
        composeTestRule.setContent {
            PaymentsdkTheme {
                CardInputForm(
                    onFormReady = {},
                    initialState = formState
                )
            }
        }

        composeTestRule.onNodeWithText("CVC is invalid").assertIsDisplayed()
    }

    @Test
    fun testCardInputForm_initialState_noErrorsShown() {
        composeTestRule.setContent {
            PaymentsdkTheme {
                CardInputForm(
                    onFormReady = {},
                    initialState = CardFormState.initial()
                )
            }
        }

        composeTestRule.onNodeWithText("This field is required").assertDoesNotExist()
        composeTestRule.onNodeWithText("Input is incomplete").assertDoesNotExist()
        composeTestRule.onNodeWithText("Invalid card number").assertDoesNotExist()
        composeTestRule.onNodeWithText("Expired or invalid date").assertDoesNotExist()
        composeTestRule.onNodeWithText("CVC is invalid").assertDoesNotExist()
    }

    @Test
    fun testCardInputForm_incompleteExpiryAfterSubmission_showsIncompleteError() {
        val formState = CardFormState(
            expiry = ExpiryState(rawValue = "12", isComplete = false)
        ).markSubmissionAttempted()
        composeTestRule.setContent {
            PaymentsdkTheme {
                CardInputForm(
                    onFormReady = {},
                    initialState = formState
                )
            }
        }

        composeTestRule.onNodeWithText("Input is incomplete").assertIsDisplayed()
    }

    @Test
    fun testCardInputForm_incompleteCvcAfterSubmission_showsIncompleteError() {
        val formState = CardFormState(
            cvc = CvcState(rawValue = "1", isComplete = false)
        ).markSubmissionAttempted()
        composeTestRule.setContent {
            PaymentsdkTheme {
                CardInputForm(
                    onFormReady = {},
                    initialState = formState
                )
            }
        }

        composeTestRule.onNodeWithText("Input is incomplete").assertIsDisplayed()
    }

    @Test
    fun testCardInputForm_incompleteCvcAfterSubmissionError_showsInvalidCvc() {
        val formState = CardFormState(
            cvc = CvcState(
                rawValue = "99",
                isComplete = true,
                isValid = false,
                error = "CVC is invalid"
            )
        ).markSubmissionAttempted()
        composeTestRule.setContent {
            PaymentsdkTheme {
                CardInputForm(
                    onFormReady = {},
                    initialState = formState
                )
            }
        }

        composeTestRule.onNodeWithText("CVC is invalid").assertIsDisplayed()
    }

    // ──── CVC Network Length Tests ───────────────────────────

    @Test
    fun testCvcInput_amexNetwork_showsFourDigitPlaceholder() {
        composeTestRule.setContent {
            PaymentsdkTheme {
                CvcInput(
                    state = CvcState.initial(),
                    onValueChange = {},
                    network = CardNetwork.AMEX
                )
            }
        }

        composeTestRule.onNodeWithText("CVC").assertIsDisplayed()
    }

    @Test
    fun testCvcInput_withTrailingIcons_showsCheckmarkForComplete() {
        composeTestRule.setContent {
            PaymentsdkTheme {
                CvcInput(
                    state = CvcState(
                        rawValue = "1234",
                        isValid = true,
                        isComplete = true
                    ),
                    onValueChange = {},
                    network = CardNetwork.AMEX
                )
            }
        }

        composeTestRule.onNodeWithText("✓").assertIsDisplayed()
    }
}
