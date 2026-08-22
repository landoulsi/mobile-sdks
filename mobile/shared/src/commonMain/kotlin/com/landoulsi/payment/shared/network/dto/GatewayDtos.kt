package com.landoulsi.payment.shared.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ─────────────────────────────────────────────────────────────
//  Google Pay token DTOs
// ─────────────────────────────────────────────────────────────

/**
 * Top-level Google Pay payment token returned by the Wallet API inside `paymentData.paymentMethodData.tokenizationData`.
 *
 * The `token` field is a JSON string that must be decoded separately using [GooglePayGatewayToken]
 * (for PAYMENT_GATEWAY tokenization) or [GooglePayDirectToken] (for DIRECT tokenization).
 *
 * @property type Tokenization type: `"PAYMENT_GATEWAY"` or `"DIRECT"`.
 * @property token JSON-encoded string containing the actual payment credentials.
 */
@Serializable
data class GooglePayTokenizationData(
    @SerialName("type") val type: String,
    @SerialName("token") val token: String
)

/**
 * Decoded gateway token payload for PAYMENT_GATEWAY tokenization (e.g., Stripe, Adyen).
 *
 * This is the JSON object found inside [GooglePayTokenizationData.token] when
 * `tokenizationData.type == "PAYMENT_GATEWAY"`.
 *
 * @property id The gateway-specific payment method token (e.g., Stripe `pm_xxx` or Adyen token).
 * @property object_ The object type string, e.g., `"token"` for Stripe.
 */
@Serializable
data class GooglePayGatewayToken(
    @SerialName("id") val id: String,
    @SerialName("object") val `object`: String? = null
)

/**
 * Decoded ECv2 direct token payload for DIRECT tokenization.
 *
 * When the tokenization spec is [com.landoulsi.payment.shared.model.GooglePayTokenizationSpecification.Direct],
 * the [GooglePayTokenizationData.token] field contains this encrypted payload.
 *
 * @property protocolVersion Encryption protocol version, e.g., `"ECv2"`.
 * @property signature Base-64 encoded ECDSA signature.
 * @property signedMessage Base-64 encoded signed message payload.
 * @property intermediateSigningKey Optional intermediate signing key for ECv2.
 */
@Serializable
data class GooglePayDirectToken(
    @SerialName("protocolVersion") val protocolVersion: String,
    @SerialName("signature") val signature: String,
    @SerialName("signedMessage") val signedMessage: String,
    @SerialName("intermediateSigningKey") val intermediateSigningKey: GooglePayIntermediateSigningKey? = null
)

/**
 * Intermediate signing key used in DIRECT (ECv2) tokenization.
 *
 * @property signedKey The base-64 encoded signed key.
 * @property signatures List of ECDSA signatures for the signed key.
 */
@Serializable
data class GooglePayIntermediateSigningKey(
    @SerialName("signedKey") val signedKey: String,
    @SerialName("signatures") val signatures: List<String> = emptyList()
)

/**
 * Parsed card info returned in `paymentData.paymentMethodData.info`.
 *
 * @property cardNetwork The card network (e.g., `"VISA"`, `"MASTERCARD"`).
 * @property cardDetails The last 4 digits of the card number.
 * @property billingAddress Optional billing address from the Google Pay sheet.
 */
@Serializable
data class GooglePayCardInfo(
    @SerialName("cardNetwork") val cardNetwork: String? = null,
    @SerialName("cardDetails") val cardDetails: String? = null,
    @SerialName("billingAddress") val billingAddress: GooglePayBillingAddress? = null
)

/**
 * Billing address as returned by the Google Pay API.
 *
 * @property name Full cardholder name.
 * @property address1 First line of street address.
 * @property address2 Second line of street address.
 * @property address3 Third line of street address.
 * @property locality City / locality.
 * @property administrativeArea State / province / region.
 * @property countryCode Two-letter ISO 3166-1 alpha-2 country code.
 * @property postalCode Postal code.
 * @property sortingCode Sorting code (used in some countries).
 * @property phoneNumber Phone number, if requested.
 */
