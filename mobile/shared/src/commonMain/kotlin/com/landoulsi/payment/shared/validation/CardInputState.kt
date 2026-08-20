package com.landoulsi.payment.shared.validation

import com.landoulsi.payment.shared.model.CardNetwork

/**
 * Typed validation error for card input fields.
 * Resolved to localized strings at the UI layer via string resources.
 */
enum class CardFieldError {
    /** The field is empty but required. */
    REQUIRED,
    /** The field has incomplete input (not enough characters). */
    INCOMPLETE,
    /** The card number failed the Luhn check. */
    INVALID_CARD_NUMBER,
    /** The expiry date is in the past or has an invalid month. */
    INVALID_EXPIRY,
    /** The CVC failed validation for the detected card network. */
    INVALID_CVC
}

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
    val cardholderName: String = "",
    val submissionAttempted: Boolean = false
) {
    val isFormValid: Boolean
        get() = number.isValid && expiry.isValid && cvc.isValid && expiry.month != null && expiry.year != null

    val isFormComplete: Boolean
        get() = number.isComplete && expiry.isComplete && cvc.isComplete && expiry.month != null && expiry.year != null

    /**
     * Returns a typed validation error for the card number field considering both
     * completion-based errors (Luhn failure) and submission-attempted errors
     * (incomplete or empty field).
     */
    val numberDisplayError: CardFieldError?
        get() = when {
            number.error != null && (number.isComplete || submissionAttempted) -> CardFieldError.INVALID_CARD_NUMBER
            submissionAttempted && number.rawValue.isBlank() -> CardFieldError.REQUIRED
            submissionAttempted && !number.isComplete -> CardFieldError.INCOMPLETE
            else -> null
        }

    /**
     * Returns a typed validation error for the expiry field considering both
     * completion-based errors and submission-attempted errors.
     */
    val expiryDisplayError: CardFieldError?
        get() = when {
            expiry.error != null && (expiry.isComplete || submissionAttempted) -> CardFieldError.INVALID_EXPIRY
            submissionAttempted && expiry.rawValue.isBlank() -> CardFieldError.REQUIRED
            submissionAttempted && !expiry.isComplete -> CardFieldError.INCOMPLETE
            else -> null
        }

    /**
     * Returns a typed validation error for the CVC field considering both
     * completion-based errors and submission-attempted errors.
     */
    val cvcDisplayError: CardFieldError?
        get() = when {
            cvc.error != null && (cvc.isComplete || submissionAttempted) -> CardFieldError.INVALID_CVC
            submissionAttempted && cvc.rawValue.isBlank() -> CardFieldError.REQUIRED
            submissionAttempted && !cvc.isComplete -> CardFieldError.INCOMPLETE
            else -> null
        }

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
     * Marks the form as having attempted submission, enabling inline error display
     * for fields that are not yet complete.
     */
    fun markSubmissionAttempted(): CardFormState = copy(submissionAttempted = true)

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