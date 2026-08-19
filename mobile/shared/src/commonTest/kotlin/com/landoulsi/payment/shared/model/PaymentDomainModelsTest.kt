package com.landoulsi.payment.shared.model

import com.landoulsi.payment.shared.provider.PaymentProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PaymentDomainModelsTest {

    @Test
    fun testCurrencyFromCode() {
        val usd = Currency.fromCode("USD")
        assertEquals("USD", usd.code)
        assertEquals("$", usd.symbol)
        assertEquals(2, usd.decimalPlaces)

        val jpy = Currency.fromCode("jpy")
        assertEquals("JPY", jpy.code)
        assertEquals("¥", jpy.symbol)
        assertEquals(0, jpy.decimalPlaces)

        val custom = Currency.fromCode("xyz")
        assertEquals("XYZ", custom.code)
        assertEquals("XYZ", custom.symbol)
        assertEquals(2, custom.decimalPlaces)
    }

    @Test
    fun testMoneyFormattingAndParsing() {
        val usdMoney = Money.ofCents(1050, Currency.USD)
        assertEquals("10.50", usdMoney.formattedAmount())
        assertEquals("$10.50", usdMoney.formattedWithSymbol())

        val jpyMoney = Money(1500, Currency.JPY)
        assertEquals("1500", jpyMoney.formattedAmount())
        assertEquals("¥1500", jpyMoney.formattedWithSymbol())

        val fromDouble = Money.fromMajorUnits(24.99, Currency.EUR)
        assertEquals(2499, fromDouble.amountMinorUnits)
        assertEquals("24.99", fromDouble.formattedAmount())
        assertEquals("€24.99", fromDouble.formattedWithSymbol())

        val fromLong = Money.fromMajorUnits(50L, Currency.GBP)
        assertEquals(5000, fromLong.amountMinorUnits)
        assertEquals("50.00", fromLong.formattedAmount())
        assertEquals("£50.00", fromLong.formattedWithSymbol())

        val negative = Money(-500, Currency.USD)
        assertEquals("-5.00", negative.formattedAmount())
        assertEquals("-$5.00", negative.formattedWithSymbol())
    }

    @Test
    fun testMoneyArithmeticAndComparison() {
        val m1 = Money.ofCents(1000, Currency.USD)
        val m2 = Money.ofCents(500, Currency.USD)

        assertEquals(Money.ofCents(1500, Currency.USD), m1 + m2)
        assertEquals(Money.ofCents(500, Currency.USD), m1 - m2)
        assertTrue(m1 > m2)
        assertTrue(m2 < m1)

        val eur = Money.ofCents(1000, Currency.EUR)
        assertFailsWith<IllegalArgumentException> {
            m1 + eur
        }
        assertFailsWith<IllegalArgumentException> {
            m1 - eur
        }
        assertFailsWith<IllegalArgumentException> {
            m1.compareTo(eur)
        }
    }

    @Test
    fun testCardNetworkParsing() {
        assertEquals(CardNetwork.VISA, CardNetwork.fromName("visa"))
        assertEquals(CardNetwork.MASTERCARD, CardNetwork.fromName("MASTERCARD"))
        assertEquals(CardNetwork.AMEX, CardNetwork.fromName("amex"))
        assertEquals(CardNetwork.DINERS_CLUB, CardNetwork.fromName("diners_club"))
        assertEquals(CardNetwork.UNION_PAY, CardNetwork.fromName("unionpay"))
        assertNull(CardNetwork.fromName("unknown_network"))
    }

    @Test
    fun testGooglePayConfigTokenizationSpecifications() {
        val stripeSpec = GooglePayTokenizationSpecification.Gateway.stripe(
            publishableKey = "pk_test_12345",
            stripeVersion = "2020-08-27"
        )
        assertEquals("PAYMENT_GATEWAY", stripeSpec.type)
        assertEquals("stripe", stripeSpec.parameters["gateway"])
        assertEquals("pk_test_12345", stripeSpec.parameters["stripe:publishableKey"])
        assertEquals("2020-08-27", stripeSpec.parameters["stripe:version"])

        val adyenSpec = GooglePayTokenizationSpecification.Gateway.adyen(
            gatewayMerchantId = "TestMerchant_123"
        )
        assertEquals("PAYMENT_GATEWAY", adyenSpec.type)
        assertEquals("adyen", adyenSpec.parameters["gateway"])
        assertEquals("TestMerchant_123", adyenSpec.parameters["gatewayMerchantId"])

        val directSpec = GooglePayTokenizationSpecification.Direct(
            publicKey = "BO19OJ0A...publicKey",
            protocolVersion = "ECv2"
        )
        assertEquals("DIRECT", directSpec.type)
        assertEquals("BO19OJ0A...publicKey", directSpec.parameters["publicKey"])
        assertEquals("ECv2", directSpec.parameters["protocolVersion"])
    }

    @Test
    fun testGooglePayConfigDefaultsAndPaymentRequest() {
        val gpayConfig = GooglePayConfig(
            merchantId = "1234567890",
            merchantName = "Test Store",
            tokenizationSpecification = GooglePayTokenizationSpecification.Gateway.stripe("pk_test_123")
        )

        assertEquals(GooglePayEnvironment.TEST, gpayConfig.environment)
        assertEquals("1234567890", gpayConfig.merchantId)
        assertEquals("Test Store", gpayConfig.merchantName)
        assertTrue(gpayConfig.allowedCardNetworks.contains(CardNetwork.VISA))
        assertTrue(gpayConfig.allowedAuthMethods.contains(GooglePayAuthMethod.PAN_ONLY))
        assertTrue(gpayConfig.allowedAuthMethods.contains(GooglePayAuthMethod.CRYPTOGRAM_3DS))
        assertFalse(gpayConfig.billingAddressRequired)

        val request = PaymentRequest(
            id = "order_123",
            amount = Money.fromMajorUnits(19.99, Currency.USD),
            merchantName = "Test Store",
            googlePayConfig = gpayConfig
        )

        assertEquals("order_123", request.id)
        assertEquals("19.99", request.amount.formattedAmount())
        assertEquals("Test Store", request.merchantName)
        assertEquals(gpayConfig, request.googlePayConfig)
    }

    @Test
    fun testPaymentResultStates() {
        val success = PaymentResult.Success(
            transactionId = "tx_987",
            paymentMethodType = PaymentMethodType.GOOGLE_PAY,
            token = "tok_abc123",
            last4 = "4242",
            cardNetwork = CardNetwork.VISA
        )
        assertEquals("tx_987", success.transactionId)
        assertEquals(PaymentMethodType.GOOGLE_PAY, success.paymentMethodType)
        assertEquals("tok_abc123", success.token)
        assertEquals("4242", success.last4)
        assertEquals(CardNetwork.VISA, success.cardNetwork)

        val failure = PaymentResult.Failure(
            errorCode = PaymentErrorCode.CARD_DECLINED,
            message = "Your card was declined"
        )
        assertEquals(PaymentErrorCode.CARD_DECLINED, failure.errorCode)
        assertEquals("Your card was declined", failure.message)

        val canceled: PaymentResult = PaymentResult.Canceled
        assertEquals(PaymentResult.Canceled, canceled)
    }

    @Test
    fun testPaymentProviderContract() {
        val mockProvider = object : PaymentProvider {
            override val paymentMethodType: PaymentMethodType = PaymentMethodType.GOOGLE_PAY
            override suspend fun isReadyToPay(): Boolean = true
            override suspend fun pay(request: PaymentRequest): PaymentResult {
                return PaymentResult.Success(
                    transactionId = "mock_tx",
                    paymentMethodType = paymentMethodType,
                    token = "mock_token"
                )
            }
        }

        assertEquals(PaymentMethodType.GOOGLE_PAY, mockProvider.paymentMethodType)
    }
}
