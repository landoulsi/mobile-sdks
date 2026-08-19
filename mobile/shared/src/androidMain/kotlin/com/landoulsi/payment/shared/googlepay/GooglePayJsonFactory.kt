package com.landoulsi.payment.shared.googlepay

import com.landoulsi.payment.shared.model.Address
import com.landoulsi.payment.shared.model.CardNetwork
import com.landoulsi.payment.shared.model.GooglePayBillingAddressFormat
import com.landoulsi.payment.shared.model.GooglePayConfig
import com.landoulsi.payment.shared.model.GooglePayTokenizationSpecification
import com.landoulsi.payment.shared.model.PaymentErrorCode
import com.landoulsi.payment.shared.model.PaymentMethodType
import com.landoulsi.payment.shared.model.PaymentRequest
import com.landoulsi.payment.shared.model.PaymentResult
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Builds and parses Google Pay API v2 JSON requests and responses.
 *
 * Conforms to Google Pay API specifications:
 * - apiVersion: 2, apiVersionMinor: 0
 * - Base card payment method configuration
 * - Tokenization specification (PAYMENT_GATEWAY and DIRECT)
 * - IsReadyToPay and PaymentData request payloads
 * - Parsing PaymentData JSON to [PaymentResult]
 */
object GooglePayJsonFactory {

    const val API_VERSION: Int = 2
    const val API_VERSION_MINOR: Int = 0

    /**
     * Creates the base request object containing API version headers.
     */
    fun createBaseRequest(): JSONObject {
        return JSONObject().apply {
            put("apiVersion", API_VERSION)
            put("apiVersionMinor", API_VERSION_MINOR)
        }
    }

    /**
     * Builds the CARD payment method object.
     *
     * @param config Google Pay configuration.
     * @param includeTokenization Whether to include the tokenization specification (required for payment, omitted for basic readiness check).
     */
    fun createCardPaymentMethod(
        config: GooglePayConfig,
        includeTokenization: Boolean = true
    ): JSONObject {
        val cardPaymentMethod = JSONObject().apply {
            put("type", "CARD")
            put("parameters", createCardParameters(config))
        }

        if (includeTokenization) {
            cardPaymentMethod.put(
                "tokenizationSpecification",
                createTokenizationSpecification(config.tokenizationSpecification)
            )
        }

        return cardPaymentMethod
    }

    /**
     * Builds the card parameters JSON object.
     */
    private fun createCardParameters(config: GooglePayConfig): JSONObject {
        val authMethods = JSONArray().apply {
            config.allowedAuthMethods.forEach { put(it.name) }
        }

        val cardNetworks = JSONArray().apply {
            config.allowedCardNetworks.forEach { put(it.networkName) }
        }

        return JSONObject().apply {
            put("allowedAuthMethods", authMethods)
            put("allowedCardNetworks", cardNetworks)
            put("allowPrepaidCards", config.allowPrepaidCards)
            put("allowCreditCards", config.allowCreditCards)

            if (config.billingAddressRequired) {
                put("billingAddressRequired", true)
                val billingParams = config.billingAddressParameters
                val billingParamsJson = JSONObject().apply {
                    put("format", billingParams?.format?.name ?: GooglePayBillingAddressFormat.MIN.name)
                    put("phoneNumberRequired", billingParams?.phoneNumberRequired ?: false)
                }
                put("billingAddressParameters", billingParamsJson)
            }
        }
    }

    /**
     * Builds the tokenization specification JSON object.
     */
    fun createTokenizationSpecification(spec: GooglePayTokenizationSpecification): JSONObject {
        return JSONObject().apply {
            put("type", spec.type)
            val paramsJson = JSONObject().apply {
                spec.parameters.forEach { (key, value) ->
                    put(key, value)
                }
            }
            put("parameters", paramsJson)
        }
    }

    /**
     * Builds the IsReadyToPay request JSON payload.
     *
     * @param config Google Pay configuration.
     * @param existingPaymentMethodRequired If set to true, requests whether the user has a card on file ready to pay.
     */
    fun createIsReadyToPayRequest(
        config: GooglePayConfig,
        existingPaymentMethodRequired: Boolean? = null
    ): JSONObject {
        val request = createBaseRequest().apply {
            val paymentMethods = JSONArray().apply {
                put(createCardPaymentMethod(config, includeTokenization = false))
            }
            put("allowedPaymentMethods", paymentMethods)
            if (existingPaymentMethodRequired != null) {
                put("existingPaymentMethodRequired", existingPaymentMethodRequired)
            }
        }
        return request
    }

