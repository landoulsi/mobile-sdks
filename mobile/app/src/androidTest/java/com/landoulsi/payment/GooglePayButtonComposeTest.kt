package com.landoulsi.payment

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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

        composeTestRule.onNodeWithText("Pay with").assertIsNotEnabled()
        composeTestRule.onNodeWithText("Pay with").performClick()
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
}
