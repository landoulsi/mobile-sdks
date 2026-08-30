package com.landoulsi.payment.shared.model

import com.landoulsi.payment.shared.provider.PaymentProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
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
        assertNull(request.applePayConfig)
        assertFalse(request.requireShipping)
        assertFalse(request.requireBillingAddress)
        assertTrue(request.metadata.isEmpty())
        assertTrue(request.allowedPaymentMethods.contains(PaymentMethodType.GOOGLE_PAY))
        assertTrue(request.allowedPaymentMethods.contains(PaymentMethodType.APPLE_PAY))
        assertTrue(request.allowedPaymentMethods.contains(PaymentMethodType.CARD))
    }

    @Test
    fun testApplePayConfigDefaultsAndPaymentRequest() {
        val applePayConfig = ApplePayConfig(
            merchantIdentifier = "merchant.com.landoulsi.payment"
        )

        assertEquals("merchant.com.landoulsi.payment", applePayConfig.merchantIdentifier)
        assertEquals("US", applePayConfig.countryCode)
        assertEquals(listOf(ApplePayMerchantCapability.THREE_D_SECURE), applePayConfig.merchantCapabilities)
        assertEquals(
            listOf(CardNetwork.VISA, CardNetwork.MASTERCARD, CardNetwork.AMEX, CardNetwork.DISCOVER),
            applePayConfig.allowedCardNetworks
        )
        assertNull(applePayConfig.merchantSessionProvider)
        assertTrue(applePayConfig.requiredBillingContactFields.isEmpty())
        assertTrue(applePayConfig.requiredShippingContactFields.isEmpty())
        assertEquals(ApplePayShippingType.SHIPPING, applePayConfig.shippingType)
        assertTrue(applePayConfig.summaryItems.isEmpty())
        assertTrue(applePayConfig.supportedCountries.isEmpty())

        val request = PaymentRequest(
            id = "order_applepay_123",
            amount = Money.fromMajorUnits(49.99, Currency.USD),
            merchantName = "Landoulsi Store",
            applePayConfig = applePayConfig
        )

        assertEquals("order_applepay_123", request.id)
        assertEquals("49.99", request.amount.formattedAmount())
        assertEquals("Landoulsi Store", request.merchantName)
        assertEquals(applePayConfig, request.applePayConfig)
        assertTrue(request.allowedPaymentMethods.contains(PaymentMethodType.APPLE_PAY))
    }

    @Test
    fun testApplePayConfigFullCustomization() {
        val customSummaryItems = listOf(
            ApplePaySummaryItem("Subtotal", Money.ofCents(3000, Currency.EUR)),
            ApplePaySummaryItem("Shipping", Money.ofCents(500, Currency.EUR)),
            ApplePaySummaryItem("Estimated Tax", Money.ofCents(250, Currency.EUR), ApplePaySummaryItemType.PENDING),
            ApplePaySummaryItem("Total", Money.ofCents(3750, Currency.EUR), ApplePaySummaryItemType.FINAL)
        )

        val config = ApplePayConfig(
            merchantIdentifier = "merchant.com.example.shop",
            countryCode = "FR",
            merchantCapabilities = listOf(
                ApplePayMerchantCapability.THREE_D_SECURE,
                ApplePayMerchantCapability.EMV,
                ApplePayMerchantCapability.CREDIT,
                ApplePayMerchantCapability.DEBIT
            ),
            allowedCardNetworks = listOf(CardNetwork.VISA, CardNetwork.MASTERCARD),
            requiredBillingContactFields = setOf(ApplePayContactField.POSTAL_ADDRESS, ApplePayContactField.EMAIL),
            requiredShippingContactFields = setOf(
                ApplePayContactField.POSTAL_ADDRESS,
                ApplePayContactField.NAME,
                ApplePayContactField.PHONE_NUMBER
            ),
            shippingType = ApplePayShippingType.DELIVERY,
            summaryItems = customSummaryItems,
            supportedCountries = setOf("FR", "DE", "ES")
        )

        assertEquals("merchant.com.example.shop", config.merchantIdentifier)
        assertEquals("FR", config.countryCode)
        assertEquals(4, config.merchantCapabilities.size)
        assertEquals(2, config.allowedCardNetworks.size)
        assertTrue(config.requiredBillingContactFields.contains(ApplePayContactField.POSTAL_ADDRESS))
        assertTrue(config.requiredBillingContactFields.contains(ApplePayContactField.EMAIL))
        assertTrue(config.requiredShippingContactFields.contains(ApplePayContactField.PHONE_NUMBER))
        assertEquals(ApplePayShippingType.DELIVERY, config.shippingType)
        assertEquals(4, config.summaryItems.size)
        assertEquals(ApplePaySummaryItemType.PENDING, config.summaryItems[2].type)
        assertEquals(3, config.supportedCountries.size)
    }

    @Test
    fun testApplePayMerchantSessionProvider() = kotlinx.coroutines.test.runTest {
        var calledUrl: String? = null
        val provider = ApplePayMerchantSessionProvider { validationUrl ->
            calledUrl = validationUrl
            """{"epochTimestamp":123456789,"expiresAt":123456799,"merchantSessionIdentifier":"SSH_123"}"""
        }

        val config = ApplePayConfig(
            merchantIdentifier = "merchant.com.example.shop",
            merchantSessionProvider = provider
        )

        assertNotNull(config.merchantSessionProvider)
        val sessionJson = config.merchantSessionProvider.provideMerchantSession("https://apple-pay-gateway.apple.com/paymentservices/startSession")
        assertEquals("https://apple-pay-gateway.apple.com/paymentservices/startSession", calledUrl)
        assertTrue(sessionJson.contains("SSH_123"))
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

    @Test
    fun testApplePayEnumsAndDataClasses() {
        val capabilities = ApplePayMerchantCapability.entries
        assertEquals(4, capabilities.size)
        assertTrue(capabilities.contains(ApplePayMerchantCapability.THREE_D_SECURE))
        assertTrue(capabilities.contains(ApplePayMerchantCapability.EMV))
        assertTrue(capabilities.contains(ApplePayMerchantCapability.CREDIT))
        assertTrue(capabilities.contains(ApplePayMerchantCapability.DEBIT))

        val shippingTypes = ApplePayShippingType.entries
        assertEquals(4, shippingTypes.size)
        assertTrue(shippingTypes.contains(ApplePayShippingType.SHIPPING))
        assertTrue(shippingTypes.contains(ApplePayShippingType.DELIVERY))
        assertTrue(shippingTypes.contains(ApplePayShippingType.STORE_PICKUP))
        assertTrue(shippingTypes.contains(ApplePayShippingType.SERVICE_PICKUP))

        val summaryItemTypes = ApplePaySummaryItemType.entries
        assertEquals(2, summaryItemTypes.size)
        assertTrue(summaryItemTypes.contains(ApplePaySummaryItemType.FINAL))
        assertTrue(summaryItemTypes.contains(ApplePaySummaryItemType.PENDING))

        val contactFields = ApplePayContactField.entries
        assertEquals(5, contactFields.size)
        assertTrue(contactFields.contains(ApplePayContactField.POSTAL_ADDRESS))
        assertTrue(contactFields.contains(ApplePayContactField.EMAIL))
        assertTrue(contactFields.contains(ApplePayContactField.PHONE_NUMBER))
        assertTrue(contactFields.contains(ApplePayContactField.NAME))
        assertTrue(contactFields.contains(ApplePayContactField.PHONETIC_NAME))

        val defaultItem = ApplePaySummaryItem("Subtotal", Money.ofCents(1000, Currency.USD))
        assertEquals(ApplePaySummaryItemType.FINAL, defaultItem.type)
        val pendingItem = defaultItem.copy(type = ApplePaySummaryItemType.PENDING)
        assertEquals(ApplePaySummaryItemType.PENDING, pendingItem.type)
        assertEquals(defaultItem, defaultItem.copy())
    }

    @Test
    fun testPaymentRequestWithApplePayAndGooglePayTogether() {
        val gpay = GooglePayConfig(
            merchantId = "gpay_123",
            merchantName = "Test Merchant",
            tokenizationSpecification = GooglePayTokenizationSpecification.Gateway.stripe("pk_123")
        )
        val applePay = ApplePayConfig(
            merchantIdentifier = "merchant.com.example"
        )
        val request = PaymentRequest(
            id = "dual_wallet_req",
            amount = Money.fromMajorUnits(100.0, Currency.USD),
            googlePayConfig = gpay,
            applePayConfig = applePay,
            requireShipping = true,
            requireBillingAddress = true
        )

        assertEquals("dual_wallet_req", request.id)
        assertNotNull(request.googlePayConfig)
        assertNotNull(request.applePayConfig)
        assertTrue(request.requireShipping)
        assertTrue(request.requireBillingAddress)
        assertTrue(request.allowedPaymentMethods.contains(PaymentMethodType.GOOGLE_PAY))
        assertTrue(request.allowedPaymentMethods.contains(PaymentMethodType.APPLE_PAY))
    }

    @Test
    fun testThreeDSChallengeDefaultsAndCreation() {
        val challenge = ThreeDSChallenge(
            paymentIntentId = "pi_3ds_001",
            clientSecret = "secret_001",
            redirectUrl = "https://bank.com/acs"
        )

        assertEquals("pi_3ds_001", challenge.paymentIntentId)
        assertEquals("secret_001", challenge.clientSecret)
        assertEquals("https://bank.com/acs", challenge.redirectUrl)
        assertEquals("paymentsdk://3ds-complete", challenge.returnUrl)
        assertNull(challenge.acsUrl)
        assertNull(challenge.cReq)
        assertNull(challenge.threeDSServerTransId)
    }

    @Test
    fun testThreeDSResultVariants() {
        val completed: ThreeDSResult = ThreeDSResult.Completed("payload_token_123")
        assertTrue(completed is ThreeDSResult.Completed)
        assertEquals("payload_token_123", completed.returnPayload)

        val failed: ThreeDSResult = ThreeDSResult.Failed(
            errorCode = PaymentErrorCode.AUTHENTICATION_FAILED,
            message = "Authentication timed out"
        )
        assertTrue(failed is ThreeDSResult.Failed)
        assertEquals(PaymentErrorCode.AUTHENTICATION_FAILED, failed.errorCode)
        assertEquals("Authentication timed out", failed.message)

        val canceled: ThreeDSResult = ThreeDSResult.Canceled
        assertEquals(ThreeDSResult.Canceled, canceled)
    }

    @Test
    fun testParseThreeDSReturnUrlEmptyInputs() {
        assertNull(parseThreeDSReturnUrl("", "paymentsdk://3ds-complete"))
        assertNull(parseThreeDSReturnUrl("paymentsdk://3ds-complete", ""))
        assertNull(parseThreeDSReturnUrl("   ", "paymentsdk://3ds-complete"))
        assertNull(parseThreeDSReturnUrl("paymentsdk://3ds-complete", "   "))
    }

    @Test
    fun testParseThreeDSReturnUrlNonMatchingUrls() {
        val expected = "paymentsdk://3ds-complete"
        // Intermediate ACS navigation
        assertNull(parseThreeDSReturnUrl("https://acs.bank.com/challenge", expected))
        assertNull(parseThreeDSReturnUrl("https://acs.bank.com/stepup?id=123", expected))
        // Query param injection on unrelated origin
        assertNull(parseThreeDSReturnUrl("https://attacker.com?return=paymentsdk://3ds-complete", expected))
        assertNull(parseThreeDSReturnUrl("https://bank.com/3ds-complete", expected))
    }

    @Test
    fun testParseThreeDSReturnUrlSuccessVariants() {
        val expected = "paymentsdk://3ds-complete"

        // transStatus = Y (EMV 3DS Success)
        val res2 = parseThreeDSReturnUrl("paymentsdk://3ds-complete?transStatus=Y&payment_intent=pi_123", expected)
        assertNotNull(res2)
        assertTrue(res2 is ThreeDSResult.Completed)
        assertEquals("paymentsdk://3ds-complete?transStatus=Y&payment_intent=pi_123", res2.returnPayload)

        // transStatus = A (EMV 3DS Attempted / Proof Generated)
        val res3 = parseThreeDSReturnUrl("paymentsdk://3ds-complete?transStatus=A", expected)
        assertNotNull(res3)
        assertTrue(res3 is ThreeDSResult.Completed)

        // Case-insensitive URL scheme/host match
        val res3Case = parseThreeDSReturnUrl("PaymentSDK://3DS-COMPLETE?transStatus=Y", expected)
        assertNotNull(res3Case)
        assertTrue(res3Case is ThreeDSResult.Completed)

        // status = succeeded / success
        val res4 = parseThreeDSReturnUrl("paymentsdk://3ds-complete?status=succeeded", expected)
        assertNotNull(res4)
        assertTrue(res4 is ThreeDSResult.Completed)

        val res5 = parseThreeDSReturnUrl("paymentsdk://3ds-complete?status=success", expected)
        assertNotNull(res5)
        assertTrue(res5 is ThreeDSResult.Completed)
    }

    @Test
    fun testParseThreeDSReturnUrlDeclineAndFailureVariants() {
        val expected = "paymentsdk://3ds-complete"

        // transStatus = N (Not authenticated / Denied)
        val res1 = parseThreeDSReturnUrl("paymentsdk://3ds-complete?transStatus=N", expected)
        assertNotNull(res1)
        assertTrue(res1 is ThreeDSResult.Failed)
        assertEquals(PaymentErrorCode.AUTHENTICATION_FAILED, res1.errorCode)

        // transStatus = R (Rejected)
        val res2 = parseThreeDSReturnUrl("paymentsdk://3ds-complete?transStatus=R", expected)
        assertNotNull(res2)
        assertTrue(res2 is ThreeDSResult.Failed)

        // transStatus = U (Unavailable / System Error)
        val res3 = parseThreeDSReturnUrl("paymentsdk://3ds-complete?transStatus=U", expected)
        assertNotNull(res3)
        assertTrue(res3 is ThreeDSResult.Failed)

        // status = declined / failed
        val res4 = parseThreeDSReturnUrl("paymentsdk://3ds-complete?status=declined", expected)
        assertNotNull(res4)
        assertTrue(res4 is ThreeDSResult.Failed)

        val res5 = parseThreeDSReturnUrl("paymentsdk://3ds-complete?status=failed", expected)
        assertNotNull(res5)
        assertTrue(res5 is ThreeDSResult.Failed)

        // error with description - maps safely without leaking attacker copy
        val res6 = parseThreeDSReturnUrl("paymentsdk://3ds-complete?error=invalid_otp&error_description=Call%201800%20Phishing", expected)
        assertNotNull(res6)
        assertTrue(res6 is ThreeDSResult.Failed)
        assertEquals(PaymentErrorCode.AUTHENTICATION_FAILED, res6.errorCode)
    }

    @Test
    fun testParseThreeDSReturnUrlFailsClosedOnIndeterminateOrBareUrl() {
        val expected = "paymentsdk://3ds-complete"

        // Bare return URL with no success indicators fails closed
        val bareRes = parseThreeDSReturnUrl("paymentsdk://3ds-complete", expected)
        assertNotNull(bareRes)
        assertTrue(bareRes is ThreeDSResult.Failed)
        assertEquals(PaymentErrorCode.AUTHENTICATION_FAILED, bareRes.errorCode)

        // transStatus = C (Challenge required, incomplete redirect) fails closed
        val cRes = parseThreeDSReturnUrl("paymentsdk://3ds-complete?transStatus=C", expected)
        assertNotNull(cRes)
        assertTrue(cRes is ThreeDSResult.Failed)

        // transStatus = D or I (Decoupled / Informational) fails closed
        val dRes = parseThreeDSReturnUrl("paymentsdk://3ds-complete?transStatus=D", expected)
        assertNotNull(dRes)
        assertTrue(dRes is ThreeDSResult.Failed)

        // Unknown parameters fail closed
        val unknownRes = parseThreeDSReturnUrl("paymentsdk://3ds-complete?foo=bar", expected)
        assertNotNull(unknownRes)
        assertTrue(unknownRes is ThreeDSResult.Failed)
    }

    @Test
    fun testParseThreeDSReturnUrlCanceledVariants() {
        val expected = "paymentsdk://3ds-complete"

        val res1 = parseThreeDSReturnUrl("paymentsdk://3ds-complete?status=canceled", expected)
        assertEquals(ThreeDSResult.Canceled, res1)

        val res2 = parseThreeDSReturnUrl("paymentsdk://3ds-complete?status=cancelled", expected)
        assertEquals(ThreeDSResult.Canceled, res2)

        val res3 = parseThreeDSReturnUrl("paymentsdk://3ds-complete?canceled=true", expected)
        assertEquals(ThreeDSResult.Canceled, res3)
    }

    @Test
    fun testParseThreeDSReturnUrlWithHttpUrl() {
        val expected = "https://example.com/checkout/return"

        val res1 = parseThreeDSReturnUrl("https://example.com/checkout/return?status=succeeded", expected)
        assertNotNull(res1)
        assertTrue(res1 is ThreeDSResult.Completed)

        val res2 = parseThreeDSReturnUrl("https://example.com/checkout/return?transStatus=N", expected)
        assertNotNull(res2)
        assertTrue(res2 is ThreeDSResult.Failed)

        assertNull(parseThreeDSReturnUrl("https://other.com/checkout/return", expected))
    }
}
