package com.landoulsi.payment.shared.googlepay

import android.app.Activity
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
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
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GooglePayClientAndProviderTest {

    private val testConfig = GooglePayConfig(
        environment = GooglePayEnvironment.TEST,
        merchantId = "test-merchant",
        merchantName = "Test Merchant",
        tokenizationSpecification = GooglePayTokenizationSpecification.Gateway.stripe("pk_test_123")
    )

    private val testPaymentRequest = PaymentRequest(
        id = "tx_12345",
        amount = Money.fromMajorUnits(29.99, Currency.USD),
        merchantName = "Test Merchant",
        googlePayConfig = testConfig
    )

    private class FakeGooglePayClient(
        var isReadyResult: Boolean = true,
        var loadPaymentDataResult: PaymentData? = null,
        var exceptionToThrow: Throwable? = null
    ) : GooglePayClient {

        override val paymentsClient: PaymentsClient
            get() = throw UnsupportedOperationException("PaymentsClient not mocked in fake")

        override suspend fun isReadyToPay(config: GooglePayConfig): Boolean = isReadyResult

        override suspend fun isReadyToPay(request: IsReadyToPayRequest): Boolean = isReadyResult

        override fun createIsReadyToPayRequest(config: GooglePayConfig): IsReadyToPayRequest {
            val json = GooglePayJsonFactory.createIsReadyToPayRequest(config)
            return IsReadyToPayRequest.fromJson(json.toString())
        }

        override fun createPaymentDataRequest(request: PaymentRequest): PaymentDataRequest {
            val json = createPaymentDataRequestJson(request)
            return PaymentDataRequest.fromJson(json)
        }

        override fun createPaymentDataRequestJson(request: PaymentRequest): String {
            return GooglePayJsonFactory.createPaymentDataRequest(request).toString()
        }

        override suspend fun loadPaymentData(request: PaymentRequest): PaymentData {
            exceptionToThrow?.let { throw it }
            return checkNotNull(loadPaymentDataResult)
        }

        override suspend fun loadPaymentData(request: PaymentDataRequest): PaymentData {
            exceptionToThrow?.let { throw it }
            return checkNotNull(loadPaymentDataResult)
        }

        override fun parsePaymentResult(paymentData: PaymentData, transactionId: String): PaymentResult {
            return parsePaymentResult(paymentData.toJson(), transactionId)
        }

        override fun parsePaymentResult(paymentDataJson: String, transactionId: String): PaymentResult {
            return GooglePayJsonFactory.parsePaymentResult(paymentDataJson, transactionId)
        }

        override fun parsePaymentResultFromIntent(
            resultCode: Int,
            data: android.content.Intent?,
            transactionId: String
        ): PaymentResult {
            return when (resultCode) {
                Activity.RESULT_OK -> PaymentResult.Success(
                    transactionId = transactionId,
                    paymentMethodType = PaymentMethodType.GOOGLE_PAY,
                    token = "fake_token",
                    last4 = "1234",
                    cardNetwork = CardNetwork.VISA
                )
                Activity.RESULT_CANCELED -> PaymentResult.Canceled
                else -> PaymentResult.Failure(
                    errorCode = PaymentErrorCode.GATEWAY_ERROR,
                    message = "Google Pay error"
                )
            }
        }
    }

    @Test
    fun testProviderPaymentMethodType() {
        val fakeClient = FakeGooglePayClient()
        val provider = GooglePayProvider(testConfig, fakeClient)
        assertEquals(PaymentMethodType.GOOGLE_PAY, provider.paymentMethodType)
    }

    @Test
    fun testProviderIsReadyToPay() = runTest {
        val fakeClient = FakeGooglePayClient(isReadyResult = true)
        val provider = GooglePayProvider(testConfig, fakeClient)
        assertTrue(provider.isReadyToPay())

        fakeClient.isReadyResult = false
        assertFalse(provider.isReadyToPay())
    }

    @Test
    fun testProviderPaySuccess() = runTest {
        val sampleJson = """
            {
              "apiVersion": 2,
              "apiVersionMinor": 0,
              "paymentMethodData": {
                "description": "Visa •••• 4242",
                "tokenizationData": {
                  "type": "PAYMENT_GATEWAY",
                  "token": "tok_stripe_123"
                },
                "type": "CARD",
                "info": {
                  "cardNetwork": "VISA",
                  "cardDetails": "4242"
                }
              }
            }
        """.trimIndent()

        val paymentData = PaymentData.fromJson(sampleJson)
        val fakeClient = FakeGooglePayClient(loadPaymentDataResult = paymentData)
        val provider = GooglePayProvider(testConfig, fakeClient)

        val result = provider.pay(testPaymentRequest)
        assertTrue(result is PaymentResult.Success)
        assertEquals("tx_12345", result.transactionId)
        assertEquals(PaymentMethodType.GOOGLE_PAY, result.paymentMethodType)
        assertEquals("tok_stripe_123", result.token)
        assertEquals("4242", result.last4)
        assertEquals(CardNetwork.VISA, result.cardNetwork)
    }

    @Test
    fun testProviderPayCanceled() = runTest {
        val fakeClient = FakeGooglePayClient(
            exceptionToThrow = ApiException(Status(CommonStatusCodes.CANCELED, "User canceled"))
        )
        val provider = GooglePayProvider(testConfig, fakeClient)

        val result = provider.pay(testPaymentRequest)
        assertTrue(result is PaymentResult.Canceled)
    }

    @Test
    fun testProviderPayNetworkError() = runTest {
        val fakeClient = FakeGooglePayClient(
            exceptionToThrow = ApiException(Status(CommonStatusCodes.NETWORK_ERROR, "Network failure"))
        )
        val provider = GooglePayProvider(testConfig, fakeClient)

        val result = provider.pay(testPaymentRequest)
        assertTrue(result is PaymentResult.Failure)
        assertEquals(PaymentErrorCode.NETWORK_ERROR, result.errorCode)
    }

    @Test
    fun testProviderPayDeveloperError() = runTest {
        val fakeClient = FakeGooglePayClient(
            exceptionToThrow = ApiException(Status(CommonStatusCodes.DEVELOPER_ERROR, "Invalid config"))
        )
        val provider = GooglePayProvider(testConfig, fakeClient)

        val result = provider.pay(testPaymentRequest)
        assertTrue(result is PaymentResult.Failure)
        assertEquals(PaymentErrorCode.CONFIGURATION_ERROR, result.errorCode)
    }

    @Test
    fun testClientIntentParsing() {
        val fakeClient = FakeGooglePayClient()

        val canceled = fakeClient.parsePaymentResultFromIntent(Activity.RESULT_CANCELED, null, "tx_1")
        assertEquals(PaymentResult.Canceled, canceled)

        val success = fakeClient.parsePaymentResultFromIntent(Activity.RESULT_OK, null, "tx_1")
        assertTrue(success is PaymentResult.Success)
        assertEquals("tx_1", success.transactionId)
    }

    @Test
    fun testIntentParsingCanceled() {
        val fakeClient = FakeGooglePayClient()
        val canceled = fakeClient.parsePaymentResultFromIntent(Activity.RESULT_CANCELED, null, "tx_cancel")
        assertEquals(PaymentResult.Canceled, canceled)
    }

    @Test
    fun testProviderPayWithRequestLevelConfigOverride() = runTest {
        val sampleJson = """
            {
              "paymentMethodData": {
                "description": "Visa •••• 9999",
                "tokenizationData": {
                  "type": "PAYMENT_GATEWAY",
                  "token": "tok_override_config"
                },
                "type": "CARD",
                "info": {
                  "cardNetwork": "VISA",
                  "cardDetails": "9999"
                }
              }
            }
        """.trimIndent()
        val paymentData = PaymentData.fromJson(sampleJson)
        val fakeClient = FakeGooglePayClient(loadPaymentDataResult = paymentData)
        val provider = GooglePayProvider(testConfig, fakeClient)

        val overrideConfig = testConfig.copy(merchantId = "override-merchant")
        val requestWithConfig = testPaymentRequest.copy(googlePayConfig = overrideConfig)

        val result = provider.pay(requestWithConfig)
        assertTrue(result is PaymentResult.Success)
        assertEquals("tok_override_config", result.token)
        assertEquals("9999", result.last4)
    }

    @Test
    fun testProviderPayWithNullConfigFallsBackToProviderConfig() = runTest {
        val sampleJson = """
            {
              "paymentMethodData": {
                "description": "Mastercard •••• 8888",
                "tokenizationData": {
                  "type": "PAYMENT_GATEWAY",
                  "token": "tok_fallback_config"
                },
                "type": "CARD",
                "info": {
                  "cardNetwork": "MASTERCARD",
                  "cardDetails": "8888"
                }
              }
            }
        """.trimIndent()
        val paymentData = PaymentData.fromJson(sampleJson)
        val fakeClient = FakeGooglePayClient(loadPaymentDataResult = paymentData)
        val provider = GooglePayProvider(testConfig, fakeClient)

        val requestNoConfig = testPaymentRequest.copy(googlePayConfig = null)
        val result = provider.pay(requestNoConfig)
        assertTrue(result is PaymentResult.Success)
        assertEquals("tok_fallback_config", result.token)
    }

    @Test
    fun testProviderPayUnexpectedThrowable() = runTest {
        val fakeClient = FakeGooglePayClient(
            exceptionToThrow = IllegalStateException("SDK not initialized")
        )
        val provider = GooglePayProvider(testConfig, fakeClient)

        val result = provider.pay(testPaymentRequest)
        assertTrue(result is PaymentResult.Failure)
        assertEquals(PaymentErrorCode.UNKNOWN, result.errorCode)
        assertEquals("SDK not initialized", result.message)
    }

    @Test
    fun testProviderPayGenericApiError() = runTest {
        val fakeClient = FakeGooglePayClient(
            exceptionToThrow = ApiException(Status(CommonStatusCodes.ERROR, "General error"))
        )
        val provider = GooglePayProvider(testConfig, fakeClient)

        val result = provider.pay(testPaymentRequest)
        assertTrue(result is PaymentResult.Failure)
        assertEquals(PaymentErrorCode.GATEWAY_ERROR, result.errorCode)
    }

    @Test
    fun testClientCreateIsReadyToPayRequest() {
        val fakeClient = FakeGooglePayClient()
        val request = fakeClient.createIsReadyToPayRequest(testConfig)
        assertNotNull(request)
    }

    @Test
    fun testClientCreatePaymentDataRequestJson() {
        val fakeClient = FakeGooglePayClient()
        val json = fakeClient.createPaymentDataRequestJson(testPaymentRequest)
        assertTrue(json.contains("apiVersion"))
        assertTrue(json.contains("transactionInfo"))
    }

    @Test
    fun testClientCreatePaymentDataRequest() {
        val fakeClient = FakeGooglePayClient()
        val request = fakeClient.createPaymentDataRequest(testPaymentRequest)
        assertNotNull(request)
    }

    @Test
    fun testParsePaymentResultFromFakeClient() {
        val fakeClient = FakeGooglePayClient()
        val successResult = fakeClient.parsePaymentResultFromIntent(
            Activity.RESULT_OK,
            null,
            "tx_parse"
        )
        assertTrue(successResult is PaymentResult.Success)
        assertEquals("tx_parse", successResult.transactionId)
        assertEquals("fake_token", successResult.token)
    }

    @Test
    fun testProviderPayWithNullTokenInResponse() = runTest {
        val sampleJson = """
            {
              "paymentMethodData": {
                "description": "Visa •••• 1111",
                "tokenizationData": {
                  "type": "PAYMENT_GATEWAY",
                  "token": ""
                },
                "type": "CARD",
                "info": {
                  "cardNetwork": "VISA",
                  "cardDetails": "1111"
                }
              }
            }
        """.trimIndent()
        val paymentData = PaymentData.fromJson(sampleJson)
        val fakeClient = FakeGooglePayClient(loadPaymentDataResult = paymentData)
        val provider = GooglePayProvider(testConfig, fakeClient)

        val result = provider.pay(testPaymentRequest)
        assertTrue(result is PaymentResult.Success)
        assertNull(result.token)
    }

    @Test
    fun testFakeClientIsReadyToPayOverload() = runTest {
        val fakeClient = FakeGooglePayClient(isReadyResult = false)
        assertFalse(fakeClient.isReadyToPay(testConfig))

        fakeClient.isReadyResult = true
        assertTrue(fakeClient.isReadyToPay(testConfig))
    }
}
