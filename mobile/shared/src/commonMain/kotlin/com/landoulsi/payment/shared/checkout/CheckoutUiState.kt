package com.landoulsi.payment.shared.checkout

import com.landoulsi.payment.shared.model.PaymentMethodType
import com.landoulsi.payment.shared.model.PaymentRequest
import com.landoulsi.payment.shared.model.PaymentResult

/**
 * Sealed hierarchy representing the UI states of a checkout session.
 */
sealed interface CheckoutUiState {

    /** The current payment request associated with the checkout session. */
    val request: PaymentRequest

    /**
     * Initial state before provider availability check has completed.
     */
    data class Initial(
        override val request: PaymentRequest
    ) : CheckoutUiState

    /**
     * Checking wallet / payment provider readiness on the current device.
     */
    data class CheckingAvailability(
        override val request: PaymentRequest
    ) : CheckoutUiState

    /**
     * Ready for shopper interaction and payment method selection.
     *
     * @property isGooglePayAvailable Whether Google Pay is supported and ready to pay.
     * @property isApplePayAvailable Whether Apple Pay is supported and ready to pay.
     * @property availablePaymentMethods Set of all payment methods ready for use.
     */
    data class Ready(
        override val request: PaymentRequest,
        val isGooglePayAvailable: Boolean = false,
        val isApplePayAvailable: Boolean = false,
        val availablePaymentMethods: Set<PaymentMethodType> = emptySet()
    ) : CheckoutUiState

    /**
     * Payment authorization or tokenization is actively processing.
     *
     * @property paymentMethodType The payment method currently processing.
     */
    data class Processing(
        override val request: PaymentRequest,
        val paymentMethodType: PaymentMethodType
    ) : CheckoutUiState

    /**
     * Payment authorization or tokenization completed successfully.
     *
     * @property result The successful payment result details.
     */
    data class Success(
        override val request: PaymentRequest,
        val result: PaymentResult.Success
    ) : CheckoutUiState

    /**
     * Payment attempt failed with an error.
     *
     * @property failure Standardized error details.
     */
    data class Failure(
        override val request: PaymentRequest,
        val failure: PaymentResult.Failure
    ) : CheckoutUiState

    /**
     * Payment sheet or authorization was canceled/dismissed by the user.
     */
    data class Canceled(
        override val request: PaymentRequest
    ) : CheckoutUiState
}

/**
 * Convenience helper to determine if checkout is currently in a loading state.
 */
val CheckoutUiState.isLoading: Boolean
    get() = this is CheckoutUiState.CheckingAvailability || this is CheckoutUiState.Processing

/**
 * Convenience helper to determine if Google Pay is available in the current state.
 */
val CheckoutUiState.isGooglePayReady: Boolean
    get() = (this as? CheckoutUiState.Ready)?.isGooglePayAvailable == true
