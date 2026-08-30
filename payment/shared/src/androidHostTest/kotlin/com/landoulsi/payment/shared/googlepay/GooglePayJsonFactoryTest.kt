package com.landoulsi.payment.shared.googlepay

import com.landoulsi.payment.shared.model.CardNetwork
import com.landoulsi.payment.shared.model.Currency
import com.landoulsi.payment.shared.model.GooglePayAuthMethod
import com.landoulsi.payment.shared.model.GooglePayBillingAddressFormat
import com.landoulsi.payment.shared.model.GooglePayBillingAddressParameters
import com.landoulsi.payment.shared.model.GooglePayConfig
import com.landoulsi.payment.shared.model.GooglePayEnvironment
import com.landoulsi.payment.shared.model.GooglePayShippingAddressParameters
import com.landoulsi.payment.shared.model.GooglePayTokenizationSpecification
import com.landoulsi.payment.shared.model.Money
import com.landoulsi.payment.shared.model.PaymentErrorCode
import com.landoulsi.payment.shared.model.PaymentMethodType
import com.landoulsi.payment.shared.model.PaymentRequest
import com.landoulsi.payment.shared.model.PaymentResult
import org.json.JSONObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GooglePayJsonFactoryTest {

    private val testConfig = GooglePayConfig(
        environment = GooglePayEnvironment.TEST,
        merchantId = "test-merchant-123",
        merchantName = "Acme Store",
        allowedCardNetworks = listOf(CardNetwork.VISA, CardNetwork.MASTERCARD, CardNetwork.AMEX),
        allowedAuthMethods = listOf(GooglePayAuthMethod.PAN_ONLY, GooglePayAuthMethod.CRYPTOGRAM_3DS),
        tokenizationSpecification = GooglePayTokenizationSpecification.Gateway.stripe(
            publishableKey = "pk_test_12345",
            stripeVersion = "2020-08-27"
        ),
        allowPrepaidCards = true,
        allowCreditCards = true,
        billingAddressRequired = true,
        billingAddressParameters = GooglePayBillingAddressParameters(
            format = GooglePayBillingAddressFormat.FULL,
            phoneNumberRequired = true
        ),
        emailRequired = true,
        shippingAddressRequired = true,
        shippingAddressParameters = GooglePayShippingAddressParameters(
            allowedCountryCodes = listOf("US", "CA"),
            phoneNumberRequired = true
        )
    )

    @Test
    fun testCreateBaseRequest() {
        val base = GooglePayJsonFactory.createBaseRequest()
        assertEquals(2, base.getInt("apiVersion"))
        assertEquals(0, base.getInt("apiVersionMinor"))
    }

    @Test
    fun testCreateTokenizationSpecificationGateway() {
        val spec = GooglePayTokenizationSpecification.Gateway.stripe("pk_test_123")
        val json = GooglePayJsonFactory.createTokenizationSpecification(spec)

        assertEquals("PAYMENT_GATEWAY", json.getString("type"))
        val params = json.getJSONObject("parameters")
        assertEquals("stripe", params.getString("gateway"))
        assertEquals("pk_test_123", params.getString("stripe:publishableKey"))
    }

    @Test
    fun testCreateTokenizationSpecificationDirect() {
        val spec = GooglePayTokenizationSpecification.Direct(
            publicKey = "BO19OJ0A...",
            protocolVersion = "ECv2"
        )
        val json = GooglePayJsonFactory.createTokenizationSpecification(spec)

        assertEquals("DIRECT", json.getString("type"))
        val params = json.getJSONObject("parameters")
        assertEquals("ECv2", params.getString("protocolVersion"))
        assertEquals("BO19OJ0A...", params.getString("publicKey"))
    }

    @Test
    fun testCreateCardPaymentMethod() {
        val cardPaymentMethod = GooglePayJsonFactory.createCardPaymentMethod(testConfig, includeTokenization = true)
        assertEquals("CARD", cardPaymentMethod.getString("type"))

        val params = cardPaymentMethod.getJSONObject("parameters")
        val authMethods = params.getJSONArray("allowedAuthMethods")
        assertEquals(2, authMethods.length())
        assertEquals("PAN_ONLY", authMethods.getString(0))
        assertEquals("CRYPTOGRAM_3DS", authMethods.getString(1))

        val networks = params.getJSONArray("allowedCardNetworks")
        assertEquals(3, networks.length())
        assertEquals("VISA", networks.getString(0))
        assertEquals("MASTERCARD", networks.getString(1))
        assertEquals("AMEX", networks.getString(2))

        assertTrue(params.getBoolean("billingAddressRequired"))
        val billingParams = params.getJSONObject("billingAddressParameters")
        assertEquals("FULL", billingParams.getString("format"))
        assertTrue(billingParams.getBoolean("phoneNumberRequired"))

        assertTrue(cardPaymentMethod.has("tokenizationSpecification"))
    }

    @Test
    fun testCreateIsReadyToPayRequest() {
        val readyRequest = GooglePayJsonFactory.createIsReadyToPayRequest(testConfig, existingPaymentMethodRequired = true)
        assertEquals(2, readyRequest.getInt("apiVersion"))
        assertEquals(0, readyRequest.getInt("apiVersionMinor"))
        assertTrue(readyRequest.getBoolean("existingPaymentMethodRequired"))

        val paymentMethods = readyRequest.getJSONArray("allowedPaymentMethods")
        assertEquals(1, paymentMethods.length())
        val cardMethod = paymentMethods.getJSONObject(0)
        assertEquals("CARD", cardMethod.getString("type"))
        assertFalse(cardMethod.has("tokenizationSpecification"))
    }

    @Test
    fun testCreatePaymentDataRequest() {
        val paymentRequest = PaymentRequest(
            id = "order_abc_123",
            amount = Money.fromMajorUnits(49.99, Currency.USD),
            merchantName = "Acme Store Override",
            googlePayConfig = testConfig
        )

        val paymentDataRequest = GooglePayJsonFactory.createPaymentDataRequest(paymentRequest)

        assertEquals(2, paymentDataRequest.getInt("apiVersion"))
        assertEquals(0, paymentDataRequest.getInt("apiVersionMinor"))

        val transactionInfo = paymentDataRequest.getJSONObject("transactionInfo")
        assertEquals("49.99", transactionInfo.getString("totalPrice"))
        assertEquals("FINAL", transactionInfo.getString("totalPriceStatus"))
        assertEquals("USD", transactionInfo.getString("currencyCode"))

        val merchantInfo = paymentDataRequest.getJSONObject("merchantInfo")
        assertEquals("test-merchant-123", merchantInfo.getString("merchantId"))
        assertEquals("Acme Store Override", merchantInfo.getString("merchantName"))

        assertTrue(paymentDataRequest.getBoolean("emailRequired"))
        assertTrue(paymentDataRequest.getBoolean("shippingAddressRequired"))

        val shippingParams = paymentDataRequest.getJSONObject("shippingAddressParameters")
        val countryCodes = shippingParams.getJSONArray("allowedCountryCodes")
        assertEquals("US", countryCodes.getString(0))
        assertEquals("CA", countryCodes.getString(1))
        assertTrue(shippingParams.getBoolean("phoneNumberRequired"))
    }

    @Test
    fun testParsePaymentResultSuccess() {
        val sampleResponseJson = """
            {
              "apiVersion": 2,
              "apiVersionMinor": 0,
              "paymentMethodData": {
                "description": "Visa •••• 1234",
                "tokenizationData": {
                  "type": "PAYMENT_GATEWAY",
                  "token": "{\"id\":\"tok_123456789\"}"
                },
                "type": "CARD",
                "info": {
                  "cardNetwork": "VISA",
                  "cardDetails": "1234",
                  "billingAddress": {
                    "name": "Jane Doe",
                    "address1": "123 Market St",
                    "address2": "Suite 400",
                    "locality": "San Francisco",
                    "administrativeArea": "CA",
                    "countryCode": "US",
                    "postalCode": "94105",
                    "phoneNumber": "+14155552671"
                  }
                }
              },
              "email": "janedoe@example.com",
              "shippingAddress": {
                "name": "Jane Doe",
                "address1": "123 Market St",
                "address2": "Suite 400",
                "locality": "San Francisco",
                "administrativeArea": "CA",
                "countryCode": "US",
                "postalCode": "94105",
                "phoneNumber": "+14155552671"
              }
            }
        """.trimIndent()

        val result = GooglePayJsonFactory.parsePaymentResult(sampleResponseJson, "order_123")
        assertTrue(result is PaymentResult.Success)

        assertEquals("order_123", result.transactionId)
        assertEquals(PaymentMethodType.GOOGLE_PAY, result.paymentMethodType)
        assertEquals("{\"id\":\"tok_123456789\"}", result.token)
        assertEquals("1234", result.last4)
        assertEquals(CardNetwork.VISA, result.cardNetwork)
        assertEquals("janedoe@example.com", result.email)

        assertNotNull(result.billingAddress)
        assertEquals("Jane Doe", result.billingAddress.name)
        assertEquals("123 Market St", result.billingAddress.address1)
        assertEquals("Suite 400", result.billingAddress.address2)
        assertEquals("San Francisco", result.billingAddress.city)
        assertEquals("CA", result.billingAddress.state)
        assertEquals("US", result.billingAddress.countryCode)
        assertEquals("94105", result.billingAddress.postalCode)
        assertEquals("+14155552671", result.billingAddress.phoneNumber)

        assertNotNull(result.shippingAddress)
        assertEquals("Jane Doe", result.shippingAddress.name)
        assertEquals("123 Market St", result.shippingAddress.address1)
    }

    @Test
    fun testParsePaymentResultMalformedJson() {
        val result = GooglePayJsonFactory.parsePaymentResult("not a json", "order_123")
        assertTrue(result is PaymentResult.Failure)
        assertEquals(PaymentErrorCode.CONFIGURATION_ERROR, result.errorCode)
    }

    @Test
    fun testCreatePaymentDataRequestOmitEmptyMerchantInfo() {
        val noMerchantConfig = testConfig.copy(merchantId = "", merchantName = "")
        val request = PaymentRequest(
            id = "order_no_merchant",
            amount = Money.fromMajorUnits(10.0, Currency.USD),
            merchantName = null,
            googlePayConfig = noMerchantConfig
        )
        val json = GooglePayJsonFactory.createPaymentDataRequest(request)
        assertFalse(json.has("merchantInfo"))
    }

    @Test
    fun testParseAddressWithAddress3() {
        val addressJson = JSONObject().apply {
            put("name", "International User")
            put("address1", "Line 1")
            put("address2", "Line 2")
            put("address3", "Line 3")
            put("locality", "Tokyo")
            put("countryCode", "JP")
        }
        val address = GooglePayJsonFactory.parseAddress(addressJson)
        assertEquals("Line 1", address.address1)
        assertEquals("Line 2, Line 3", address.address2)
        assertEquals("Tokyo", address.city)
    }

    @Test
    fun testParseAddressWithOnlyAddress2() {
        val addressJson = JSONObject().apply {
            put("name", "Flat User")
            put("address1", "10 Downing St")
            put("address2", "Apt 5")
            put("locality", "London")
            put("administrativeArea", "England")
            put("countryCode", "GB")
            put("postalCode", "SW1A 1AA")
        }
        val address = GooglePayJsonFactory.parseAddress(addressJson)
        assertEquals("10 Downing St", address.address1)
        assertEquals("Apt 5", address.address2)
        assertEquals("London", address.city)
        assertEquals("England", address.state)
        assertEquals("GB", address.countryCode)
        assertEquals("SW1A 1AA", address.postalCode)
    }

    @Test
    fun testParseAddressEmpty() {
        val addressJson = JSONObject()
        val address = GooglePayJsonFactory.parseAddress(addressJson)
        assertNull(address.name)
        assertNull(address.address1)
        assertNull(address.address2)
        assertNull(address.city)
        assertNull(address.state)
        assertNull(address.postalCode)
        assertNull(address.countryCode)
        assertNull(address.phoneNumber)
        assertNull(address.email)
    }

    @Test
    fun testCreatePaymentDataRequestNoBillingAddress() {
        val noBillingConfig = testConfig.copy(
            billingAddressRequired = false,
            billingAddressParameters = null
        )
        val paymentRequest = PaymentRequest(
            id = "order_no_billing",
            amount = Money.fromMajorUnits(5.0, Currency.USD),
            googlePayConfig = noBillingConfig
        )
        val json = GooglePayJsonFactory.createPaymentDataRequest(paymentRequest)
        val cardParams = json.getJSONArray("allowedPaymentMethods")
            .getJSONObject(0)
            .getJSONObject("parameters")
        assertFalse(cardParams.has("billingAddressRequired"))
        assertFalse(cardParams.has("billingAddressParameters"))
    }

    @Test
    fun testCreatePaymentDataRequestShippingFromRequest() {
        val noShippingConfig = testConfig.copy(
            shippingAddressRequired = false,
            shippingAddressParameters = null
        )
        val requestWithShipping = PaymentRequest(
            id = "order_ship",
            amount = Money.fromMajorUnits(15.0, Currency.USD),
            requireShipping = true,
            googlePayConfig = noShippingConfig
        )
        val json = GooglePayJsonFactory.createPaymentDataRequest(requestWithShipping)
        assertTrue(json.getBoolean("shippingAddressRequired"))
    }

    @Test
    fun testCreatePaymentDataRequestNoShipping() {
        val noShippingConfig = testConfig.copy(
            shippingAddressRequired = false,
            shippingAddressParameters = null
        )
        val requestNoShipping = PaymentRequest(
            id = "order_no_ship",
            amount = Money.fromMajorUnits(15.0, Currency.USD),
            requireShipping = false,
            googlePayConfig = noShippingConfig
        )
        val json = GooglePayJsonFactory.createPaymentDataRequest(requestNoShipping)
        assertFalse(json.has("shippingAddressRequired"))
    }

    @Test
    fun testCreatePaymentDataRequestMerchantNameFallback() {
        val requestWithNullMerchant = PaymentRequest(
            id = "order_fallback",
            amount = Money.fromMajorUnits(10.0, Currency.USD),
            merchantName = null,
            googlePayConfig = testConfig
        )
        val json = GooglePayJsonFactory.createPaymentDataRequest(requestWithNullMerchant)
        val merchantInfo = json.getJSONObject("merchantInfo")
        assertEquals("Acme Store", merchantInfo.getString("merchantName"))
    }

    @Test
    fun testCreatePaymentDataRequestNoEmailRequired() {
        val noEmailConfig = testConfig.copy(emailRequired = false)
        val request = PaymentRequest(
            id = "order_no_email",
            amount = Money.fromMajorUnits(10.0, Currency.USD),
            googlePayConfig = noEmailConfig
        )
        val json = GooglePayJsonFactory.createPaymentDataRequest(request)
        assertFalse(json.has("emailRequired"))
    }

    @Test
    fun testCreateIsReadyToPayRequestWithoutExistingPaymentMethod() {
        val readyRequest = GooglePayJsonFactory.createIsReadyToPayRequest(testConfig)
        assertEquals(2, readyRequest.getInt("apiVersion"))
        assertFalse(readyRequest.has("existingPaymentMethodRequired"))
    }

    @Test
    fun testParsePaymentResultMinimalJson() {
        val minimalJson = """
            {
              "paymentMethodData": {
                "tokenizationData": {
                  "type": "PAYMENT_GATEWAY",
                  "token": "tok_simple"
                },
                "type": "CARD",
                "info": {}
              }
            }
        """.trimIndent()
        val result = GooglePayJsonFactory.parsePaymentResult(minimalJson, "tx_min")
        assertTrue(result is PaymentResult.Success)
        assertEquals("tx_min", result.transactionId)
        assertEquals("tok_simple", result.token)
        assertNull(result.last4)
        assertNull(result.cardNetwork)
        assertNull(result.billingAddress)
        assertNull(result.shippingAddress)
        assertNull(result.email)
    }

    @Test
    fun testParsePaymentResultTopLevelEmail() {
        val json = """
            {
              "paymentMethodData": {
                "tokenizationData": {
                  "type": "PAYMENT_GATEWAY",
                  "token": "tok_email"
                },
                "type": "CARD",
                "info": {
                  "cardNetwork": "VISA",
                  "cardDetails": "1234"
                }
              },
              "email": "toplevel@example.com",
              "shippingAddress": {
                "name": "Ship User",
                "address1": "456 Oak Ave",
                "locality": "Chicago",
                "administrativeArea": "IL",
                "countryCode": "US",
                "postalCode": "60601"
              }
            }
        """.trimIndent()
        val result = GooglePayJsonFactory.parsePaymentResult(json, "tx_email")
        assertTrue(result is PaymentResult.Success)
        assertEquals("toplevel@example.com", result.email)
        assertNotNull(result.shippingAddress)
        assertEquals(CardNetwork.VISA, result.cardNetwork)
        assertEquals("1234", result.last4)
    }

    @Test
    fun testParsePaymentResultEmailNullNotReturned() {
        val json = """
            {
              "paymentMethodData": {
                "tokenizationData": {
                  "type": "PAYMENT_GATEWAY",
                  "token": "tok"
                },
                "type": "CARD",
                "info": {}
              }
            }
        """.trimIndent()
        val result = GooglePayJsonFactory.parsePaymentResult(json, "tx")
        assertTrue(result is PaymentResult.Success)
        assertNull(result.email)
    }

    @Test
    fun testCreateCardPaymentMethodWithoutTokenization() {
        val cardMethod = GooglePayJsonFactory.createCardPaymentMethod(testConfig, includeTokenization = false)
        assertEquals("CARD", cardMethod.getString("type"))
        assertFalse(cardMethod.has("tokenizationSpecification"))
    }

    @Test
    fun testCreatePaymentDataRequestWithBillingAddressMinFormat() {
        val minBillingConfig = testConfig.copy(
            billingAddressRequired = true,
            billingAddressParameters = GooglePayBillingAddressParameters(
                format = GooglePayBillingAddressFormat.MIN,
                phoneNumberRequired = false
            )
        )
        val request = PaymentRequest(
            id = "order_min_billing",
            amount = Money.fromMajorUnits(1.0, Currency.USD),
            googlePayConfig = minBillingConfig
        )
        val json = GooglePayJsonFactory.createPaymentDataRequest(request)
        val cardParams = json.getJSONArray("allowedPaymentMethods")
            .getJSONObject(0)
            .getJSONObject("parameters")
        assertTrue(cardParams.getBoolean("billingAddressRequired"))
        val billingParams = cardParams.getJSONObject("billingAddressParameters")
        assertEquals("MIN", billingParams.getString("format"))
        assertFalse(billingParams.getBoolean("phoneNumberRequired"))
    }

    @Test
    fun testCreatePaymentDataRequestShippingWithCountryCodesOnly() {
        val shippingConfig = testConfig.copy(
            shippingAddressRequired = true,
            shippingAddressParameters = GooglePayShippingAddressParameters(
                allowedCountryCodes = listOf("DE", "FR", "IT"),
                phoneNumberRequired = false
            )
        )
        val request = PaymentRequest(
            id = "order_eu_shipping",
            amount = Money.fromMajorUnits(100.0, Currency.EUR),
            googlePayConfig = shippingConfig
        )
        val json = GooglePayJsonFactory.createPaymentDataRequest(request)
        val shippingParams = json.getJSONObject("shippingAddressParameters")
        val countryCodes = shippingParams.getJSONArray("allowedCountryCodes")
        assertEquals(3, countryCodes.length())
        assertEquals("DE", countryCodes.getString(0))
        assertEquals("FR", countryCodes.getString(1))
        assertEquals("IT", countryCodes.getString(2))
        assertFalse(shippingParams.getBoolean("phoneNumberRequired"))
    }
}
