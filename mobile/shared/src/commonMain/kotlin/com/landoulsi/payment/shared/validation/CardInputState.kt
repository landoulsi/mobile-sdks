package com.landoulsi.payment.shared.validation

import com.landoulsi.payment.shared.model.CardNetwork

/**
 * State for card number input field.
 */
data class CardNumberState(
    val rawValue: String = "",
    val network: CardNetwork? = null,
    val isValid: Boolean = false,
    val isComplete: Boolean = false,
    val error: String? = null
) {
    val formattedValue: String
        get() = CardValidation.formatCardNumber(rawValue, network)

    companion object {
        fun initial(): CardNumberState = CardNumberState()
    }
}

/**
 * State for expiry date input field (MM/YY).
 */
data class ExpiryState(
    val rawValue: String = "",
    val month: Int? = null,
    val year: Int? = null,
    val isValid: Boolean = false,
    val isComplete: Boolean = false,
    val error: String? = null
) {
    val formattedValue: String
        get() = CardValidation.formatExpiry(rawValue)

    companion object {
        fun initial(): ExpiryState = ExpiryState()
    }
}

/**
 * State for CVC input field.
 */
data class CvcState(
    val rawValue: String = "",
    val isValid: Boolean = false,
    val isComplete: Boolean = false,
    val error: String? = null
) {
    val formattedValue: String
        get() = rawValue

    companion object {
        fun initial(): CvcState = CvcState()
    }
}

/**
 * Combined state for the entire card input form.
 * [cardholderName] is optional in the card input form.
 */
data class CardFormState(
    val number: CardNumberState = CardNumberState.initial(),
    val expiry: ExpiryState = ExpiryState.initial(),
    val cvc: CvcState = CvcState.initial(),
    val cardholderName: String = ""
) {
    val isFormValid: Boolean
        get() = number.isValid && expiry.isValid && cvc.isValid && expiry.month != null && expiry.year != null

    val isFormComplete: Boolean
        get() = number.isComplete && expiry.isComplete && cvc.isComplete && expiry.month != null && expiry.year != null

    /**
     * Clears sensitive authentication data (CVC/CVV) from the form state while retaining
     * the entered card number and expiry for user convenience during editing.
     *
     * Per PCI-DSS Requirement 3.2, Card Verification Codes must never be retained after authorization.
     * Use [clearAll] to zero all card data including the PAN.
     */
    fun clearSensitiveData(): CardFormState = copy(
        cvc = CvcState.initial()
    )

    /**
     * Convenience alias for [clearSensitiveData] to explicitly reflect that only CVC is reset.
     */
    fun clearCvc(): CardFormState = copy(
        cvc = CvcState.initial()
    )

    /**
     * Clears all cardholder data (PAN, expiry, CVC, name) from the form state.
     */
    fun clearAll(): CardFormState = initial()

    companion object {
        fun initial(): CardFormState = CardFormState()
    }
}

/**
 * Result of updating a card number field.
 */
data class CardNumberUpdateResult(
    val newState: CardNumberState,
    val networkChanged: Boolean
)

/**
 * Result of updating an expiry field.
 */
data class ExpiryUpdateResult(
    val newState: ExpiryState
)

/**
 * Result of updating a CVC field.
 */
data class CvcUpdateResult(
    val newState: CvcState
)