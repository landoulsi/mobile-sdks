package com.landoulsi.payment.shared.applepay

import com.landoulsi.payment.shared.model.Address
import com.landoulsi.payment.shared.model.ApplePayConfig
import com.landoulsi.payment.shared.model.ApplePayContactField
import com.landoulsi.payment.shared.model.ApplePayMerchantCapability
import com.landoulsi.payment.shared.model.ApplePayShippingType
import com.landoulsi.payment.shared.model.ApplePaySummaryItem
import com.landoulsi.payment.shared.model.ApplePaySummaryItemType
import com.landoulsi.payment.shared.model.CardNetwork
import com.landoulsi.payment.shared.model.Currency
import com.landoulsi.payment.shared.model.Money
import com.landoulsi.payment.shared.model.PaymentErrorCode
import com.landoulsi.payment.shared.model.PaymentMethodType
import com.landoulsi.payment.shared.model.PaymentRequest
import com.landoulsi.payment.shared.model.PaymentResult
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.test.runTest
import platform.PassKit.PKContactFieldEmailAddress
import platform.PassKit.PKContactFieldName
import platform.PassKit.PKContactFieldPhoneNumber
import platform.PassKit.PKContactFieldPostalAddress
import platform.PassKit.PKMerchantCapability3DS
import platform.PassKit.PKMerchantCapabilityCredit
import platform.PassKit.PKMerchantCapabilityDebit
import platform.PassKit.PKMerchantCapabilityEMV
import platform.PassKit.PKPayment
import platform.PassKit.PKPaymentNetworkAmex
import platform.PassKit.PKPaymentNetworkDiscover
import platform.PassKit.PKPaymentNetworkMasterCard
import platform.PassKit.PKPaymentNetworkVisa
import platform.PassKit.PKPaymentRequest
import platform.PassKit.PKPaymentSummaryItem
import platform.PassKit.PKPaymentSummaryItemType
import platform.PassKit.PKShippingType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalForeignApi::class)
class ApplePayProviderTest {

    private val testConfig = ApplePayConfig(
        merchantIdentifier = "merchant.com.landoulsi.payment",
        countryCode = "US",
        merchantCapabilities = listOf(
            ApplePayMerchantCapability.THREE_D_SECURE,
            ApplePayMerchantCapability.DEBIT
        ),
        allowedCardNetworks = listOf(
            CardNetwork.VISA,
            CardNetwork.MASTERCARD,
            CardNetwork.AMEX
        )
    )

    private val testRequest = PaymentRequest(
        id = "tx_ios_123",
        amount = Money.fromMajorUnits(49.99, Currency.USD),
        merchantName = "Landoulsi Store",
        applePayConfig = testConfig
    )

    private class FakeApplePayClient(
        var isReadyResult: Boolean = true,
        var paymentResultToReturn: PaymentResult = PaymentResult.Success(
            transactionId = "tx_ios_123",
            paymentMethodType = PaymentMethodType.APPLE_PAY,
            token = "fake_apple_pay_token"
        ),
        var exceptionToThrow: Throwable? = null
    ) : ApplePayClient {
        var lastPresentedRequest: PaymentRequest? = null

        override fun canMakePayments(): Boolean = isReadyResult
        override fun canMakePaymentsWithActiveCard(config: ApplePayConfig): Boolean = isReadyResult
        override suspend fun isReadyToPay(config: ApplePayConfig): Boolean = isReadyResult

        override suspend fun presentPaymentSheet(request: PaymentRequest): PaymentResult {
            lastPresentedRequest = request
            exceptionToThrow?.let { throw it }
            return paymentResultToReturn
        }

        override fun createPKPaymentRequest(request: PaymentRequest): PKPaymentRequest {
            val pkRequest = PKPaymentRequest()
            val config = request.applePayConfig ?: ApplePayConfig(merchantIdentifier = "merchant.test")
            pkRequest.merchantIdentifier = config.merchantIdentifier
            pkRequest.countryCode = config.countryCode
            pkRequest.currencyCode = request.amount.currency.code
            return pkRequest
        }

        override fun parsePaymentResult(payment: PKPayment, transactionId: String): PaymentResult {
            return paymentResultToReturn
        }
    }

