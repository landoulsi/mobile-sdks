package com.landoulsi.payment.shared.checkout

import com.landoulsi.payment.shared.model.CardNetwork
import com.landoulsi.payment.shared.model.Currency
import com.landoulsi.payment.shared.model.Money
import com.landoulsi.payment.shared.model.PaymentErrorCode
import com.landoulsi.payment.shared.model.PaymentMethodType
import com.landoulsi.payment.shared.model.PaymentRequest
import com.landoulsi.payment.shared.model.PaymentResult
import com.landoulsi.payment.shared.provider.PaymentProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CheckoutViewModelTest {

    private class FakePaymentProvider(
        override val paymentMethodType: PaymentMethodType,
        var isReady: Boolean = true,
        var resultToReturn: PaymentResult = PaymentResult.Success(
            transactionId = "tx_123",
            paymentMethodType = paymentMethodType,
            token = "fake_token",
            last4 = "4242",
            cardNetwork = CardNetwork.VISA
        )
    ) : PaymentProvider {
        var isReadyCheckedCount = 0
        var payCallCount = 0
        var lastPaymentRequest: PaymentRequest? = null

        override suspend fun isReadyToPay(): Boolean {
            isReadyCheckedCount++
            return isReady
        }

        override suspend fun pay(request: PaymentRequest): PaymentResult {
            payCallCount++
            lastPaymentRequest = request
            return resultToReturn
        }
    }

    private fun createSampleRequest(
        allowedMethods: List<PaymentMethodType> = listOf(PaymentMethodType.GOOGLE_PAY, PaymentMethodType.CARD)
    ) = PaymentRequest(
        id = "req_test_101",
        amount = Money.fromMajorUnits(49.99, Currency.USD),
        merchantName = "Test Store",
        description = "Test Order",
        allowedPaymentMethods = allowedMethods
    )

    @Test
    fun testInitializationChecksAvailabilityAndTransitionsToReady() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)

        val gpayProvider = FakePaymentProvider(PaymentMethodType.GOOGLE_PAY, isReady = true)
        val request = createSampleRequest()

        val viewModel = CheckoutViewModel(
            initialRequest = request,
            providers = listOf(gpayProvider),
            coroutineScope = testScope
        )

        testScope.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertIs<CheckoutUiState.Ready>(state)
        assertTrue(state.isGooglePayAvailable)
        assertFalse(state.isApplePayAvailable)
        assertTrue(state.availablePaymentMethods.contains(PaymentMethodType.GOOGLE_PAY))
        assertTrue(state.availablePaymentMethods.contains(PaymentMethodType.CARD))
        assertEquals(1, gpayProvider.isReadyCheckedCount)
        assertTrue(viewModel.isPaymentMethodAvailable(PaymentMethodType.GOOGLE_PAY))
    }

    @Test
    fun testInitializationWhenGooglePayUnavailable() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)

        val gpayProvider = FakePaymentProvider(PaymentMethodType.GOOGLE_PAY, isReady = false)
        val request = createSampleRequest()

        val viewModel = CheckoutViewModel(
            initialRequest = request,
            providers = listOf(gpayProvider),
            coroutineScope = testScope
        )

        testScope.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertIs<CheckoutUiState.Ready>(state)
        assertFalse(state.isGooglePayAvailable)
        assertFalse(state.availablePaymentMethods.contains(PaymentMethodType.GOOGLE_PAY))
        assertTrue(state.availablePaymentMethods.contains(PaymentMethodType.CARD))
        assertFalse(viewModel.isPaymentMethodAvailable(PaymentMethodType.GOOGLE_PAY))
    }

    @Test
    fun testPaySuccessFlow() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)

        val successResult = PaymentResult.Success(
            transactionId = "tx_success_777",
            paymentMethodType = PaymentMethodType.GOOGLE_PAY,
            token = "tok_gpay_encrypted",
            last4 = "1111",
            cardNetwork = CardNetwork.MASTERCARD
        )
        val gpayProvider = FakePaymentProvider(PaymentMethodType.GOOGLE_PAY, resultToReturn = successResult)
        val request = createSampleRequest()

        val viewModel = CheckoutViewModel(
            initialRequest = request,
            providers = listOf(gpayProvider),
            coroutineScope = testScope
        )
        testScope.advanceUntilIdle()

        viewModel.pay(PaymentMethodType.GOOGLE_PAY)

        testScope.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertIs<CheckoutUiState.Success>(state)
        assertEquals("tx_success_777", state.result.transactionId)
        assertEquals(PaymentMethodType.GOOGLE_PAY, state.result.paymentMethodType)
        assertEquals("1111", state.result.last4)
        assertEquals(CardNetwork.MASTERCARD, state.result.cardNetwork)
        assertEquals(1, gpayProvider.payCallCount)
    }

    @Test
    fun testPayFailureFlow() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)

        val failureResult = PaymentResult.Failure(
            errorCode = PaymentErrorCode.CARD_DECLINED,
            message = "Card declined by issuer"
        )
        val gpayProvider = FakePaymentProvider(PaymentMethodType.GOOGLE_PAY, resultToReturn = failureResult)
        val request = createSampleRequest()

        val viewModel = CheckoutViewModel(
            initialRequest = request,
            providers = listOf(gpayProvider),
            coroutineScope = testScope
        )
        testScope.advanceUntilIdle()

        viewModel.pay(PaymentMethodType.GOOGLE_PAY)
        testScope.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertIs<CheckoutUiState.Failure>(state)
        assertEquals(PaymentErrorCode.CARD_DECLINED, state.failure.errorCode)
        assertEquals("Card declined by issuer", state.failure.message)
    }

    @Test
    fun testHandlePaymentResultFromContract() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)

        val request = createSampleRequest()
        val viewModel = CheckoutViewModel(
            initialRequest = request,
            providers = emptyList(),
            coroutineScope = testScope
        )
        testScope.advanceUntilIdle()

        viewModel.startProcessing(PaymentMethodType.GOOGLE_PAY)
        assertIs<CheckoutUiState.Processing>(viewModel.uiState.value)

        // Simulate Contract Success
        val success = PaymentResult.Success(
            transactionId = "tx_contract_456",
            paymentMethodType = PaymentMethodType.GOOGLE_PAY,
            token = "contract_token",
            last4 = "9999"
        )
        viewModel.handlePaymentResult(success)

        val successState = viewModel.uiState.value
        assertIs<CheckoutUiState.Success>(successState)
        assertEquals("tx_contract_456", successState.result.transactionId)

        // Reset
        viewModel.reset()
        assertIs<CheckoutUiState.Ready>(viewModel.uiState.value)

        // Simulate Contract Canceled
        viewModel.handlePaymentResult(PaymentResult.Canceled)
        assertIs<CheckoutUiState.Canceled>(viewModel.uiState.value)

        // Reset
        viewModel.reset()
        assertIs<CheckoutUiState.Ready>(viewModel.uiState.value)

        // Simulate Contract Failure
        val failure = PaymentResult.Failure(
            errorCode = PaymentErrorCode.NETWORK_ERROR,
            message = "Connection timeout"
        )
        viewModel.handlePaymentResult(failure)
        assertIs<CheckoutUiState.Failure>(viewModel.uiState.value)
    }

    @Test
    fun testPayWithoutRegisteredProviderReturnsFailure() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)

        val request = createSampleRequest()
        val viewModel = CheckoutViewModel(
            initialRequest = request,
            providers = emptyList(),
            coroutineScope = testScope
        )
        testScope.advanceUntilIdle()

        viewModel.pay(PaymentMethodType.PAYPAL)

        val state = viewModel.uiState.value
        assertIs<CheckoutUiState.Failure>(state)
        assertEquals(PaymentErrorCode.PAYMENT_METHOD_UNAVAILABLE, state.failure.errorCode)
    }

    @Test
    fun testUpdatePaymentRequest() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)

        val gpayProvider = FakePaymentProvider(PaymentMethodType.GOOGLE_PAY, isReady = true)
        val initialRequest = createSampleRequest()

        val viewModel = CheckoutViewModel(
            initialRequest = initialRequest,
            providers = listOf(gpayProvider),
            coroutineScope = testScope
        )
        testScope.advanceUntilIdle()

        val updatedRequest = PaymentRequest(
            id = "req_updated_999",
            amount = Money.fromMajorUnits(100.0, Currency.EUR),
            merchantName = "Updated Merchant"
        )

        viewModel.updatePaymentRequest(updatedRequest)
        testScope.advanceUntilIdle()

        assertEquals("req_updated_999", viewModel.uiState.value.request.id)
        assertEquals(Money.fromMajorUnits(100.0, Currency.EUR), viewModel.uiState.value.request.amount)
        assertNotNull(viewModel.getProvider(PaymentMethodType.GOOGLE_PAY))
        assertNull(viewModel.getProvider(PaymentMethodType.APPLE_PAY))
    }

    @Test
    fun testUiStateExtensionProperties() {
        val request = createSampleRequest()

        val initial: CheckoutUiState = CheckoutUiState.Initial(request)
        assertFalse(initial.isLoading)
        assertFalse(initial.isGooglePayReady)

        val checking: CheckoutUiState = CheckoutUiState.CheckingAvailability(request)
        assertTrue(checking.isLoading)
        assertFalse(checking.isGooglePayReady)

        val ready: CheckoutUiState = CheckoutUiState.Ready(request, isGooglePayAvailable = true)
        assertFalse(ready.isLoading)
        assertTrue(ready.isGooglePayReady)

        val processing: CheckoutUiState = CheckoutUiState.Processing(request, PaymentMethodType.GOOGLE_PAY)
        assertTrue(processing.isLoading)
        assertFalse(processing.isGooglePayReady)
    }

    @Test
    fun testUiStateExtensionPropertiesForSuccessFailureCanceled() {
        val request = createSampleRequest()

        val success: CheckoutUiState = CheckoutUiState.Success(
            request = request,
            result = PaymentResult.Success(
                transactionId = "tx_ext",
                paymentMethodType = PaymentMethodType.GOOGLE_PAY
            )
        )
        assertFalse(success.isLoading)
        assertFalse(success.isGooglePayReady)

        val failure: CheckoutUiState = CheckoutUiState.Failure(
            request = request,
            failure = PaymentResult.Failure(
                errorCode = PaymentErrorCode.NETWORK_ERROR,
                message = "timeout"
            )
        )
        assertFalse(failure.isLoading)
        assertFalse(failure.isGooglePayReady)

        val canceled: CheckoutUiState = CheckoutUiState.Canceled(request)
        assertFalse(canceled.isLoading)
        assertFalse(canceled.isGooglePayReady)
    }

    @Test
    fun testResetPreservesCachedGooglePayUnavailable() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)

        val gpayProvider = FakePaymentProvider(PaymentMethodType.GOOGLE_PAY, isReady = false)
        val request = createSampleRequest()

        val viewModel = CheckoutViewModel(
            initialRequest = request,
            providers = listOf(gpayProvider),
            coroutineScope = testScope
        )
        testScope.advanceUntilIdle()

        val readyState = viewModel.uiState.value as CheckoutUiState.Ready
        assertFalse(readyState.isGooglePayAvailable)
        assertFalse(readyState.availablePaymentMethods.contains(PaymentMethodType.GOOGLE_PAY))

        viewModel.reset()
        val resetState = viewModel.uiState.value as CheckoutUiState.Ready
        assertFalse(resetState.isGooglePayAvailable)
        assertFalse(resetState.availablePaymentMethods.contains(PaymentMethodType.GOOGLE_PAY))
        assertTrue(resetState.availablePaymentMethods.contains(PaymentMethodType.CARD))
        assertEquals(request, resetState.request)
    }

    @Test
    fun testResetPreservesCachedGooglePayAvailable() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)

        val gpayProvider = FakePaymentProvider(PaymentMethodType.GOOGLE_PAY, isReady = true)
        val request = createSampleRequest()

        val viewModel = CheckoutViewModel(
            initialRequest = request,
            providers = listOf(gpayProvider),
            coroutineScope = testScope
        )
        testScope.advanceUntilIdle()

        viewModel.startProcessing(PaymentMethodType.GOOGLE_PAY)
        assertIs<CheckoutUiState.Processing>(viewModel.uiState.value)

        viewModel.handlePaymentResult(PaymentResult.Canceled)
        assertIs<CheckoutUiState.Canceled>(viewModel.uiState.value)

        viewModel.reset()
        val resetState = viewModel.uiState.value as CheckoutUiState.Ready
        assertTrue(resetState.isGooglePayAvailable)
        assertTrue(resetState.availablePaymentMethods.contains(PaymentMethodType.GOOGLE_PAY))
        assertTrue(resetState.availablePaymentMethods.contains(PaymentMethodType.CARD))
    }

    @Test
    fun testIsPaymentMethodAvailableForUnregisteredMethod() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)

        val gpayProvider = FakePaymentProvider(PaymentMethodType.GOOGLE_PAY, isReady = true)
        val request = createSampleRequest()

        val viewModel = CheckoutViewModel(
            initialRequest = request,
            providers = listOf(gpayProvider),
            coroutineScope = testScope
        )
        testScope.advanceUntilIdle()

        assertTrue(viewModel.isPaymentMethodAvailable(PaymentMethodType.GOOGLE_PAY))
        assertTrue(viewModel.isPaymentMethodAvailable(PaymentMethodType.CARD))
        assertFalse(viewModel.isPaymentMethodAvailable(PaymentMethodType.APPLE_PAY))
        assertFalse(viewModel.isPaymentMethodAvailable(PaymentMethodType.PAYPAL))
        assertFalse(viewModel.isPaymentMethodAvailable(PaymentMethodType.KLARNA))
    }

    @Test
    fun testGetProviderReturnsCorrectProvider() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)

        val gpayProvider = FakePaymentProvider(PaymentMethodType.GOOGLE_PAY, isReady = true)
        val request = createSampleRequest()

        val viewModel = CheckoutViewModel(
            initialRequest = request,
            providers = listOf(gpayProvider),
            coroutineScope = testScope
        )
        testScope.advanceUntilIdle()

        assertEquals(gpayProvider, viewModel.getProvider(PaymentMethodType.GOOGLE_PAY))
        assertNull(viewModel.getProvider(PaymentMethodType.APPLE_PAY))
        assertNull(viewModel.getProvider(PaymentMethodType.CARD))
        assertNull(viewModel.getProvider(PaymentMethodType.PAYPAL))
    }

    @Test
    fun testCheckAvailabilityWhenProviderThrows() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)

        val throwingProvider = object : PaymentProvider {
            override val paymentMethodType = PaymentMethodType.GOOGLE_PAY
            override suspend fun isReadyToPay(): Boolean = throw RuntimeException("Play Services error")
            override suspend fun pay(request: PaymentRequest): PaymentResult = TODO()
        }

        val request = createSampleRequest()
        val viewModel = CheckoutViewModel(
            initialRequest = request,
            providers = listOf(throwingProvider),
            coroutineScope = testScope
        )
        testScope.advanceUntilIdle()

        val state = viewModel.uiState.value as CheckoutUiState.Ready
        assertFalse(state.isGooglePayAvailable)
        assertFalse(state.availablePaymentMethods.contains(PaymentMethodType.GOOGLE_PAY))
        assertTrue(state.availablePaymentMethods.contains(PaymentMethodType.CARD))
    }

    @Test
    fun testUpdatePaymentRequestWithoutRefreshAvailability() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)

        val gpayProvider = FakePaymentProvider(PaymentMethodType.GOOGLE_PAY, isReady = true)
        val initialRequest = createSampleRequest()

        val viewModel = CheckoutViewModel(
            initialRequest = initialRequest,
            providers = listOf(gpayProvider),
            coroutineScope = testScope
        )
        testScope.advanceUntilIdle()

        assertEquals(1, gpayProvider.isReadyCheckedCount)

        val updatedRequest = PaymentRequest(
            id = "req_no_refresh",
            amount = Money.fromMajorUnits(5.0, Currency.USD),
            merchantName = "No Refresh Merchant"
        )

        viewModel.updatePaymentRequest(updatedRequest, refreshAvailability = false)
        testScope.advanceUntilIdle()

        assertEquals(1, gpayProvider.isReadyCheckedCount)
        assertEquals("req_no_refresh", viewModel.uiState.value.request.id)
        assertIs<CheckoutUiState.Ready>(viewModel.uiState.value)
    }

    @Test
    fun testMultipleProvidersAvailability() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)

        val gpayProvider = FakePaymentProvider(PaymentMethodType.GOOGLE_PAY, isReady = true)
        val applePayProvider = FakePaymentProvider(PaymentMethodType.APPLE_PAY, isReady = true)

        val request = PaymentRequest(
            id = "req_multi",
            amount = Money.fromMajorUnits(10.0, Currency.USD),
            allowedPaymentMethods = listOf(
                PaymentMethodType.GOOGLE_PAY,
                PaymentMethodType.APPLE_PAY,
                PaymentMethodType.CARD
            )
        )

        val viewModel = CheckoutViewModel(
            initialRequest = request,
            providers = listOf(gpayProvider, applePayProvider),
            coroutineScope = testScope
        )
        testScope.advanceUntilIdle()

        val state = viewModel.uiState.value as CheckoutUiState.Ready
        assertTrue(state.isGooglePayAvailable)
        assertTrue(state.isApplePayAvailable)
        assertTrue(state.availablePaymentMethods.contains(PaymentMethodType.GOOGLE_PAY))
        assertTrue(state.availablePaymentMethods.contains(PaymentMethodType.APPLE_PAY))
        assertTrue(state.availablePaymentMethods.contains(PaymentMethodType.CARD))
        assertEquals(2, gpayProvider.isReadyCheckedCount + applePayProvider.isReadyCheckedCount)
    }

    @Test
    fun testStartProcessingSetsCorrectState() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)

        val request = createSampleRequest()
        val viewModel = CheckoutViewModel(
            initialRequest = request,
            providers = emptyList(),
            coroutineScope = testScope
        )
        testScope.advanceUntilIdle()

        viewModel.startProcessing(PaymentMethodType.GOOGLE_PAY)
        val state = viewModel.uiState.value
        assertIs<CheckoutUiState.Processing>(state)
        assertEquals(PaymentMethodType.GOOGLE_PAY, state.paymentMethodType)
        assertEquals(request, state.request)
    }

    @Test
    fun testPayWithProviderExceptionResultsInFailure() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)

        val failingProvider = object : PaymentProvider {
            override val paymentMethodType = PaymentMethodType.GOOGLE_PAY
            override suspend fun isReadyToPay(): Boolean = true
            override suspend fun pay(request: PaymentRequest): PaymentResult =
                throw IllegalStateException("SDK crash")
        }

        val request = createSampleRequest()
        val viewModel = CheckoutViewModel(
            initialRequest = request,
            providers = listOf(failingProvider),
            coroutineScope = testScope
        )
        testScope.advanceUntilIdle()

        viewModel.pay(PaymentMethodType.GOOGLE_PAY)
        testScope.advanceUntilIdle()

        val state = viewModel.uiState.value as CheckoutUiState.Failure
        assertEquals(PaymentErrorCode.UNKNOWN, state.failure.errorCode)
        assertEquals("SDK crash", state.failure.message)
    }

    @Test
    fun testPayPassesCurrentRequestToProvider() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)

        val gpayProvider = FakePaymentProvider(PaymentMethodType.GOOGLE_PAY)
        val initialRequest = createSampleRequest()

        val viewModel = CheckoutViewModel(
            initialRequest = initialRequest,
            providers = listOf(gpayProvider),
            coroutineScope = testScope
        )
        testScope.advanceUntilIdle()

        val updatedRequest = PaymentRequest(
            id = "req_new_amount",
            amount = Money.fromMajorUnits(99.50, Currency.EUR),
            allowedPaymentMethods = listOf(PaymentMethodType.GOOGLE_PAY)
        )
        viewModel.updatePaymentRequest(updatedRequest)
        testScope.advanceUntilIdle()

        viewModel.pay(PaymentMethodType.GOOGLE_PAY)
        testScope.advanceUntilIdle()

        assertEquals("req_new_amount", gpayProvider.lastPaymentRequest?.id)
        assertEquals(Money.fromMajorUnits(99.50, Currency.EUR), gpayProvider.lastPaymentRequest?.amount)
    }

    @Test
    fun testCanceledStatePreservesRequest() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)

        val request = createSampleRequest()
        val viewModel = CheckoutViewModel(
            initialRequest = request,
            providers = emptyList(),
            coroutineScope = testScope
        )
        testScope.advanceUntilIdle()

        viewModel.handlePaymentResult(PaymentResult.Canceled)

        val state = viewModel.uiState.value as CheckoutUiState.Canceled
        assertEquals(request, state.request)
        assertEquals("req_test_101", state.request.id)
    }

    @Test
    fun testFailureStatePreservesRequestAndFailureDetails() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)

        val request = createSampleRequest()
        val viewModel = CheckoutViewModel(
            initialRequest = request,
            providers = emptyList(),
            coroutineScope = testScope
        )
        testScope.advanceUntilIdle()

        val failure = PaymentResult.Failure(
            errorCode = PaymentErrorCode.CARD_DECLINED,
            message = "Card was declined",
            cause = RuntimeException("declined")
        )
        viewModel.handlePaymentResult(failure)

        val state = viewModel.uiState.value as CheckoutUiState.Failure
        assertEquals(request, state.request)
        assertEquals(PaymentErrorCode.CARD_DECLINED, state.failure.errorCode)
        assertEquals("Card was declined", state.failure.message)
        assertNotNull(state.failure.cause)
    }

    @Test
    fun testVarargProvidersConstructor() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)

        val gpayProvider = FakePaymentProvider(PaymentMethodType.GOOGLE_PAY, isReady = true)
        val request = createSampleRequest()

        val viewModel = CheckoutViewModel(
            initialRequest = request,
            providers = listOf(gpayProvider),
            coroutineScope = testScope
        )
        testScope.advanceUntilIdle()

        val state = viewModel.uiState.value as CheckoutUiState.Ready
        assertTrue(state.isGooglePayAvailable)
        assertEquals(gpayProvider, viewModel.getProvider(PaymentMethodType.GOOGLE_PAY))
    }

    @Test
    fun testFailureThenResetThenSuccessCycle() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)

        val gpayProvider = FakePaymentProvider(PaymentMethodType.GOOGLE_PAY, isReady = true)
        val request = createSampleRequest()

        val viewModel = CheckoutViewModel(
            initialRequest = request,
            providers = listOf(gpayProvider),
            coroutineScope = testScope
        )
        testScope.advanceUntilIdle()
        assertIs<CheckoutUiState.Ready>(viewModel.uiState.value)

        gpayProvider.resultToReturn = PaymentResult.Failure(
            errorCode = PaymentErrorCode.NETWORK_ERROR,
            message = "Connection lost"
        )
        viewModel.pay(PaymentMethodType.GOOGLE_PAY)
        testScope.advanceUntilIdle()
        assertIs<CheckoutUiState.Failure>(viewModel.uiState.value)

        viewModel.reset()
        assertIs<CheckoutUiState.Ready>(viewModel.uiState.value)

        gpayProvider.resultToReturn = PaymentResult.Success(
            transactionId = "tx_recovery",
            paymentMethodType = PaymentMethodType.GOOGLE_PAY,
            token = "tok_recovery",
            last4 = "0000"
        )
        viewModel.pay(PaymentMethodType.GOOGLE_PAY)
        testScope.advanceUntilIdle()

        val successState = viewModel.uiState.value as CheckoutUiState.Success
        assertEquals("tx_recovery", successState.result.transactionId)
    }
}
