package com.landoulsi.payment.shared.provider

import com.landoulsi.payment.shared.model.PaymentMethodType
import com.landoulsi.payment.shared.model.PaymentRequest
import com.landoulsi.payment.shared.model.PaymentResult

/**
 * Common abstraction for payment providers (e.g. Google Pay, Apple Pay, Card, PayPal).
 * Platform implementations wrap native SDKs (e.g. Google Play Services Wallet on Android, PassKit on iOS).
 */
interface PaymentProvider {

    /**
     * The payment method type handled by this provider.
     */
    val paymentMethodType: PaymentMethodType

    /**
     * Checks if this payment provider is available and ready to process payments on the current device.
     */
    suspend fun isReadyToPay(): Boolean

    /**
     * Executes the payment flow for the given [request].
     *
     * @param request Payment details including amount, currency, and configuration.
     * @return [PaymentResult.Success], [PaymentResult.Failure], or [PaymentResult.Canceled].
     */
    suspend fun pay(request: PaymentRequest): PaymentResult
}