@Serializable
data class GooglePayBillingAddress(
    @SerialName("name") val name: String? = null,
    @SerialName("address1") val address1: String? = null,
    @SerialName("address2") val address2: String? = null,
    @SerialName("address3") val address3: String? = null,
    @SerialName("locality") val locality: String? = null,
    @SerialName("administrativeArea") val administrativeArea: String? = null,
    @SerialName("countryCode") val countryCode: String? = null,
    @SerialName("postalCode") val postalCode: String? = null,
    @SerialName("sortingCode") val sortingCode: String? = null,
    @SerialName("phoneNumber") val phoneNumber: String? = null
)

// ─────────────────────────────────────────────────────────────
//  Card payment request / response DTOs
// ─────────────────────────────────────────────────────────────

/**
 * Card tokenization request payload sent to a payment gateway.
 *
 * @property number The card number (PAN). Must be transmitted only over TLS.
 * @property expiryMonth Two-digit expiry month (01–12).
 * @property expiryYear Four-digit expiry year (e.g., 2028).
 * @property cvc Card verification code.
 * @property cardholderName Optional cardholder name.
 */
@Serializable
data class CardTokenRequest(
    @SerialName("number") val number: String,
    @SerialName("exp_month") val expiryMonth: Int,
    @SerialName("exp_year") val expiryYear: Int,
    @SerialName("cvc") val cvc: String,
    @SerialName("name") val cardholderName: String? = null
) {
    override fun toString(): String {
        val maskedNum = com.landoulsi.payment.shared.validation.CardValidation.maskCardNumber(number)
        val nameStr = if (cardholderName != null) ", name=$cardholderName" else ""
        return "CardTokenRequest(number=$maskedNum, expiryMonth=$expiryMonth, expiryYear=$expiryYear, cvc=[REDACTED]$nameStr)"
    }
}

/**
 * Card tokenization response returned by the payment gateway.
 *
 * @property id The token identifier (e.g., `"tok_xxx"` for Stripe, `"pm_xxx"` for payment methods).
 * @property object_ Object type string, typically `"token"` or `"payment_method"`.
 * @property created Unix timestamp when the token was created.
 * @property livemode Whether this is a production token.
 * @property type The token type, e.g., `"card"`.
 * @property card Embedded card details returned alongside the token.
 */
@Serializable
data class CardTokenResponse(
    @SerialName("id") val id: String,
    @SerialName("object") val `object`: String? = null,
    @SerialName("created") val created: Long? = null,
    @SerialName("livemode") val livemode: Boolean = false,
    @SerialName("type") val type: String? = null,
    @SerialName("card") val card: CardDetails? = null
)

/**
 * Card details embedded in a gateway token response.
 *
 * @property id Card resource identifier.
 * @property brand The card brand, e.g., `"visa"`, `"mastercard"`.
 * @property last4 Last four digits of the card number.
 * @property expMonth Expiry month.
 * @property expYear Expiry year.
 * @property funding Funding type: `"credit"`, `"debit"`, `"prepaid"`, `"unknown"`.
 * @property country Two-letter ISO 3166-1 country code of the card issuer.
 */
@Serializable
data class CardDetails(
    @SerialName("id") val id: String? = null,
    @SerialName("brand") val brand: String? = null,
    @SerialName("last4") val last4: String? = null,
    @SerialName("exp_month") val expMonth: Int? = null,
    @SerialName("exp_year") val expYear: Int? = null,
    @SerialName("funding") val funding: String? = null,
    @SerialName("country") val country: String? = null
)

// ─────────────────────────────────────────────────────────────
//  Generic gateway error DTO
// ─────────────────────────────────────────────────────────────

/**
 * Wraps a gateway error response body, commonly shared across Stripe-compatible APIs.
 *
 * @property error The error detail object.
 */
@Serializable
data class GatewayErrorResponse(
    @SerialName("error") val error: GatewayError? = null
)

/**
 * Details of a gateway API error.
 *
 * @property type Error type (e.g., `"card_error"`, `"invalid_request_error"`).
 * @property code Machine-readable error code (e.g., `"card_declined"`, `"expired_card"`).
 * @property message Human-readable error description.
 * @property param The request parameter that caused the error, if applicable.
 * @property declineCode Decline code for card errors.
 */
@Serializable
data class GatewayError(
    @SerialName("type") val type: String? = null,
    @SerialName("code") val code: String? = null,
    @SerialName("message") val message: String? = null,
    @SerialName("param") val param: String? = null,
    @SerialName("decline_code") val declineCode: String? = null
)