    @Test
    fun testProviderPropertiesAndReadiness() = runTest {
        val fakeClient = FakeApplePayClient(isReadyResult = true)
        val provider = ApplePayProvider(config = testConfig, client = fakeClient)

        assertEquals(PaymentMethodType.APPLE_PAY, provider.paymentMethodType)
        assertTrue(provider.isReadyToPay())

        fakeClient.isReadyResult = false
        assertFalse(provider.isReadyToPay())
    }

    @Test
    fun testProviderPaySuccess() = runTest {
        val expectedResult = PaymentResult.Success(
            transactionId = "tx_ios_123",
            paymentMethodType = PaymentMethodType.APPLE_PAY,
            token = "token_abc_123",
            last4 = "4242",
            cardNetwork = CardNetwork.VISA
        )
        val fakeClient = FakeApplePayClient(paymentResultToReturn = expectedResult)
        val provider = ApplePayProvider(config = testConfig, client = fakeClient)

        val result = provider.pay(testRequest)
        assertEquals(expectedResult, result)
        assertEquals("tx_ios_123", fakeClient.lastPresentedRequest?.id)
    }

    @Test
    fun testProviderPayCanceled() = runTest {
        val fakeClient = FakeApplePayClient(paymentResultToReturn = PaymentResult.Canceled)
        val provider = ApplePayProvider(config = testConfig, client = fakeClient)

        val result = provider.pay(testRequest)
        assertEquals(PaymentResult.Canceled, result)
    }

    @Test
    fun testProviderPayExceptionHandled() = runTest {
        val fakeClient = FakeApplePayClient(
            exceptionToThrow = RuntimeException("PassKit presentation error")
        )
        val provider = ApplePayProvider(config = testConfig, client = fakeClient)

        val result = provider.pay(testRequest)
        assertIs<PaymentResult.Failure>(result)
        assertEquals(PaymentErrorCode.UNKNOWN, result.errorCode)
        assertEquals("PassKit presentation error", result.message)
    }

    @Test
    fun testProviderUsesDefaultConfigWhenNotProvidedInRequest() = runTest {
        val fakeClient = FakeApplePayClient()
        val provider = ApplePayProvider(config = testConfig, client = fakeClient)

        val requestWithoutConfig = PaymentRequest(
            id = "tx_no_config",
            amount = Money.fromMajorUnits(15.00, Currency.USD),
            merchantName = "Landoulsi Store"
        )

        provider.pay(requestWithoutConfig)
        assertEquals(testConfig, fakeClient.lastPresentedRequest?.applePayConfig)
    }

    @Test
    fun testCreatePKPaymentRequestBasic() {
        val client = DefaultApplePayClient()
        val pkRequest = client.createPKPaymentRequest(testRequest)

        assertEquals("merchant.com.landoulsi.payment", pkRequest.merchantIdentifier)
        assertEquals("US", pkRequest.countryCode)
        assertEquals("USD", pkRequest.currencyCode)

        val expectedCaps = PKMerchantCapability3DS or PKMerchantCapabilityDebit
        assertEquals(expectedCaps, pkRequest.merchantCapabilities)

        val summaryItems = pkRequest.paymentSummaryItems
        assertNotNull(summaryItems)
        assertEquals(1, summaryItems.size)
        val totalItem = summaryItems.first() as PKPaymentSummaryItem
        assertEquals("Landoulsi Store", totalItem.label)
        assertEquals(PKPaymentSummaryItemType.PKPaymentSummaryItemTypeFinal, totalItem.type)
        val diff = kotlin.math.abs(totalItem.amount.doubleValue - 49.99)
        assertTrue(diff < 0.001, "Expected ~49.99, got ${totalItem.amount.doubleValue}")
    }

