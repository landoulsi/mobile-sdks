package com.landoulsi.payment.shared.model

/**
 * High-level payment method types supported by the SDK.
 */
enum class PaymentMethodType(val identifier: String) {
    GOOGLE_PAY("google_pay"),
    APPLE_PAY("apple_pay"),
    CARD("card"),
    PAYPAL("paypal"),
    KLARNA("klarna"),
    IDEAL("ideal")
}

/**
 * Sealed hierarchy representing concrete payment method inputs or selections.
 */
sealed interface PaymentMethod {
    val type: PaymentMethodType

    data class GooglePay(
        val config: GooglePayConfig
    ) : PaymentMethod {
        override val type: PaymentMethodType get() = PaymentMethodType.GOOGLE_PAY
    }

    data class ApplePay(
        val merchantId: String,
        val countryCode: String
    ) : PaymentMethod {
        override val type: PaymentMethodType get() = PaymentMethodType.APPLE_PAY
    }

    data class Card(
        val number: String,
        val expiryMonth: Int,
        val expiryYear: Int,
        val cvc: String,
        val cardholderName: String? = null
    ) : PaymentMethod {
        override val type: PaymentMethodType get() = PaymentMethodType.CARD
    }

    data class PayPal(
        val accountId: String? = null
    ) : PaymentMethod {
        override val type: PaymentMethodType get() = PaymentMethodType.PAYPAL
    }

    data class Alternative(
        override val type: PaymentMethodType,
        val parameters: Map<String, String> = emptyMap()
    ) : PaymentMethod
}
