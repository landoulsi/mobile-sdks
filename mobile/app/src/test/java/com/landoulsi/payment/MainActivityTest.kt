package com.landoulsi.payment

import com.landoulsi.payment.shared.checkout.CheckoutUiState
import com.landoulsi.payment.shared.checkout.CheckoutViewModel
import com.landoulsi.payment.shared.checkout.isGooglePayReady
import com.landoulsi.payment.shared.checkout.isLoading
import com.landoulsi.payment.shared.model.CardNetwork
import com.landoulsi.payment.shared.model.Currency
import com.landoulsi.payment.shared.model.GooglePayAuthMethod
import com.landoulsi.payment.shared.model.GooglePayEnvironment
import com.landoulsi.payment.shared.model.PaymentErrorCode
import com.landoulsi.payment.shared.model.PaymentMethodType
import com.landoulsi.payment.shared.model.PaymentResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MainActivityTest {

    @Test
    fun testDemoGooglePayConfig() {
        val config = MainActivity.createDemoGooglePayConfig()

        assertEquals(GooglePayEnvironment.TEST, config.environment)
        assertEquals("12345678901234567890", config.merchantId)
        assertEquals("Payment SDK Demo Store", config.merchantName)
        assertTrue(config.allowedCardNetworks.contains(CardNetwork.VISA))
        assertTrue(config.allowedCardNetworks.contains(CardNetwork.MASTERCARD))
        assertTrue(config.allowedAuthMethods.contains(GooglePayAuthMethod.PAN_ONLY))
        assertTrue(config.allowedAuthMethods.contains(GooglePayAuthMethod.CRYPTOGRAM_3DS))
        assertEquals("PAYMENT_GATEWAY", config.tokenizationSpecification.type)
        assertEquals("stripe", config.tokenizationSpecification.parameters["gateway"])
        assertTrue(config.billingAddressRequired)
        assertTrue(config.emailRequired)
    }

    @Test
    fun testDemoGooglePayConfigContainsAllCardNetworks() {
        val config = MainActivity.createDemoGooglePayConfig()

        assertEquals(4, config.allowedCardNetworks.size)
        assertTrue(config.allowedCardNetworks.contains(CardNetwork.VISA))
        assertTrue(config.allowedCardNetworks.contains(CardNetwork.MASTERCARD))
        assertTrue(config.allowedCardNetworks.contains(CardNetwork.AMEX))
        assertTrue(config.allowedCardNetworks.contains(CardNetwork.DISCOVER))
    }

    @Test
    fun testDemoGooglePayConfigBillingAddressParameters() {
        val config = MainActivity.createDemoGooglePayConfig()

        assertNotNull(config.billingAddressParameters)
        assertTrue(config.billingAddressRequired)
        assertTrue(config.billingAddressParameters?.phoneNumberRequired == true)
    }

    @Test
    fun testDemoGooglePayConfigTokenizationParameters() {
        val config = MainActivity.createDemoGooglePayConfig()

        val params = config.tokenizationSpecification.parameters
        assertEquals("pk_test_TYooMQauvdEDq54NiTphI7jx", params["stripe:publishableKey"])
        assertNotNull(params["gateway"])
    }

    @Test
    fun testDemoPaymentRequest() {
        val config = MainActivity.createDemoGooglePayConfig()
        val request = MainActivity.createDemoPaymentRequest(config)

        assertEquals("order_demo_1001", request.id)
        assertEquals(Currency.USD, request.amount.currency)
        assertEquals("29.99", request.amount.formattedAmount())
        assertEquals("$29.99", request.amount.formattedWithSymbol())
        assertEquals("Payment SDK Demo Store", request.merchantName)
        assertEquals("Pro Developer License (1 Year)", request.description)
        assertTrue(request.allowedPaymentMethods.contains(PaymentMethodType.GOOGLE_PAY))
        assertTrue(request.allowedPaymentMethods.contains(PaymentMethodType.CARD))
        assertEquals(config, request.googlePayConfig)
    }

    @Test
    fun testDemoPaymentRequestRequiresBillingAddress() {
        val config = MainActivity.createDemoGooglePayConfig()
        val request = MainActivity.createDemoPaymentRequest(config)

        assertTrue(request.requireBillingAddress)
    }

    @Test
    fun testDemoPaymentRequestAmountFormatting() {
        val config = MainActivity.createDemoGooglePayConfig()
        val request = MainActivity.createDemoPaymentRequest(config)

        assertEquals("29.99", request.amount.formattedAmount())
        assertEquals("$29.99", request.amount.formattedWithSymbol())
    }

    @Test
    fun testDemoPaymentRequestHasBothPaymentMethods() {
        val config = MainActivity.createDemoGooglePayConfig()
        val request = MainActivity.createDemoPaymentRequest(config)

        assertEquals(2, request.allowedPaymentMethods.size)
        assertTrue(request.allowedPaymentMethods.contains(PaymentMethodType.GOOGLE_PAY))
        assertTrue(request.allowedPaymentMethods.contains(PaymentMethodType.CARD))
    }

    @Test
    fun testCheckoutViewModelWithDemoSetup() {
        val config = MainActivity.createDemoGooglePayConfig()
        val request = MainActivity.createDemoPaymentRequest(config)
        val viewModel = CheckoutViewModel(request)

        assertNotNull(viewModel.uiState.value)
        assertEquals(request, viewModel.uiState.value.request)

        // Handle success result
        val success = PaymentResult.Success(
            transactionId = "demo_tx_999",
            paymentMethodType = PaymentMethodType.GOOGLE_PAY,
            token = "sample_encrypted_payload_12345",
            last4 = "4242",
            cardNetwork = CardNetwork.VISA
        )
        viewModel.handlePaymentResult(success)

        val state = viewModel.uiState.value
        assertTrue(state is CheckoutUiState.Success)
        val successState = state as CheckoutUiState.Success
        assertEquals("demo_tx_999", successState.result.transactionId)
        assertEquals("4242", successState.result.last4)
        assertEquals(CardNetwork.VISA, successState.result.cardNetwork)

        // Reset
        viewModel.reset()
        assertTrue(viewModel.uiState.value is CheckoutUiState.Ready)
    }

    @Test
    fun testCheckoutViewModelWithCanceledResult() {
        val config = MainActivity.createDemoGooglePayConfig()
        val request = MainActivity.createDemoPaymentRequest(config)
        val viewModel = CheckoutViewModel(request)

        viewModel.handlePaymentResult(PaymentResult.Canceled)

        val state = viewModel.uiState.value
        assertTrue(state is CheckoutUiState.Canceled)
        assertEquals(request, (state as CheckoutUiState.Canceled).request)
    }

    @Test
    fun testCheckoutViewModelWithFailureResult() {
        val config = MainActivity.createDemoGooglePayConfig()
        val request = MainActivity.createDemoPaymentRequest(config)
        val viewModel = CheckoutViewModel(request)

        val failure = PaymentResult.Failure(
            errorCode = PaymentErrorCode.NETWORK_ERROR,
            message = "Network timeout"
        )
        viewModel.handlePaymentResult(failure)

        val state = viewModel.uiState.value
        assertTrue(state is CheckoutUiState.Failure)
        val failureState = state as CheckoutUiState.Failure
        assertEquals(PaymentErrorCode.NETWORK_ERROR, failureState.failure.errorCode)
        assertEquals("Network timeout", failureState.failure.message)
    }

    @Test
    fun testCheckoutViewModelFailureThenReset() {
        val config = MainActivity.createDemoGooglePayConfig()
        val request = MainActivity.createDemoPaymentRequest(config)
        val viewModel = CheckoutViewModel(request)

        viewModel.handlePaymentResult(
            PaymentResult.Failure(
                errorCode = PaymentErrorCode.CARD_DECLINED,
                message = "Declined"
            )
        )
        assertTrue(viewModel.uiState.value is CheckoutUiState.Failure)

        viewModel.reset()
        val resetState = viewModel.uiState.value
        assertTrue(resetState is CheckoutUiState.Ready)
        assertEquals(request, resetState.request)
    }

    @Test
    fun testCheckoutViewModelCanceledThenReset() {
        val config = MainActivity.createDemoGooglePayConfig()
        val request = MainActivity.createDemoPaymentRequest(config)
        val viewModel = CheckoutViewModel(request)

        viewModel.handlePaymentResult(PaymentResult.Canceled)
        assertTrue(viewModel.uiState.value is CheckoutUiState.Canceled)

        viewModel.reset()
        val resetState = viewModel.uiState.value
        assertTrue(resetState is CheckoutUiState.Ready)
        assertEquals(request, resetState.request)
    }

    @Test
    fun testCheckoutViewModelSuccessWithTokenAndLast4() {
        val config = MainActivity.createDemoGooglePayConfig()
        val request = MainActivity.createDemoPaymentRequest(config)
        val viewModel = CheckoutViewModel(request)

        val success = PaymentResult.Success(
            transactionId = "tx_full_details",
            paymentMethodType = PaymentMethodType.GOOGLE_PAY,
            token = "tok_encrypted_payload_full",
            last4 = "1234",
            cardNetwork = CardNetwork.MASTERCARD
        )
        viewModel.handlePaymentResult(success)

        val state = viewModel.uiState.value as CheckoutUiState.Success
        assertEquals("tx_full_details", state.result.transactionId)
        assertEquals("tok_encrypted_payload_full", state.result.token)
        assertEquals("1234", state.result.last4)
        assertEquals(CardNetwork.MASTERCARD, state.result.cardNetwork)
    }

    @Test
    fun testCheckoutViewModelSuccessWithoutOptionalFields() {
        val config = MainActivity.createDemoGooglePayConfig()
        val request = MainActivity.createDemoPaymentRequest(config)
        val viewModel = CheckoutViewModel(request)

        val success = PaymentResult.Success(
            transactionId = "tx_minimal",
            paymentMethodType = PaymentMethodType.GOOGLE_PAY
        )
        viewModel.handlePaymentResult(success)

        val state = viewModel.uiState.value as CheckoutUiState.Success
        assertEquals("tx_minimal", state.result.transactionId)
        assertNull(state.result.token)
        assertNull(state.result.last4)
        assertNull(state.result.cardNetwork)
    }

    @Test
    fun testCheckoutViewModelResetPreservesRequest() {
        val config = MainActivity.createDemoGooglePayConfig()
        val request = MainActivity.createDemoPaymentRequest(config)
        val viewModel = CheckoutViewModel(request)

        viewModel.handlePaymentResult(PaymentResult.Canceled)
        viewModel.reset()

        val state = viewModel.uiState.value as CheckoutUiState.Ready
        assertEquals(request.id, state.request.id)
        assertEquals(request.amount, state.request.amount)
        assertEquals(request.merchantName, state.request.merchantName)
        assertEquals(request.description, state.request.description)
        assertEquals(request.googlePayConfig, state.request.googlePayConfig)
    }
}