    /**
     * Builds the PaymentDataRequest JSON payload for initiating checkout.
     *
     * @param request Payment request with amount, merchant details, and Google Pay configuration.
     */
    fun createPaymentDataRequest(request: PaymentRequest): JSONObject {
        val config = requireNotNull(request.googlePayConfig) {
            "GooglePayConfig must be provided in PaymentRequest to build PaymentDataRequest"
        }

        val paymentDataRequest = createBaseRequest().apply {
            val paymentMethods = JSONArray().apply {
                put(createCardPaymentMethod(config, includeTokenization = true))
            }
            put("allowedPaymentMethods", paymentMethods)

            // Transaction Info
            val transactionInfo = JSONObject().apply {
                put("totalPrice", request.amount.formattedAmount())
                put("totalPriceStatus", "FINAL")
                put("currencyCode", request.amount.currency.code)
            }
            put("transactionInfo", transactionInfo)

            // Merchant Info
            val merchantName = request.merchantName ?: config.merchantName
            if (config.merchantId.isNotEmpty() || merchantName.isNotEmpty()) {
                val merchantInfo = JSONObject().apply {
                    if (config.merchantId.isNotEmpty()) {
                        put("merchantId", config.merchantId)
                    }
                    if (merchantName.isNotEmpty()) {
                        put("merchantName", merchantName)
                    }
                }
                put("merchantInfo", merchantInfo)
            }

            // Email requirement
            if (config.emailRequired) {
                put("emailRequired", true)
            }

            // Shipping Address requirement
            val shippingRequired = request.requireShipping || config.shippingAddressRequired
            if (shippingRequired) {
                put("shippingAddressRequired", true)
                val shippingParams = config.shippingAddressParameters
                if (shippingParams != null) {
                    val shippingJson = JSONObject().apply {
                        if (shippingParams.allowedCountryCodes.isNotEmpty()) {
                            val countryCodes = JSONArray().apply {
                                shippingParams.allowedCountryCodes.forEach { put(it) }
                            }
                            put("allowedCountryCodes", countryCodes)
                        }
                        put("phoneNumberRequired", shippingParams.phoneNumberRequired)
                    }
                    put("shippingAddressParameters", shippingJson)
                }
            }
        }

        return paymentDataRequest
    }

    /**
     * Parses a Google Pay PaymentData response JSON string into a [PaymentResult].
     *
     * @param jsonString The raw JSON string from Google Pay.
     * @param transactionId The ID to assign to the resulting [PaymentResult.Success].
     */
    fun parsePaymentResult(jsonString: String, transactionId: String): PaymentResult {
        return try {
            val root = JSONObject(jsonString)
            val paymentMethodData = root.getJSONObject("paymentMethodData")
            val tokenizationData = paymentMethodData.optJSONObject("tokenizationData")
            val token = tokenizationData?.optString("token")?.takeIf { it.isNotEmpty() }

            val info = paymentMethodData.optJSONObject("info")
            val cardDetails = info?.optString("cardDetails")?.takeIf { it.isNotEmpty() }
            val cardNetworkStr = info?.optString("cardNetwork")?.takeIf { it.isNotEmpty() }
            val cardNetwork = cardNetworkStr?.let { CardNetwork.fromName(it) }

            val billingAddressJson = info?.optJSONObject("billingAddress")
            val billingAddress = billingAddressJson?.let { parseAddress(it) }

            val shippingAddressJson = root.optJSONObject("shippingAddress")
            val shippingAddress = shippingAddressJson?.let { parseAddress(it) }

            val email = root.optString("email").takeIf { it.isNotEmpty() }
                ?: billingAddress?.email
                ?: shippingAddress?.email

            PaymentResult.Success(
                transactionId = transactionId,
                paymentMethodType = PaymentMethodType.GOOGLE_PAY,
                token = token,
                rawPaymentData = jsonString,
                last4 = cardDetails,
                cardNetwork = cardNetwork,
                billingAddress = billingAddress,
                shippingAddress = shippingAddress,
                email = email
            )
        } catch (e: JSONException) {
            PaymentResult.Failure(
                errorCode = PaymentErrorCode.CONFIGURATION_ERROR,
                message = "Failed to parse Google Pay response JSON: ${e.message}",
                cause = e
            )
        }
    }

    /**
     * Parses an [Address] from a Google Pay address JSON object.
     */
    fun parseAddress(addressJson: JSONObject): Address {
        val addr2 = addressJson.optString("address2").takeIf { it.isNotEmpty() }
        val addr3 = addressJson.optString("address3").takeIf { it.isNotEmpty() }
        val combinedAddress2 = when {
            addr2 != null && addr3 != null -> "$addr2, $addr3"
            addr2 != null -> addr2
            addr3 != null -> addr3
            else -> null
        }

        return Address(
            name = addressJson.optString("name").takeIf { it.isNotEmpty() },
            address1 = addressJson.optString("address1").takeIf { it.isNotEmpty() },
            address2 = combinedAddress2,
            city = addressJson.optString("locality").takeIf { it.isNotEmpty() },
            state = addressJson.optString("administrativeArea").takeIf { it.isNotEmpty() },
            postalCode = addressJson.optString("postalCode").takeIf { it.isNotEmpty() },
            countryCode = addressJson.optString("countryCode").takeIf { it.isNotEmpty() },
            phoneNumber = addressJson.optString("phoneNumber").takeIf { it.isNotEmpty() },
            email = null
        )
    }
}
