package com.landoulsi.payment.shared.model

/**
 * Google Pay target environment.
 */
enum class GooglePayEnvironment {
    /** Test environment with dummy cards. */
    TEST,
    /** Production live payment environment. */
    PRODUCTION
}

/**
 * Supported Google Pay authentication methods.
 */
enum class GooglePayAuthMethod {
    /** Card on file (PAN). */
    PAN_ONLY,
    /** 3D Secure cryptogram tokenized card on device. */
    CRYPTOGRAM_3DS
}

/**
 * Google Pay billing address format.
 */
enum class GooglePayBillingAddressFormat {
    /** Minimal format: Name, country code, and postal code. */
    MIN,
    /** Full format: Name, street address, locality, region, country code, and postal code. */
    FULL
}

/**
 * Parameters for requesting billing address in Google Pay.
 */
data class GooglePayBillingAddressParameters(
    val format: GooglePayBillingAddressFormat = GooglePayBillingAddressFormat.MIN,
    val phoneNumberRequired: Boolean = false
)

/**
 * Parameters for requesting shipping address in Google Pay.
 */
data class GooglePayShippingAddressParameters(
    val allowedCountryCodes: List<String> = emptyList(),
    val phoneNumberRequired: Boolean = false
)

/**
 * Google Pay tokenization specification describing how payment credentials will be tokenized.
 */
sealed interface GooglePayTokenizationSpecification {

    val type: String
    val parameters: Map<String, String>

    /**
     * Gateway tokenization (e.g. Stripe, Adyen, Braintree, etc.).
     */
    data class Gateway(
        val gateway: String,
        val gatewayMerchantId: String,
        val extraParameters: Map<String, String> = emptyMap()
    ) : GooglePayTokenizationSpecification {
        override val type: String get() = "PAYMENT_GATEWAY"

        override val parameters: Map<String, String>
            get() = buildMap {
                put("gateway", gateway)
                if (gatewayMerchantId.isNotEmpty()) {
                    put("gatewayMerchantId", gatewayMerchantId)
                }
                putAll(extraParameters)
            }

        companion object {
            /**
             * Helper for Stripe gateway tokenization.
             */
            fun stripe(
                publishableKey: String,
                stripeVersion: String = "2020-08-27"
            ): Gateway = Gateway(
                gateway = "stripe",
                gatewayMerchantId = "",
                extraParameters = mapOf(
                    "stripe:version" to stripeVersion,
                    "stripe:publishableKey" to publishableKey
                )
            )

            /**
             * Helper for Adyen gateway tokenization.
             */
            fun adyen(gatewayMerchantId: String): Gateway = Gateway(
                gateway = "adyen",
                gatewayMerchantId = gatewayMerchantId
            )

            /**
             * Helper for Braintree gateway tokenization.
             */
            fun braintree(
                tokenizationKey: String,
                braintreeVersion: String = "v1"
            ): Gateway = Gateway(
                gateway = "braintree",
                gatewayMerchantId = "",
                extraParameters = mapOf(
                    "braintree:apiVersion" to braintreeVersion,
                    "braintree:sdkVersion" to "custom",
                    "braintree:merchantId" to tokenizationKey,
                    "braintree:clientKey" to tokenizationKey
                )
            )
        }
    }

    /**
     * Direct tokenization using merchant's public encryption keys.
     */
    data class Direct(
        val publicKey: String,
        val protocolVersion: String = "ECv2"
    ) : GooglePayTokenizationSpecification {
        override val type: String get() = "DIRECT"

        override val parameters: Map<String, String>
            get() = mapOf(
                "protocolVersion" to protocolVersion,
                "publicKey" to publicKey
            )
    }
}

/**
 * Configuration for Google Pay integration.
 *
 * @property environment Google Pay environment ([GooglePayEnvironment.TEST] or [GooglePayEnvironment.PRODUCTION]).
 * @property merchantId Google Pay merchant identifier (assigned by Google Pay Business Console).
 * @property merchantName Display name of the merchant.
 * @property allowedCardNetworks Supported card networks for Google Pay.
 * @property allowedAuthMethods Allowed payment card authentication methods.
 * @property tokenizationSpecification Tokenization specification for gateway or direct processing.
 * @property allowPrepaidCards Whether prepaid cards are allowed.
 * @property allowCreditCards Whether credit cards are allowed.
 * @property billingAddressRequired Whether a billing address is required from the user.
 * @property billingAddressParameters Configuration parameters for billing address collection.
 * @property emailRequired Whether user's email address is required.
 * @property shippingAddressRequired Whether shipping address is required.
 * @property shippingAddressParameters Configuration parameters for shipping address collection.
 */
data class GooglePayConfig(
    val environment: GooglePayEnvironment = GooglePayEnvironment.TEST,
    val merchantId: String,
    val merchantName: String,
    val allowedCardNetworks: List<CardNetwork> = listOf(
        CardNetwork.VISA,
        CardNetwork.MASTERCARD,
        CardNetwork.AMEX,
        CardNetwork.DISCOVER
    ),
    val allowedAuthMethods: List<GooglePayAuthMethod> = listOf(
        GooglePayAuthMethod.PAN_ONLY,
        GooglePayAuthMethod.CRYPTOGRAM_3DS
    ),
    val tokenizationSpecification: GooglePayTokenizationSpecification,
    val allowPrepaidCards: Boolean = true,
    val allowCreditCards: Boolean = true,
    val billingAddressRequired: Boolean = false,
    val billingAddressParameters: GooglePayBillingAddressParameters? = null,
    val emailRequired: Boolean = false,
    val shippingAddressRequired: Boolean = false,
    val shippingAddressParameters: GooglePayShippingAddressParameters? = null
)
