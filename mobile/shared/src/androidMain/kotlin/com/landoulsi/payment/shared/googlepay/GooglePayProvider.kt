package com.landoulsi.payment.shared.googlepay

import android.content.Context
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.landoulsi.payment.shared.model.GooglePayConfig
import com.landoulsi.payment.shared.model.PaymentErrorCode
import com.landoulsi.payment.shared.model.PaymentMethodType
import com.landoulsi.payment.shared.model.PaymentRequest
import com.landoulsi.payment.shared.model.PaymentResult
import com.landoulsi.payment.shared.provider.PaymentProvider

/**
 * Android implementation of [PaymentProvider] for Google Pay, wrapping Google Play Services Wallet API.
 *
 * @property config Default Google Pay configuration for this provider instance.
 * @property client Google Pay client wrapper over Play Services [com.google.android.gms.wallet.PaymentsClient].
 */
class GooglePayProvider(
    val config: GooglePayConfig,
    val client: GooglePayClient
) : PaymentProvider {

    /**
     * Secondary constructor creating the underlying [GooglePayClient] with the given [context].
     */
    constructor(
        context: Context,
        config: GooglePayConfig
    ) : this(
        config = config,
        client = GooglePayClient.create(context, config.environment)
    )

    override val paymentMethodType: PaymentMethodType = PaymentMethodType.GOOGLE_PAY

    /**
     * Checks if Google Pay is available and enabled on the current device.
     */
    override suspend fun isReadyToPay(): Boolean {
        return client.isReadyToPay(config)
    }

    /**
     * Executes the Google Pay payment flow for the given [request].
     *
     * @param request Payment details including amount, currency, and optional request-level GooglePayConfig override.
     * @return [PaymentResult.Success], [PaymentResult.Failure], or [PaymentResult.Canceled].
     */
    override suspend fun pay(request: PaymentRequest): PaymentResult {
        val effectiveConfig = request.googlePayConfig ?: config
        val effectiveRequest = if (request.googlePayConfig == null) {
            request.copy(googlePayConfig = effectiveConfig)
        } else {
            request
        }

        return try {
            val paymentData = client.loadPaymentData(effectiveRequest)
            client.parsePaymentResult(paymentData, request.id)
        } catch (e: ApiException) {
            when (e.statusCode) {
                CommonStatusCodes.CANCELED -> {
                    PaymentResult.Canceled
                }
                CommonStatusCodes.NETWORK_ERROR -> {
                    PaymentResult.Failure(
                        errorCode = PaymentErrorCode.NETWORK_ERROR,
                        message = e.message ?: "Network error during Google Pay transaction",
                        cause = e
                    )
                }
                CommonStatusCodes.DEVELOPER_ERROR -> {
                    PaymentResult.Failure(
                        errorCode = PaymentErrorCode.CONFIGURATION_ERROR,
                        message = e.message ?: "Configuration error in Google Pay request",
                        cause = e
                    )
                }
                else -> {
                    PaymentResult.Failure(
                        errorCode = PaymentErrorCode.GATEWAY_ERROR,
                        message = e.message ?: "Google Pay transaction failed (status code: ${e.statusCode})",
                        cause = e
                    )
                }
            }
        } catch (e: Throwable) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            PaymentResult.Failure(
                errorCode = PaymentErrorCode.UNKNOWN,
                message = e.message ?: "Unexpected error during Google Pay transaction",
                cause = e
            )
        }
    }
}
