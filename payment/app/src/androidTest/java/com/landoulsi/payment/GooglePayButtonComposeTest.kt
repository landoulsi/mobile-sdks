package com.landoulsi.payment

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.landoulsi.payment.shared.checkout.CheckoutUiState
import com.landoulsi.payment.shared.model.Currency
import com.landoulsi.payment.shared.model.Money
import com.landoulsi.payment.shared.model.PaymentMethodType
import com.landoulsi.payment.shared.model.PaymentRequest
import com.landoulsi.payment.ui.GooglePayButton
import com.landoulsi.payment.ui.GooglePayButtonTheme
import com.landoulsi.payment.ui.GooglePayButtonType
import com.landoulsi.payment.ui.theme.PaymentsdkTheme
import org.junit.Rule
import org.junit.Test

class GooglePayButtonComposeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testPayButtonDisplaysLabel() {
        composeTestRule.setContent {
            PaymentsdkTheme {
                GooglePayButton(
                    onClick = {},
                    type = GooglePayButtonType.PAY,
                    buttonTheme = GooglePayButtonTheme.DARK
                )
            }
        }

        composeTestRule.onNodeWithText("Pay with").assertIsDisplayed()
    }

    @Test
    fun testBuyButtonDisplaysLabel() {
        composeTestRule.setContent {
            PaymentsdkTheme {
                GooglePayButton(
                    onClick = {},
                    type = GooglePayButtonType.BUY,
                    buttonTheme = GooglePayButtonTheme.LIGHT
                )
            }
        }

        composeTestRule.onNodeWithText("Buy with").assertIsDisplayed()
    }

    @Test
    fun testCheckoutButtonDisplaysLabel() {
        composeTestRule.setContent {
            PaymentsdkTheme {
                GooglePayButton(
                    onClick = {},
                    type = GooglePayButtonType.CHECKOUT
                )
            }
        }

        composeTestRule.onNodeWithText("Checkout with").assertIsDisplayed()
    }

    @Test
    fun testOrderButtonDisplaysLabel() {
        composeTestRule.setContent {
            PaymentsdkTheme {
                GooglePayButton(
                    onClick = {},
                    type = GooglePayButtonType.ORDER
                )
            }
        }

        composeTestRule.onNodeWithText("Order with").assertIsDisplayed()
    }

    @Test
    fun testSubscribeButtonDisplaysLabel() {
        composeTestRule.setContent {
            PaymentsdkTheme {
                GooglePayButton(
                    onClick = {},
                    type = GooglePayButtonType.SUBSCRIBE
                )
            }
        }

        composeTestRule.onNodeWithText("Subscribe with").assertIsDisplayed()
    }

    @Test
    fun testPlainButtonTypeHasNoLabel() {
        composeTestRule.setContent {
            PaymentsdkTheme {
                GooglePayButton(
                    onClick = {},
                    type = GooglePayButtonType.PLAIN,
                    buttonTheme = GooglePayButtonTheme.DARK
                )
            }
        }

        composeTestRule.onNodeWithText("Pay with").assertDoesNotExist()
        composeTestRule.onNodeWithText("Buy with").assertDoesNotExist()
        composeTestRule.onNodeWithText("Checkout with").assertDoesNotExist()
    }

    @Test
    fun testButtonIsClickableByDefault() {
        var clicked = false
        composeTestRule.setContent {
            PaymentsdkTheme {
                GooglePayButton(
                    onClick = { clicked = true },
                    type = GooglePayButtonType.PAY
                )
            }
        }

        composeTestRule.onNodeWithText("Pay with").performClick()
        assert(clicked) { "Button click callback was not invoked" }
    }

    @Test
    fun testButtonIsDisabledWhenEnabledFalse() {
        var clicked = false
        composeTestRule.setContent {
            PaymentsdkTheme {
                GooglePayButton(
                    onClick = { clicked = true },
                    type = GooglePayButtonType.PAY,
                    enabled = false
                )
            }
        }

        composeTestRule.onNodeWithText("Pay with").assertIsNotEnabled()
        composeTestRule.onNodeWithText("Pay with").performClick()
        assert(!clicked) { "Button should not be clickable when disabled" }
    }

    @Test
    fun testButtonIsDisabledWhenLoading() {
        var clicked = false
        composeTestRule.setContent {
            PaymentsdkTheme {
                GooglePayButton(
                    onClick = { clicked = true },
                    type = GooglePayButtonType.PAY,
                    isLoading = true
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Pay with Google Pay").assertIsNotEnabled()
        composeTestRule.onNodeWithContentDescription("Pay with Google Pay").performClick()
        assert(!clicked) { "Button should not be clickable when loading" }
    }

    @Test
    fun testButtonIsEnabledWhenExplicitlyEnabled() {
        composeTestRule.setContent {
            PaymentsdkTheme {
                GooglePayButton(
                    onClick = {},
                    type = GooglePayButtonType.PAY,
                    enabled = true,
                    isLoading = false
                )
            }
        }

        composeTestRule.onNodeWithText("Pay with").assertIsEnabled()
    }

    @Test
    fun testPayButtonHasContentDescription() {
        composeTestRule.setContent {
            PaymentsdkTheme {
                GooglePayButton(
                    onClick = {},
                    type = GooglePayButtonType.PAY,
                    buttonTheme = GooglePayButtonTheme.DARK
                )
            }
        }

        composeTestRule
            .onNodeWithContentDescription("Pay with Google Pay")
            .assertIsDisplayed()
    }

    @Test
    fun testBuyButtonHasContentDescription() {
        composeTestRule.setContent {
            PaymentsdkTheme {
                GooglePayButton(
                    onClick = {},
                    type = GooglePayButtonType.BUY,
                    buttonTheme = GooglePayButtonTheme.DARK
                )
            }
        }

        composeTestRule
            .onNodeWithContentDescription("Buy with Google Pay")
            .assertIsDisplayed()
    }

    @Test
    fun testPlainButtonHasContentDescription() {
        composeTestRule.setContent {
            PaymentsdkTheme {
                GooglePayButton(
                    onClick = {},
                    type = GooglePayButtonType.PLAIN,
                    buttonTheme = GooglePayButtonTheme.DARK
                )
            }
        }

        composeTestRule
            .onNodeWithContentDescription("Google Pay")
            .assertIsDisplayed()
    }

    @Test
    fun testMultipleClicks() {
        var clickCount = 0
        composeTestRule.setContent {
            PaymentsdkTheme {
                GooglePayButton(
                    onClick = { clickCount++ },
                    type = GooglePayButtonType.PAY
                )
            }
        }

        val node = composeTestRule.onNodeWithText("Pay with")
        node.performClick()
        node.performClick()
        node.performClick()
        assert(clickCount == 3) { "Expected 3 clicks, got $clickCount" }
    }

    // --- Checkout sheet layout tests ---

    private fun makeReadyState(isGooglePayAvailable: Boolean): CheckoutUiState.Ready {
        val request = PaymentRequest(
            id = "test_order",
            amount = Money.fromMajorUnits(9.99, Currency.USD),
            merchantName = "Test Store",
            description = "Test Item",
            allowedPaymentMethods = listOf(PaymentMethodType.GOOGLE_PAY, PaymentMethodType.CARD)
        )
        return CheckoutUiState.Ready(
            request = request,
            isGooglePayAvailable = isGooglePayAvailable
        )
    }

    @Test
    fun checkoutSheet_googlePayAvailable_showsGooglePayButtonAboveCardOption() {
        composeTestRule.setContent {
            PaymentsdkTheme {
                ReadyCheckoutSection(
                    state = makeReadyState(isGooglePayAvailable = true),
                    onGooglePayClick = {}
                )
            }
        }

        // Google Pay button is visible
        composeTestRule.onNodeWithContentDescription("Pay with Google Pay").assertIsDisplayed()
        // "Or pay with card" divider is visible
        composeTestRule.onNodeWithText("OR PAY WITH CARD").assertIsDisplayed()
        // Card option button is visible below the divider
        composeTestRule.onNodeWithText("Pay with Card").assertIsDisplayed()
    }

    @Test
    fun checkoutSheet_googlePayUnavailable_hidesGooglePayButton() {
        composeTestRule.setContent {
            PaymentsdkTheme {
                ReadyCheckoutSection(
                    state = makeReadyState(isGooglePayAvailable = false),
                    onGooglePayClick = {}
                )
            }
        }

        // Google Pay button should NOT be present
        composeTestRule.onNodeWithContentDescription("Pay with Google Pay").assertDoesNotExist()
        // Unavailability notice shown instead
        composeTestRule.onNodeWithText("Google Pay is unavailable on this device.").assertIsDisplayed()
        // Card option is still available
        composeTestRule.onNodeWithText("Pay with Card").assertIsDisplayed()
    }

    @Test
    fun checkoutSheet_googlePayAvailable_cardFormExpandsOnClick() {
        composeTestRule.setContent {
            PaymentsdkTheme {
                ReadyCheckoutSection(
                    state = makeReadyState(isGooglePayAvailable = true),
                    onGooglePayClick = {}
                )
            }
        }

        // Before click, "Pay with Card" button is shown but card number field is not
        composeTestRule.onNodeWithText("Card number").assertDoesNotExist()

        // Tap "Pay with Card" to expand inline card form
        composeTestRule.onNodeWithText("Pay with Card").performClick()

        // After expansion, the card input form fields appear
        composeTestRule.onNodeWithText("Card number").assertIsDisplayed()
        composeTestRule.onNodeWithText("Expiry").assertIsDisplayed()
        composeTestRule.onNodeWithText("CVC").assertIsDisplayed()
    }

    @Test
    fun checkoutSheet_googlePayAvailable_googlePayClickCallbackFires() {
        var clicked = false
        composeTestRule.setContent {
            PaymentsdkTheme {
                ReadyCheckoutSection(
                    state = makeReadyState(isGooglePayAvailable = true),
                    onGooglePayClick = { clicked = true }
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Pay with Google Pay").performClick()
        assert(clicked) { "onGooglePayClick callback should be invoked when button is tapped" }
    }

    @Test
    fun checkoutSheet_googlePayUnavailable_cardFormExpandsOnClick() {
        composeTestRule.setContent {
            PaymentsdkTheme {
                ReadyCheckoutSection(
                    state = makeReadyState(isGooglePayAvailable = false),
                    onGooglePayClick = {}
                )
            }
        }

        // Card form is collapsed initially
        composeTestRule.onNodeWithText("Card number").assertDoesNotExist()

        composeTestRule.onNodeWithText("Pay with Card").performClick()

        // Card form fields appear after expansion
        composeTestRule.onNodeWithText("Card number").assertIsDisplayed()
    }

    @Test
    fun checkoutSheet_dividerAlwaysVisible() {
        composeTestRule.setContent {
            PaymentsdkTheme {
                ReadyCheckoutSection(
                    state = makeReadyState(isGooglePayAvailable = true),
                    onGooglePayClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText("OR PAY WITH CARD").assertIsDisplayed()
    }

    // --- Additional GooglePayButton tests for uncovered paths ---

    @Test
    fun testSubscribeButtonHasContentDescription() {
        composeTestRule.setContent {
            PaymentsdkTheme {
                GooglePayButton(
                    onClick = {},
                    type = GooglePayButtonType.SUBSCRIBE,
                    buttonTheme = GooglePayButtonTheme.DARK
                )
            }
        }

        composeTestRule
            .onNodeWithContentDescription("Subscribe with Google Pay")
            .assertIsDisplayed()
    }

    @Test
    fun testOrderButtonHasContentDescription() {
        composeTestRule.setContent {
            PaymentsdkTheme {
                GooglePayButton(
                    onClick = {},
                    type = GooglePayButtonType.ORDER,
                    buttonTheme = GooglePayButtonTheme.DARK
                )
            }
        }

        composeTestRule
            .onNodeWithContentDescription("Order with Google Pay")
            .assertIsDisplayed()
    }

    @Test
    fun testCheckoutButtonHasContentDescription() {
        composeTestRule.setContent {
            PaymentsdkTheme {
                GooglePayButton(
                    onClick = {},
                    type = GooglePayButtonType.CHECKOUT,
                    buttonTheme = GooglePayButtonTheme.DARK
                )
            }
        }

        composeTestRule
            .onNodeWithContentDescription("Checkout with Google Pay")
            .assertIsDisplayed()
    }

    @Test
    fun testLightThemeButtonDisplaysLabel() {
        composeTestRule.setContent {
            PaymentsdkTheme {
                GooglePayButton(
                    onClick = {},
                    type = GooglePayButtonType.PAY,
                    buttonTheme = GooglePayButtonTheme.LIGHT
                )
            }
        }

        composeTestRule.onNodeWithText("Pay with").assertIsDisplayed()
        composeTestRule
            .onNodeWithContentDescription("Pay with Google Pay")
            .assertIsDisplayed()
    }

    @Test
    fun testDynamicThemeButtonDisplaysLabel() {
        composeTestRule.setContent {
            PaymentsdkTheme {
                GooglePayButton(
                    onClick = {},
                    type = GooglePayButtonType.PAY,
                    buttonTheme = GooglePayButtonTheme.DYNAMIC
                )
            }
        }

        // Regardless of system theme, the button should render with label and content description
        composeTestRule.onNodeWithText("Pay with").assertIsDisplayed()
        composeTestRule
            .onNodeWithContentDescription("Pay with Google Pay")
            .assertIsDisplayed()
    }

    @Test
    fun testLoadingStateShowsNoLabelText() {
        composeTestRule.setContent {
            PaymentsdkTheme {
                GooglePayButton(
                    onClick = {},
                    type = GooglePayButtonType.PAY,
                    isLoading = true
                )
            }
        }

        // While loading, the label text is replaced by a progress indicator — text should not exist
        composeTestRule.onNodeWithText("Pay with").assertDoesNotExist()
    }

    @Test
    fun testEnabledFalseDoesNotFireOnClick() {
        var clicked = false
        composeTestRule.setContent {
            PaymentsdkTheme {
                GooglePayButton(
                    onClick = { clicked = true },
                    type = GooglePayButtonType.BUY,
                    enabled = false
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Buy with Google Pay").performClick()
        assert(!clicked) { "Disabled button should not fire onClick" }
    }
}
