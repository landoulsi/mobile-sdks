package com.landoulsi.payment.shared.model

/**
 * Standard error codes for payment operations.
 */
enum class PaymentErrorCode {
    /** An unexpected or unknown error occurred. */
    UNKNOWN,
    /** Network connection error during payment processing. */
    NETWORK_ERROR,
    /** Payment gateway returned an error. */
    GATEWAY_ERROR,
    /** SDK or merchant configuration is invalid or missing required parameters. */
    CONFIGURATION_ERROR,
    /** The requested payment method is not available on this device or environment. */
    PAYMENT_METHOD_UNAVAILABLE,
    /** The card was declined by the issuing bank or payment processor. */
    CARD_DECLINED,
    /** The card has expired. */
    EXPIRED_CARD,
    /** The account has insufficient funds. */
    INSUFFICIENT_FUNDS,
    /** 3D Secure / biometric authentication failed or was cancelled. */
    AUTHENTICATION_FAILED,
    /** The user explicitly cancelled or dismissed the payment sheet. */
    USER_CANCELED
}

/**
 * Sealed hierarchy representing the result of a payment authorization or capture attempt.
 */
sealed interface PaymentResult {

    /**
     * Successful payment authorization or tokenization.
     *
     * @property transactionId The resulting transaction or payment intent identifier.
     * @property paymentMethodType The payment method used.
     * @property token The payment method token / cryptogram returned by the wallet or gateway.
     * @property rawPaymentData The raw JSON or payload from the wallet provider if needed for custom decoding.
     * @property last4 Last 4 digits of the payment card if available.
     * @property cardNetwork Card network of the used payment card if known.
     * @property billingAddress User's billing address if requested and provided.
     * @property shippingAddress User's shipping address if requested and provided.
     * @property email User's email address if requested and provided.
     */
    data class Success(
        val transactionId: String,
        val paymentMethodType: PaymentMethodType,
        val token: String? = null,
        val rawPaymentData: String? = null,
        val last4: String? = null,
        val cardNetwork: CardNetwork? = null,
        val billingAddress: Address? = null,
        val shippingAddress: Address? = null,
        val email: String? = null
    ) : PaymentResult {
        /**
         * Redacted representation that never exposes [token] or [rawPaymentData].
         */
        override fun toString(): String {
            val maskedToken = if (token != null) "[REDACTED]" else "null"
            val maskedRaw = if (rawPaymentData != null) "[REDACTED]" else "null"
            return "PaymentResult.Success(transactionId=$transactionId, paymentMethodType=$paymentMethodType, token=$maskedToken, rawPaymentData=$maskedRaw, last4=$last4, cardNetwork=$cardNetwork)"
        }
    }

    /**
     * Payment attempt failed.
     *
     * @property errorCode Standardized error code.
     * @property message Human-readable error message.
     * @property cause Optional underlying exception or throwable.
     */
    data class Failure(
        val errorCode: PaymentErrorCode,
        val message: String,
        val cause: Throwable? = null
    ) : PaymentResult

    /**
     * The payment operation was cancelled by the user.
     */
    data object Canceled : PaymentResult
}