    @Test
    fun testCreatePKPaymentRequestWithCustomSummaryItemsAndContactFields() {
        val client = DefaultApplePayClient()
        val customConfig = testConfig.copy(
            shippingType = ApplePayShippingType.DELIVERY,
            requiredBillingContactFields = setOf(ApplePayContactField.POSTAL_ADDRESS, ApplePayContactField.EMAIL),
            requiredShippingContactFields = setOf(ApplePayContactField.NAME, ApplePayContactField.PHONE_NUMBER),
            summaryItems = listOf(
                ApplePaySummaryItem(
                    label = "Subtotal",
                    amount = Money.fromMajorUnits(40.00, Currency.USD),
                    type = ApplePaySummaryItemType.FINAL
                ),
                ApplePaySummaryItem(
                    label = "Estimated Shipping",
                    amount = Money.fromMajorUnits(9.99, Currency.USD),
                    type = ApplePaySummaryItemType.PENDING
                ),
                ApplePaySummaryItem(
                    label = "Total",
                    amount = Money.fromMajorUnits(49.99, Currency.USD),
                    type = ApplePaySummaryItemType.FINAL
                )
            )
        )
        val request = testRequest.copy(
            applePayConfig = customConfig,
            requireBillingAddress = true,
            requireShipping = true
        )

        val pkRequest = client.createPKPaymentRequest(request)
        assertEquals(PKShippingType.PKShippingTypeDelivery, pkRequest.shippingType)

        val summaryItems = pkRequest.paymentSummaryItems
        assertNotNull(summaryItems)
        assertEquals(3, summaryItems.size)

        val subtotal = summaryItems[0] as PKPaymentSummaryItem
        assertEquals("Subtotal", subtotal.label)
        val subtotalDiff = kotlin.math.abs(subtotal.amount.doubleValue - 40.0)
        assertTrue(subtotalDiff < 0.001, "Expected ~40.0, got ${subtotal.amount.doubleValue}")
        assertEquals(PKPaymentSummaryItemType.PKPaymentSummaryItemTypeFinal, subtotal.type)

        val shipping = summaryItems[1] as PKPaymentSummaryItem
        assertEquals("Estimated Shipping", shipping.label)
        val shippingDiff = kotlin.math.abs(shipping.amount.doubleValue - 9.99)
        assertTrue(shippingDiff < 0.001, "Expected ~9.99, got ${shipping.amount.doubleValue}")
        assertEquals(PKPaymentSummaryItemType.PKPaymentSummaryItemTypePending, shipping.type)

        val total = summaryItems[2] as PKPaymentSummaryItem
        assertEquals("Total", total.label)
        val totalDiff = kotlin.math.abs(total.amount.doubleValue - 49.99)
        assertTrue(totalDiff < 0.001, "Expected ~49.99, got ${total.amount.doubleValue}")
        assertEquals(PKPaymentSummaryItemType.PKPaymentSummaryItemTypeFinal, total.type)
    }

    @Test
    fun testPresentPaymentSheetBlankMerchantIdentifier() = runTest {
        val client = DefaultApplePayClient()
        val blankConfigRequest = testRequest.copy(
            applePayConfig = testConfig.copy(merchantIdentifier = "")
        )

        val result = client.presentPaymentSheet(blankConfigRequest)
        assertIs<PaymentResult.Failure>(result)
        assertEquals(PaymentErrorCode.CONFIGURATION_ERROR, result.errorCode)
        assertTrue(result.message.contains("merchantIdentifier cannot be empty"))
    }

    @Test
    fun testPresentPaymentSheetNullApplePayConfig() = runTest {
        val client = DefaultApplePayClient()
        val nullConfigRequest = testRequest.copy(
            applePayConfig = null
        )

        val result = client.presentPaymentSheet(nullConfigRequest)
        assertIs<PaymentResult.Failure>(result)
        assertEquals(PaymentErrorCode.CONFIGURATION_ERROR, result.errorCode)
        assertTrue(result.message.contains("merchantIdentifier cannot be empty"))
    }

