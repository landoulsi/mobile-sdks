package com.landoulsi.payment.shared.model

/**
 * Merchant capability options supported by Apple Pay.
 */
enum class ApplePayMerchantCapability {
    /** Supports 3D Secure protocol (required for most transactions). */
    THREE_D_SECURE,
    /** Supports EMV protocol. */
    EMV,
    /** Supports credit cards. */
    CREDIT,
    /** Supports debit cards. */
    DEBIT
}

/**
 * Supported Apple Pay shipping types.
 */
enum class ApplePayShippingType {
    SHIPPING,
    DELIVERY,
    STORE_PICKUP,
    SERVICE_PICKUP
}

/**
 * Summary item type indicating whether the amount is final or pending (e.g. estimated).
 */
enum class ApplePaySummaryItemType {
    FINAL,
    PENDING
}

/**
 * Contact field options for requesting billing and shipping contact info from Apple Pay.
 */
enum class ApplePayContactField {
    POSTAL_ADDRESS,
    EMAIL,
    PHONE_NUMBER,
    NAME,
    PHONETIC_NAME
}

/**
 * Represents a single line item in the Apple Pay payment summary.
 *
 * @property label Human-readable description of the line item (e.g. "Subtotal", "Shipping", "Grand Total").
 * @property amount The monetary amount for this item.
 * @property type Whether this line item is [ApplePaySummaryItemType.FINAL] or [ApplePaySummaryItemType.PENDING].
 */
data class ApplePaySummaryItem(
    val label: String,
    val amount: Money,
    val type: ApplePaySummaryItemType = ApplePaySummaryItemType.FINAL
)

/**
 * Functional interface for providing server-validated Apple Pay merchant sessions.
 *
 * In production web/app merchant validation, PassKit requests a merchant session by supplying
 * an Apple validation URL. The app forwards this URL to its backend, which calls Apple's Payment
 * Services server using the merchant identity certificate and returns the session payload JSON.
 */
fun interface ApplePayMerchantSessionProvider {
    /**
     * Obtains the merchant session dictionary payload from the merchant backend.
     *
     * @param validationUrl The Apple validation URL provided by PassKit.
     * @return JSON string of the merchant session payload returned by Apple servers.
     */
    suspend fun provideMerchantSession(validationUrl: String): String
}

/**
 * Configuration for Apple Pay integration.
 *
 * @property merchantIdentifier The Apple Pay Merchant Identifier configured in Apple Developer portal (e.g. "merchant.com.landoulsi.payment").
 * @property countryCode Two-letter ISO 3166-1 alpha-2 country code of the merchant (e.g. "US").
 * @property merchantCapabilities List of merchant capabilities (defaults to [ApplePayMerchantCapability.THREE_D_SECURE]).
 * @property allowedCardNetworks Supported card networks (defaults to Visa, Mastercard, Amex, Discover).
 * @property merchantSessionProvider Optional session provider for handling server-side merchant validation.
 * @property requiredBillingContactFields Contact fields required for the billing address.
 * @property requiredShippingContactFields Contact fields required for the shipping address.
 * @property shippingType The type of shipping (defaults to [ApplePayShippingType.SHIPPING]).
 * @property summaryItems Custom line items displayed on the payment sheet. If empty, a single total item is derived from the request.
 * @property supportedCountries Optional set of two-letter ISO country codes to limit card issuance countries.
 */
data class ApplePayConfig(
    val merchantIdentifier: String,
    val countryCode: String = "US",
    val merchantCapabilities: List<ApplePayMerchantCapability> = listOf(
        ApplePayMerchantCapability.THREE_D_SECURE
    ),
    val allowedCardNetworks: List<CardNetwork> = listOf(
        CardNetwork.VISA,
        CardNetwork.MASTERCARD,
        CardNetwork.AMEX,
        CardNetwork.DISCOVER
    ),
    val merchantSessionProvider: ApplePayMerchantSessionProvider? = null,
    val requiredBillingContactFields: Set<ApplePayContactField> = emptySet(),
    val requiredShippingContactFields: Set<ApplePayContactField> = emptySet(),
    val shippingType: ApplePayShippingType = ApplePayShippingType.SHIPPING,
    val summaryItems: List<ApplePaySummaryItem> = emptyList(),
    val supportedCountries: Set<String> = emptySet()
)
