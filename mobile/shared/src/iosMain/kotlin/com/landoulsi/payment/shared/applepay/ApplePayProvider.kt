package com.landoulsi.payment.shared.applepay

import com.landoulsi.payment.shared.model.ApplePayConfig
import com.landoulsi.payment.shared.model.PaymentErrorCode
import com.landoulsi.payment.shared.model.PaymentMethodType
import com.landoulsi.payment.shared.model.PaymentRequest
import com.landoulsi.payment.shared.model.PaymentResult
import com.landoulsi.payment.shared.provider.PaymentProvider

/**
 * iOS implementation of [PaymentProvider] for Apple Pay, wrapping PassKit.
 *
 * @property config Default Apple Pay configuration for this provider instance.
 * @property client Apple Pay client wrapper over PassKit [platform.PassKit.PKPaymentAuthorizationController].
 */
class ApplePayProvider(
    val config: ApplePayConfig,
    val client: ApplePayClient = ApplePayClient.create()
) : PaymentProvider {

    override val paymentMethodType: PaymentMethodType = PaymentMethodType.APPLE_PAY

    /**
     * Checks if Apple Pay is supported and ready to process payments on this device.
     */
    override suspend fun isReadyToPay(): Boolean {
        return client.isReadyToPay(config)
    }

    /**
     * Executes the Apple Pay payment flow for the given [request].
     *
     * @param request Payment details including amount, currency, and optional request-level ApplePayConfig override.
     * @return [PaymentResult.Success], [PaymentResult.Failure], or [PaymentResult.Canceled].
     */
    override suspend fun pay(request: PaymentRequest): PaymentResult {
        val effectiveConfig = request.applePayConfig ?: config
        val effectiveRequest = if (request.applePayConfig == null) {
            request.copy(applePayConfig = effectiveConfig)
        } else {
            request
        }

        return try {
            client.presentPaymentSheet(effectiveRequest)
        } catch (e: Throwable) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            PaymentResult.Failure(
                errorCode = PaymentErrorCode.UNKNOWN,
                message = e.message ?: "Unexpected error during Apple Pay transaction",
                cause = e
            )
        }
    }
}
