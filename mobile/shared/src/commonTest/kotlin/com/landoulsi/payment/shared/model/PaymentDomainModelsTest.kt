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

    @Test
    fun testCardNetworkFromNameHyphenated() {
        assertEquals(CardNetwork.DINERS_CLUB, CardNetwork.fromName("diners-club"))
        assertEquals(CardNetwork.UNION_PAY, CardNetwork.fromName("union-pay"))
        assertEquals(CardNetwork.UNION_PAY, CardNetwork.fromName("UNION-PAY"))
    }

    @Test
    fun testCardNetworkFromNameWithSpaces() {
        assertEquals(CardNetwork.DINERS_CLUB, CardNetwork.fromName("diners club"))
    }

    @Test
    fun testGatewayTokenizationEmptyMerchantIdExcluded() {
        val spec = GooglePayTokenizationSpecification.Gateway(
            gateway = "stripe",
            gatewayMerchantId = ""
        )
        assertFalse(spec.parameters.containsKey("gatewayMerchantId"))
        assertEquals("stripe", spec.parameters["gateway"])
    }

    @Test
    fun testGatewayTokenizationWithMerchantId() {
        val spec = GooglePayTokenizationSpecification.Gateway(
            gateway = "adyen",
            gatewayMerchantId = "merchant_123"
        )
        assertEquals("merchant_123", spec.parameters["gatewayMerchantId"])
        assertEquals("adyen", spec.parameters["gateway"])
    }

    @Test
    fun testGatewayBraintreeTokenization() {
        val spec = GooglePayTokenizationSpecification.Gateway.braintree(
            tokenizationKey = "bt_test_key"
        )
        assertEquals("PAYMENT_GATEWAY", spec.type)
        assertEquals("braintree", spec.parameters["gateway"])
        assertEquals("bt_test_key", spec.parameters["braintree:merchantId"])
        assertEquals("bt_test_key", spec.parameters["braintree:clientKey"])
        assertEquals("v1", spec.parameters["braintree:apiVersion"])
        assertEquals("custom", spec.parameters["braintree:sdkVersion"])
    }

    @Test
    fun testMoneyZero() {
        val zero = Money.ZERO
        assertEquals(0L, zero.amountMinorUnits)
        assertEquals("0.00", zero.formattedAmount())

        val zeroJpy = Money(0, Currency.JPY)
        assertEquals("0", zeroJpy.formattedAmount())
    }

    @Test
    fun testMoneyFromMajorUnitsZero() {
        val zero = Money.fromMajorUnits(0.0, Currency.USD)
        assertEquals(0L, zero.amountMinorUnits)
        assertEquals("0.00", zero.formattedAmount())
    }

    @Test
    fun testGooglePayBillingAddressParametersDefaults() {
        val params = GooglePayBillingAddressParameters()
        assertEquals(GooglePayBillingAddressFormat.MIN, params.format)
        assertFalse(params.phoneNumberRequired)
    }

    @Test
    fun testGooglePayShippingAddressParametersDefaults() {
        val params = GooglePayShippingAddressParameters()
        assertTrue(params.allowedCountryCodes.isEmpty())
        assertFalse(params.phoneNumberRequired)
    }

    @Test
    fun testPaymentRequestDefaultValues() {
        val request = PaymentRequest(
            id = "order_default",
            amount = Money.ofCents(100, Currency.USD)
        )
        assertNull(request.merchantName)
        assertNull(request.description)
        assertNull(request.googlePayConfig)
        assertFalse(request.requireShipping)
        assertFalse(request.requireBillingAddress)
        assertTrue(request.metadata.isEmpty())
        assertTrue(request.allowedPaymentMethods.contains(PaymentMethodType.GOOGLE_PAY))
        assertTrue(request.allowedPaymentMethods.contains(PaymentMethodType.CARD))
    }

    @Test
    fun testPaymentMethodTypeIdentifiers() {
        assertEquals("google_pay", PaymentMethodType.GOOGLE_PAY.identifier)
        assertEquals("apple_pay", PaymentMethodType.APPLE_PAY.identifier)
        assertEquals("card", PaymentMethodType.CARD.identifier)
        assertEquals("paypal", PaymentMethodType.PAYPAL.identifier)
        assertEquals("klarna", PaymentMethodType.KLARNA.identifier)
        assertEquals("ideal", PaymentMethodType.IDEAL.identifier)
    }

    @Test
    fun testGooglePayConfigFullCustomization() {
        val config = GooglePayConfig(
            environment = GooglePayEnvironment.PRODUCTION,
            merchantId = "prod_merchant",
            merchantName = "Prod Store",
            allowedCardNetworks = listOf(CardNetwork.JCB, CardNetwork.INTERAC),
            allowedAuthMethods = listOf(GooglePayAuthMethod.CRYPTOGRAM_3DS),
            tokenizationSpecification = GooglePayTokenizationSpecification.Direct(
                publicKey = "pub_key_123",
                protocolVersion = "ECv1"
            ),
            allowPrepaidCards = false,
            allowCreditCards = false,
            billingAddressRequired = true,
            billingAddressParameters = GooglePayBillingAddressParameters(
                format = GooglePayBillingAddressFormat.MIN,
                phoneNumberRequired = true
            ),
            emailRequired = true,
            shippingAddressRequired = true,
            shippingAddressParameters = GooglePayShippingAddressParameters(
                allowedCountryCodes = listOf("JP"),
                phoneNumberRequired = true
            )
        )
        assertEquals(GooglePayEnvironment.PRODUCTION, config.environment)
        assertEquals("prod_merchant", config.merchantId)
        assertEquals(2, config.allowedCardNetworks.size)
        assertEquals(1, config.allowedAuthMethods.size)
        assertFalse(config.allowPrepaidCards)
        assertFalse(config.allowCreditCards)
        assertTrue(config.billingAddressRequired)
        assertTrue(config.emailRequired)
        assertTrue(config.shippingAddressRequired)
    }
}
