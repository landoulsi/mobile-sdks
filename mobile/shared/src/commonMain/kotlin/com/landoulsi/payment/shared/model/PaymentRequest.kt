package com.landoulsi.payment.shared.model

/**
 * Encapsulates the details of a checkout transaction initiated by the client app.
 *
 * @property id Unique transaction or order identifier.
 * @property amount Total monetary amount to be charged.
 * @property merchantName Optional merchant display name for wallet sheets.
 * @property description Optional item or order description.
 * @property allowedPaymentMethods Payment methods enabled for this payment request.
 * @property googlePayConfig Configuration for Google Pay if enabled.
 * @property requireShipping Whether a shipping address is required for this transaction.
 * @property requireBillingAddress Whether a full billing address is required for this transaction.
 * @property metadata Arbitrary custom key-value metadata to accompany the payment request.
 */
data class PaymentRequest(
    val id: String,
    val amount: Money,
    val merchantName: String? = null,
    val description: String? = null,
    val allowedPaymentMethods: List<PaymentMethodType> = listOf(
        PaymentMethodType.GOOGLE_PAY,
        PaymentMethodType.CARD
    ),
    val googlePayConfig: GooglePayConfig? = null,
    val requireShipping: Boolean = false,
    val requireBillingAddress: Boolean = false,
    val metadata: Map<String, String> = emptyMap()
)