// ─────────────────────────────────────────────────────────────
//  PaymentIntent & 3D Secure DTOs
// ─────────────────────────────────────────────────────────────

/**
 * Request payload for confirming a PaymentIntent on the gateway.
 *
 * @property paymentMethodId Payment method identifier (e.g., `"pm_xxx"` or `"tok_xxx"`).
 * @property clientSecret The client secret associated with the PaymentIntent.
 * @property returnUrl The URL to redirect the shopper back to after 3DS authentication.
 */
@Serializable
data class PaymentIntentConfirmRequest(
    @SerialName("payment_method") val paymentMethodId: String? = null,
    @SerialName("client_secret") val clientSecret: String? = null,
    @SerialName("return_url") val returnUrl: String? = null
) {
    /**
     * Redacted representation that never exposes the [clientSecret].
     */
    override fun toString(): String {
        return "PaymentIntentConfirmRequest(paymentMethodId=$paymentMethodId, clientSecret=[REDACTED], returnUrl=$returnUrl)"
    }
}

/**
 * Next action descriptor returned when a PaymentIntent requires 3DS or customer action.
 *
 * @property type Action type (e.g., `"redirect_to_url"`, `"use_stripe_sdk"`).
 * @property redirectToUrl Details for web-based redirect flow.
 * @property useStripeSdk Details for 3DS2 / SDK challenge flow.
 */
@Serializable
data class NextAction(
    @SerialName("type") val type: String? = null,
    @SerialName("redirect_to_url") val redirectToUrl: RedirectToUrl? = null,
    @SerialName("use_stripe_sdk") val useStripeSdk: ThreeDSChallengeData? = null
)

/**
 * Redirect parameters when next action is web-based 3DS redirect.
 *
 * @property url The authentication URL to load in the browser / WebView.
 * @property returnUrl The URL or scheme that the gateway redirects back to upon completion.
 */
@Serializable
data class RedirectToUrl(
    @SerialName("url") val url: String,
    @SerialName("return_url") val returnUrl: String? = null
)

/**
 * 3DS2 challenge parameters for native/SDK authentication.
 *
 * @property acsUrl Access Control Server URL.
 * @property cReq Challenge request payload.
 * @property threeDSServerTransId 3DS Server transaction identifier.
 * @property stripeJs Fallback or helper URL for Stripe.js / 3DS.
 */
@Serializable
data class ThreeDSChallengeData(
    @SerialName("acs_url") val acsUrl: String? = null,
    @SerialName("creq") val cReq: String? = null,
    @SerialName("three_d_s_server_trans_id") val threeDSServerTransId: String? = null,
    @SerialName("stripe_js") val stripeJs: String? = null
) {
    /**
     * Redacted representation that never exposes the challenge request payload [cReq].
     */
    override fun toString(): String {
        return "ThreeDSChallengeData(acsUrl=$acsUrl, cReq=[REDACTED], threeDSServerTransId=$threeDSServerTransId, stripeJs=$stripeJs)"
    }
}

/**
 * Response returned from confirming or retrieving a PaymentIntent.
 *
 * @property id The PaymentIntent identifier (e.g., `"pi_xxx"`).
 * @property object_ Object type string, typically `"payment_intent"`.
 * @property status Current PaymentIntent status (e.g., `"succeeded"`, `"requires_action"`, `"requires_payment_method"`).
 * @property clientSecret The client secret for confirming / authenticating the PaymentIntent.
 * @property nextAction Next action details if status is `"requires_action"`.
 * @property paymentMethod The payment method token or ID attached to this intent.
 * @property amount The amount in minor units (e.g., cents).
 * @property currency Three-letter ISO currency code.
 * @property lastPaymentError Details of the latest error if the intent failed.
 */
@Serializable
data class PaymentIntentConfirmResponse(
    @SerialName("id") val id: String,
    @SerialName("object") val `object`: String? = null,
    @SerialName("status") val status: String,
    @SerialName("client_secret") val clientSecret: String? = null,
    @SerialName("next_action") val nextAction: NextAction? = null,
    @SerialName("payment_method") val paymentMethod: String? = null,
    @SerialName("amount") val amount: Long? = null,
    @SerialName("currency") val currency: String? = null,
    @SerialName("last_payment_error") val lastPaymentError: GatewayError? = null
)