    @Test
    fun testPresentPaymentSheetInvalidMerchantPrefix() = runTest {
        val client = DefaultApplePayClient()
        val invalidConfigRequest = testRequest.copy(
            applePayConfig = testConfig.copy(merchantIdentifier = "com.invalid.prefix")
        )

        val result = client.presentPaymentSheet(invalidConfigRequest)
        assertIs<PaymentResult.Failure>(result)
        assertEquals(PaymentErrorCode.CONFIGURATION_ERROR, result.errorCode)
        assertTrue(result.message.contains("must start with 'merchant.'"))
    }

    @Test
    fun testPresentPaymentSheetInvalidCountryCode() = runTest {
        val client = DefaultApplePayClient()
        val invalidCountryRequest = testRequest.copy(
            applePayConfig = testConfig.copy(countryCode = "USA")
        )

        val result = client.presentPaymentSheet(invalidCountryRequest)
        assertIs<PaymentResult.Failure>(result)
        assertEquals(PaymentErrorCode.CONFIGURATION_ERROR, result.errorCode)
        assertTrue(result.message.contains("countryCode must be a 2-letter ISO"))
    }

    @Test
    fun testPresentPaymentSheetInvalidSupportedCountries() = runTest {
        val client = DefaultApplePayClient()
        val invalidCountriesRequest = testRequest.copy(
            applePayConfig = testConfig.copy(supportedCountries = setOf("US", "FRA"))
        )

        val result = client.presentPaymentSheet(invalidCountriesRequest)
        assertIs<PaymentResult.Failure>(result)
        assertEquals(PaymentErrorCode.CONFIGURATION_ERROR, result.errorCode)
        assertTrue(result.message.contains("supportedCountries must contain 2-letter ISO"))
    }

    @Test
    fun testPresentPaymentSheetZeroOrNegativeAmount() = runTest {
        val client = DefaultApplePayClient()
        val zeroRequest = testRequest.copy(
            amount = Money(0L, Currency.USD)
        )

        val resultZero = client.presentPaymentSheet(zeroRequest)
        assertIs<PaymentResult.Failure>(resultZero)
        assertEquals(PaymentErrorCode.CONFIGURATION_ERROR, resultZero.errorCode)
        assertTrue(resultZero.message.contains("greater than 0"))

        val negativeRequest = testRequest.copy(
            amount = Money(-100L, Currency.USD)
        )
        val resultNegative = client.presentPaymentSheet(negativeRequest)
        assertIs<PaymentResult.Failure>(resultNegative)
        assertEquals(PaymentErrorCode.CONFIGURATION_ERROR, resultNegative.errorCode)
        assertTrue(resultNegative.message.contains("greater than 0"))
    }

    @Test
    fun testPresentPaymentSheetSummaryItemsCurrencyMismatch() = runTest {
        val client = DefaultApplePayClient()
        val mismatchRequest = testRequest.copy(
            amount = Money.fromMajorUnits(50.0, Currency.USD),
            applePayConfig = testConfig.copy(
                summaryItems = listOf(
                    ApplePaySummaryItem("Subtotal", Money.fromMajorUnits(50.0, Currency.EUR), ApplePaySummaryItemType.FINAL)
                )
            )
        )

        val result = client.presentPaymentSheet(mismatchRequest)
        assertIs<PaymentResult.Failure>(result)
        assertEquals(PaymentErrorCode.CONFIGURATION_ERROR, result.errorCode)
        assertTrue(result.message.contains("currencies must match"))
    }

    @Test
    fun testPresentPaymentSheetSummaryItemsTotalMismatch() = runTest {
        val client = DefaultApplePayClient()
        val mismatchRequest = testRequest.copy(
            amount = Money.fromMajorUnits(50.0, Currency.USD),
            applePayConfig = testConfig.copy(
                summaryItems = listOf(
                    ApplePaySummaryItem("Subtotal", Money.fromMajorUnits(40.0, Currency.USD), ApplePaySummaryItemType.FINAL)
                )
            )
        )

        val result = client.presentPaymentSheet(mismatchRequest)
        assertIs<PaymentResult.Failure>(result)
        assertEquals(PaymentErrorCode.CONFIGURATION_ERROR, result.errorCode)
        assertTrue(result.message.contains("must match the total request amount"))
    }

