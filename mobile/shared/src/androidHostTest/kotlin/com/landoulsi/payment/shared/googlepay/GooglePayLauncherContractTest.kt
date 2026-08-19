package com.landoulsi.payment.shared.googlepay

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.wallet.AutoResolveHelper
import com.google.android.gms.wallet.IsReadyToPayRequest
import com.google.android.gms.wallet.PaymentData
import com.google.android.gms.wallet.PaymentDataRequest
import com.google.android.gms.wallet.PaymentsClient
import com.landoulsi.payment.shared.model.CardNetwork
import com.landoulsi.payment.shared.model.Currency
import com.landoulsi.payment.shared.model.GooglePayConfig
import com.landoulsi.payment.shared.model.GooglePayEnvironment
import com.landoulsi.payment.shared.model.GooglePayTokenizationSpecification
import com.landoulsi.payment.shared.model.Money
import com.landoulsi.payment.shared.model.PaymentErrorCode
import com.landoulsi.payment.shared.model.PaymentMethodType
import com.landoulsi.payment.shared.model.PaymentRequest
import com.landoulsi.payment.shared.model.PaymentResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GooglePayLauncherContractTest {

    private val testConfig = GooglePayConfig(
        environment = GooglePayEnvironment.TEST,
        merchantId = "test-merchant",
        merchantName = "Test Merchant",
        tokenizationSpecification = GooglePayTokenizationSpecification.Gateway.stripe("pk_test_123")
    )

    private val testPaymentRequest = PaymentRequest(
        id = "tx_123",
        amount = Money.fromMajorUnits(19.99, Currency.USD),
        merchantName = "Test Merchant",
        googlePayConfig = testConfig
    )

    private class MockGooglePayClient(
        var intentResultToReturn: PaymentResult = PaymentResult.Canceled
    ) : GooglePayClient {
        override val paymentsClient: PaymentsClient
            get() = throw UnsupportedOperationException("PaymentsClient not mocked")

        override suspend fun isReadyToPay(config: GooglePayConfig): Boolean = true
        override suspend fun isReadyToPay(request: IsReadyToPayRequest): Boolean = true
        override fun createIsReadyToPayRequest(config: GooglePayConfig): IsReadyToPayRequest =
            IsReadyToPayRequest.fromJson("{}")
        override fun createPaymentDataRequest(request: PaymentRequest): PaymentDataRequest =
            PaymentDataRequest.fromJson(GooglePayJsonFactory.createPaymentDataRequest(request).toString())
        override fun createPaymentDataRequestJson(request: PaymentRequest): String = "{}"
        override suspend fun loadPaymentData(request: PaymentRequest): PaymentData =
            throw UnsupportedOperationException()
        override suspend fun loadPaymentData(request: PaymentDataRequest): PaymentData =
            throw UnsupportedOperationException()
        override fun parsePaymentResult(paymentData: PaymentData, transactionId: String): PaymentResult =
            intentResultToReturn
        override fun parsePaymentResult(paymentDataJson: String, transactionId: String): PaymentResult =
            intentResultToReturn
        override fun parsePaymentResultFromIntent(
            resultCode: Int,
            data: Intent?,
            transactionId: String
        ): PaymentResult = intentResultToReturn
    }

    @Test
    fun testIntentContractCanceled() {
        val contract = GooglePayIntentResultContract(defaultTransactionId = "tx_cancel")
        val result = contract.parseResult(Activity.RESULT_CANCELED, null)
        assertEquals(PaymentResult.Canceled, result)
    }

    @Test
    fun testIntentContractWithClient() {
        val mockClient = MockGooglePayClient(
            intentResultToReturn = PaymentResult.Success(
                transactionId = "tx_client_success",
                paymentMethodType = PaymentMethodType.GOOGLE_PAY,
                token = "tok_abc",
                last4 = "4242",
                cardNetwork = CardNetwork.VISA
            )
        )
        val contract = GooglePayIntentResultContract(client = mockClient, defaultTransactionId = "tx_client_success")
        val result = contract.parseResult(Activity.RESULT_OK, null)

        assertTrue(result is PaymentResult.Success)
        assertEquals("tx_client_success", result.transactionId)
        assertEquals("tok_abc", result.token)
        assertEquals(PaymentMethodType.GOOGLE_PAY, result.paymentMethodType)
    }

    @Test
    fun testIntentContractErrorFallback() {
        val contract = GooglePayIntentResultContract(defaultTransactionId = "tx_err")
        val result = contract.parseResult(AutoResolveHelper.RESULT_ERROR, null)

        assertTrue(result is PaymentResult.Failure)
        assertEquals(PaymentErrorCode.GATEWAY_ERROR, result.errorCode)
    }

    @Test
    fun testIntentContractNullIntentOkFallback() {
        val contract = GooglePayIntentResultContract(defaultTransactionId = "tx_null_intent")
        val result = contract.parseResult(Activity.RESULT_OK, null)

        assertTrue(result is PaymentResult.Failure)
        assertEquals(PaymentErrorCode.UNKNOWN, result.errorCode)
    }

    @Test
    fun testIntentContractResultErrorWithEmptyIntent() {
        val contract = GooglePayIntentResultContract(defaultTransactionId = "tx_empty_intent")
        val result = contract.parseResult(AutoResolveHelper.RESULT_ERROR, Intent())

        assertTrue(result is PaymentResult.Failure)
        assertEquals(PaymentErrorCode.GATEWAY_ERROR, result.errorCode)
    }

    @Test
    fun testIntentContractResultErrorWithNullStatusFallsBackToDefault() {
        val contract = GooglePayIntentResultContract(defaultTransactionId = "tx_null_status")
        val result = contract.parseResult(AutoResolveHelper.RESULT_ERROR, null)

        assertTrue(result is PaymentResult.Failure)
        assertEquals(PaymentErrorCode.GATEWAY_ERROR, result.errorCode)
    }

    @Test
    fun testIntentContractResultErrorWithIntentContainingData() {
        val intent = Intent().apply {
            putExtra("some_key", "some_value")
        }
        val contract = GooglePayIntentResultContract(defaultTransactionId = "tx_extra_data")
        val result = contract.parseResult(AutoResolveHelper.RESULT_ERROR, intent)

        assertTrue(result is PaymentResult.Failure)
        assertEquals(PaymentErrorCode.GATEWAY_ERROR, result.errorCode)
    }

    @Test
    fun testIntentContractClientReceivesTransactionId() {
        val mockClient = MockGooglePayClient(
            intentResultToReturn = PaymentResult.Success(
                transactionId = "tx_from_client",
                paymentMethodType = PaymentMethodType.GOOGLE_PAY,
                token = "tok_xyz"
            )
        )
        val contract = GooglePayIntentResultContract(client = mockClient, defaultTransactionId = "tx_default")
        val result = contract.parseResult(Activity.RESULT_OK, null)

        assertTrue(result is PaymentResult.Success)
        assertEquals("tx_from_client", result.transactionId)
    }

    @Test
    fun testIntentContractClientCanceled() {
        val mockClient = MockGooglePayClient(intentResultToReturn = PaymentResult.Canceled)
        val contract = GooglePayIntentResultContract(client = mockClient, defaultTransactionId = "tx_cl_cancel")
        val result = contract.parseResult(Activity.RESULT_CANCELED, null)

        assertEquals(PaymentResult.Canceled, result)
    }

    @Test
    fun testIntentContractClientFailure() {
        val mockClient = MockGooglePayClient(
            intentResultToReturn = PaymentResult.Failure(
                errorCode = PaymentErrorCode.NETWORK_ERROR,
                message = "Client network error"
            )
        )
        val contract = GooglePayIntentResultContract(client = mockClient, defaultTransactionId = "tx_cl_fail")
        val result = contract.parseResult(AutoResolveHelper.RESULT_ERROR, null)

        assertTrue(result is PaymentResult.Failure)
        assertEquals(PaymentErrorCode.NETWORK_ERROR, result.errorCode)
        assertEquals("Client network error", result.message)
    }

    @Test
    fun testIntentContractClientWithUnknownResultCode() {
        val mockClient = MockGooglePayClient(intentResultToReturn = PaymentResult.Canceled)
        val contract = GooglePayIntentResultContract(client = mockClient, defaultTransactionId = "tx_cl_unk")
        val result = contract.parseResult(999, null)

        assertTrue(result is PaymentResult.Canceled)
    }

    @Test
    fun testIntentContractUnknownResultCode() {
        val contract = GooglePayIntentResultContract(defaultTransactionId = "tx_unknown_rc")
        val result = contract.parseResult(42, null)

        assertTrue(result is PaymentResult.Failure)
        assertEquals(PaymentErrorCode.UNKNOWN, result.errorCode)
        assertTrue(result.message.contains("42"))
    }

    @Test
    fun testIntentContractClientDelegatesAllResultCodes() {
        val mockClient = MockGooglePayClient(
            intentResultToReturn = PaymentResult.Canceled
        )
        val contract = GooglePayIntentResultContract(client = mockClient, defaultTransactionId = "tx_delegate")

        val canceled = contract.parseResult(Activity.RESULT_CANCELED, null)
        assertTrue(canceled is PaymentResult.Canceled)

        val okResult = contract.parseResult(Activity.RESULT_OK, null)
        assertTrue(okResult is PaymentResult.Canceled)

        val errorResult = contract.parseResult(AutoResolveHelper.RESULT_ERROR, null)
        assertTrue(errorResult is PaymentResult.Canceled)

        val unknownResult = contract.parseResult(99, null)
        assertTrue(unknownResult is PaymentResult.Canceled)
    }

    @Test
    fun testIntentContractEmptyTransactionIdFallsBackToDefault() {
        val contract = GooglePayIntentResultContract(defaultTransactionId = "tx_default_fallback")
        val result = contract.parseResult(Activity.RESULT_OK, null)

        assertTrue(result is PaymentResult.Failure)
        assertEquals(PaymentErrorCode.UNKNOWN, result.errorCode)
    }

    @Test
    fun testIntentContractInputDefaultTransactionId() {
        val input = GooglePayIntentResultInput(
            intent = Intent(),
            transactionId = ""
        )
        assertEquals("", input.transactionId)
    }

    @Test
    fun testPaymentTaskContractCancellation() {
        val contract = GooglePayPaymentTaskContract(defaultTransactionId = "tx_task_cancel")
        val result = contract.parseResult(Activity.RESULT_CANCELED, null)
        assertEquals(PaymentResult.Canceled, result)
    }

    @Test
    fun testPaymentTaskContractErrorFallback() {
        val contract = GooglePayPaymentTaskContract(defaultTransactionId = "tx_task_err")
        val result = contract.parseResult(AutoResolveHelper.RESULT_ERROR, null)

        assertTrue(result is PaymentResult.Failure)
    }

    @Test
    fun testPaymentTaskContractResultCanceledWithNullIntent() {
        val contract = GooglePayPaymentTaskContract(defaultTransactionId = "tx_task_cancel_null")
        val result = contract.parseResult(Activity.RESULT_CANCELED, null)
        assertEquals(PaymentResult.Canceled, result)
    }

    @Test
    fun testPaymentTaskContractResultOkWithNullIntent() {
        val contract = GooglePayPaymentTaskContract(defaultTransactionId = "tx_task_ok_null")
        val result = contract.parseResult(Activity.RESULT_OK, null)

        assertTrue(result is PaymentResult.Failure)
    }

    @Test
    fun testPaymentTaskInputEncapsulation() {
        val task = com.google.android.gms.tasks.Tasks.forCanceled<PaymentData>()
        val input = GooglePayPaymentTaskInput(
            task = task,
            transactionId = "tx_dynamic_999"
        )
        assertEquals("tx_dynamic_999", input.transactionId)
        assertEquals(task, input.task)
    }

    @Test
    fun testPaymentTaskInputDefaultTransactionId() {
        val task = com.google.android.gms.tasks.Tasks.forCanceled<PaymentData>()
        val input = GooglePayPaymentTaskInput(task = task)
        assertEquals("", input.transactionId)
    }

    @Test
    fun testPaymentTaskInputEmptyTransactionId() {
        val task = com.google.android.gms.tasks.Tasks.forCanceled<PaymentData>()
        val input = GooglePayPaymentTaskInput(task = task, transactionId = "")
        assertEquals("", input.transactionId)
    }

    @Test
    fun testIntentResultInputEncapsulation() {
        val dummyIntent = Intent()
        val input = GooglePayIntentResultInput(
            intent = dummyIntent,
            transactionId = "tx_intent_456"
        )
        assertEquals("tx_intent_456", input.transactionId)
        assertEquals(dummyIntent, input.intent)
    }

    @Test
    fun testIntentResultInputDefaultTransactionId() {
        val input = GooglePayIntentResultInput(intent = Intent())
        assertEquals("", input.transactionId)
    }

    @Test
    fun testIntentResultInputCopyWithDifferentTransactionId() {
        val original = GooglePayIntentResultInput(
            intent = Intent(),
            transactionId = "tx_orig"
        )
        val copied = original.copy(transactionId = "tx_copied")
        assertEquals("tx_copied", copied.transactionId)
        assertEquals(original.intent, copied.intent)
    }

    @Test
    fun testPaymentTaskInputCopyWithDifferentTask() {
        val task1 = com.google.android.gms.tasks.Tasks.forCanceled<PaymentData>()
        val task2 = com.google.android.gms.tasks.Tasks.forCanceled<PaymentData>()
        val original = GooglePayPaymentTaskInput(task = task1, transactionId = "tx_1")
        val copied = original.copy(task = task2, transactionId = "tx_2")
        assertEquals(task2, copied.task)
        assertEquals("tx_2", copied.transactionId)
    }
}