    @Test
    fun testCreatePKPaymentRequestValidationExceptions() {
        val client = DefaultApplePayClient()

        val noConfigRequest = testRequest.copy(applePayConfig = null)
        val ex1 = kotlin.test.assertFailsWith<IllegalArgumentException> {
            client.createPKPaymentRequest(noConfigRequest)
        }
        assertTrue(ex1.message?.contains("PaymentRequest must have applePayConfig set") == true)

        val blankIdRequest = testRequest.copy(applePayConfig = testConfig.copy(merchantIdentifier = "  "))
        val ex2 = kotlin.test.assertFailsWith<IllegalArgumentException> {
            client.createPKPaymentRequest(blankIdRequest)
        }
        assertTrue(ex2.message?.contains("merchantIdentifier cannot be empty") == true)

        val zeroAmountRequest = testRequest.copy(amount = Money.ZERO)
        val ex3 = kotlin.test.assertFailsWith<IllegalArgumentException> {
            client.createPKPaymentRequest(zeroAmountRequest)
        }
        assertTrue(ex3.message?.contains("greater than 0") == true)

        val currencyMismatchRequest = testRequest.copy(
            applePayConfig = testConfig.copy(
                summaryItems = listOf(
                    ApplePaySummaryItem("Total", Money.fromMajorUnits(49.99, Currency.EUR))
                )
            )
        )
        val ex4 = kotlin.test.assertFailsWith<IllegalArgumentException> {
            client.createPKPaymentRequest(currencyMismatchRequest)
        }
        assertTrue(ex4.message?.contains("currencies must match") == true)

        val totalMismatchRequest = testRequest.copy(
            applePayConfig = testConfig.copy(
                summaryItems = listOf(
                    ApplePaySummaryItem("Total", Money.fromMajorUnits(30.00, Currency.USD))
                )
            )
        )
        val ex5 = kotlin.test.assertFailsWith<IllegalArgumentException> {
            client.createPKPaymentRequest(totalMismatchRequest)
        }
        assertTrue(ex5.message?.contains("last Apple Pay summary item must match") == true)
    }

    @Test
    fun testCreatePKPaymentRequestDefaultMerchantNameFallback() {
        val client = DefaultApplePayClient()
        val requestWithoutMerchantName = testRequest.copy(
            merchantName = null,
            applePayConfig = testConfig.copy(summaryItems = emptyList())
        )

        val pkRequest = client.createPKPaymentRequest(requestWithoutMerchantName)
        val summaryItems = pkRequest.paymentSummaryItems
        assertNotNull(summaryItems)
        assertEquals(1, summaryItems.size)
        val item = summaryItems.first() as PKPaymentSummaryItem
        assertEquals("Total", item.label)
    }

    @Test
    fun testCreatePKPaymentRequestAllCapabilitiesAndCardNetworks() {
        val client = DefaultApplePayClient()
        val fullConfig = testConfig.copy(
            merchantCapabilities = listOf(
                ApplePayMerchantCapability.THREE_D_SECURE,
                ApplePayMerchantCapability.EMV,
                ApplePayMerchantCapability.CREDIT,
                ApplePayMerchantCapability.DEBIT
            ),
            allowedCardNetworks = listOf(
                CardNetwork.VISA,
                CardNetwork.MASTERCARD,
                CardNetwork.AMEX,
                CardNetwork.DISCOVER,
                CardNetwork.JCB,
                CardNetwork.INTERAC,
                CardNetwork.UNION_PAY,
                CardNetwork.DINERS_CLUB
            ),
            supportedCountries = setOf("US", "GB", "CA"),
            shippingType = ApplePayShippingType.STORE_PICKUP
        )

        val request = testRequest.copy(applePayConfig = fullConfig)
        val pkRequest = client.createPKPaymentRequest(request)

        val expectedCaps = PKMerchantCapability3DS or PKMerchantCapabilityEMV or PKMerchantCapabilityCredit or PKMerchantCapabilityDebit
        assertEquals(expectedCaps, pkRequest.merchantCapabilities)
        assertEquals(PKShippingType.PKShippingTypeStorePickup, pkRequest.shippingType)
        assertNotNull(pkRequest.supportedCountries)
        assertEquals(3L, pkRequest.supportedCountries!!.count().toLong())
    }

    @Test
    fun testCreatePKPaymentRequestEmptyMerchantCapabilitiesDefaultsTo3DS() {
        val client = DefaultApplePayClient()
        val config = testConfig.copy(merchantCapabilities = emptyList())
        val pkRequest = client.createPKPaymentRequest(testRequest.copy(applePayConfig = config))

        assertEquals(PKMerchantCapability3DS, pkRequest.merchantCapabilities)
    }

    @Test
    fun testCreatePKPaymentRequestAllShippingTypes() {
        val client = DefaultApplePayClient()

        val reqShipping = client.createPKPaymentRequest(testRequest.copy(applePayConfig = testConfig.copy(shippingType = ApplePayShippingType.SHIPPING)))
        assertEquals(PKShippingType.PKShippingTypeShipping, reqShipping.shippingType)

        val reqDelivery = client.createPKPaymentRequest(testRequest.copy(applePayConfig = testConfig.copy(shippingType = ApplePayShippingType.DELIVERY)))
        assertEquals(PKShippingType.PKShippingTypeDelivery, reqDelivery.shippingType)

        val reqStorePickup = client.createPKPaymentRequest(testRequest.copy(applePayConfig = testConfig.copy(shippingType = ApplePayShippingType.STORE_PICKUP)))
        assertEquals(PKShippingType.PKShippingTypeStorePickup, reqStorePickup.shippingType)

        val reqServicePickup = client.createPKPaymentRequest(testRequest.copy(applePayConfig = testConfig.copy(shippingType = ApplePayShippingType.SERVICE_PICKUP)))
        assertEquals(PKShippingType.PKShippingTypeServicePickup, reqServicePickup.shippingType)
    }

    @Test
    fun testCreatePKPaymentRequestAllContactFields() {
        val client = DefaultApplePayClient()
        val allFields = setOf(
            ApplePayContactField.POSTAL_ADDRESS,
            ApplePayContactField.EMAIL,
            ApplePayContactField.PHONE_NUMBER,
            ApplePayContactField.NAME,
            ApplePayContactField.PHONETIC_NAME
        )
        val config = testConfig.copy(
            requiredBillingContactFields = allFields,
            requiredShippingContactFields = allFields
        )
        val request = testRequest.copy(
            applePayConfig = config,
            requireBillingAddress = false,
            requireShipping = false
        )

        val pkRequest = client.createPKPaymentRequest(request)
        assertNotNull(pkRequest.requiredBillingContactFields)
        assertEquals(5L, pkRequest.requiredBillingContactFields.count().toLong())
        assertNotNull(pkRequest.requiredShippingContactFields)
        assertEquals(5L, pkRequest.requiredShippingContactFields.count().toLong())
    }

    @Test
    fun testProviderPayPropagatesCustomRequestConfig() = runTest {
        val fakeClient = FakeApplePayClient()
        val provider = ApplePayProvider(config = testConfig, client = fakeClient)

        val customConfig = testConfig.copy(merchantIdentifier = "merchant.custom.id")
        val customRequest = testRequest.copy(applePayConfig = customConfig)

        provider.pay(customRequest)
        assertEquals("merchant.custom.id", fakeClient.lastPresentedRequest?.applePayConfig?.merchantIdentifier)
    }

    @Test
    fun testProviderPayCancellationExceptionRethrown() = runTest {
        val fakeClient = FakeApplePayClient(
            exceptionToThrow = kotlinx.coroutines.CancellationException("Job cancelled")
        )
        val provider = ApplePayProvider(config = testConfig, client = fakeClient)

        kotlin.test.assertFailsWith<kotlinx.coroutines.CancellationException> {
            provider.pay(testRequest)
        }
    }
}
