package com.landoulsi.payment.shared.googlepay

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.wallet.AutoResolveHelper
import com.google.android.gms.wallet.IsReadyToPayRequest
import com.google.android.gms.wallet.PaymentData
import com.google.android.gms.wallet.PaymentDataRequest
import com.google.android.gms.wallet.PaymentsClient
import com.google.android.gms.wallet.Wallet
import com.google.android.gms.wallet.WalletConstants
import com.landoulsi.payment.shared.model.GooglePayConfig
import com.landoulsi.payment.shared.model.GooglePayEnvironment
import com.landoulsi.payment.shared.model.PaymentErrorCode
import com.landoulsi.payment.shared.model.PaymentRequest
import com.landoulsi.payment.shared.model.PaymentResult
import kotlinx.coroutines.tasks.await

/**
 * Client for interacting with Google Play Services Wallet API.
 */
interface GooglePayClient {

    /**
     * The underlying Google Play Services [PaymentsClient].
     */
    val paymentsClient: PaymentsClient

    /**
     * Checks if Google Pay is supported and ready to pay on the current device.
     *
     * @param config Google Pay configuration.
     * @return `true` if Google Pay is available, `false` otherwise.
     */
    suspend fun isReadyToPay(config: GooglePayConfig): Boolean

    /**
     * Checks if Google Pay is supported using a pre-constructed [IsReadyToPayRequest].
     */
    suspend fun isReadyToPay(request: IsReadyToPayRequest): Boolean

    /**
     * Constructs a Google Play Services [IsReadyToPayRequest] from [GooglePayConfig].
     */
    fun createIsReadyToPayRequest(config: GooglePayConfig): IsReadyToPayRequest

    /**
     * Constructs a Google Play Services [PaymentDataRequest] from [PaymentRequest].
     */
    fun createPaymentDataRequest(request: PaymentRequest): PaymentDataRequest

    /**
     * Constructs the raw Google Pay API v2 JSON string for the given [request].
     */
    fun createPaymentDataRequestJson(request: PaymentRequest): String

    /**
     * Loads payment data directly from Google Play Services.
     * Note: If user interaction is required, this may throw [com.google.android.gms.common.api.ResolvableApiException].
     */
    suspend fun loadPaymentData(request: PaymentRequest): PaymentData

    /**
     * Loads payment data using a pre-constructed [PaymentDataRequest].
     */
    suspend fun loadPaymentData(request: PaymentDataRequest): PaymentData

    /**
     * Parses a [PaymentData] object into a unified [PaymentResult].
     */
    fun parsePaymentResult(paymentData: PaymentData, transactionId: String = ""): PaymentResult

    /**
     * Parses a raw JSON response string into a unified [PaymentResult].
     */
    fun parsePaymentResult(paymentDataJson: String, transactionId: String = ""): PaymentResult

    /**
     * Parses the result received from an Activity/contract result callback.
     *
     * @param resultCode Activity result code (e.g. [Activity.RESULT_OK], [Activity.RESULT_CANCELED], or [AutoResolveHelper.RESULT_ERROR]).
     * @param data Intent data returned from Google Pay.
     * @param transactionId Transaction ID to associate with the result.
     */
    fun parsePaymentResultFromIntent(
        resultCode: Int,
        data: Intent?,
        transactionId: String = ""
    ): PaymentResult

    companion object {
        /**
         * Creates a default implementation of [GooglePayClient].
         *
         * @param context Application or Activity context.
         * @param environment Google Pay environment (TEST or PRODUCTION).
         */
        fun create(
            context: Context,
            environment: GooglePayEnvironment = GooglePayEnvironment.TEST
        ): GooglePayClient {
            val walletEnvironment = when (environment) {
                GooglePayEnvironment.PRODUCTION -> WalletConstants.ENVIRONMENT_PRODUCTION
                GooglePayEnvironment.TEST -> WalletConstants.ENVIRONMENT_TEST
            }
            val walletOptions = Wallet.WalletOptions.Builder()
                .setEnvironment(walletEnvironment)
                .build()
            val paymentsClient = Wallet.getPaymentsClient(context, walletOptions)
            return DefaultGooglePayClient(paymentsClient)
        }

        /**
         * Creates a [GooglePayClient] wrapping an existing [PaymentsClient].
         */
        fun from(paymentsClient: PaymentsClient): GooglePayClient {
            return DefaultGooglePayClient(paymentsClient)
        }
    }
}

/**
 * Default implementation of [GooglePayClient] wrapping Google Play Services [PaymentsClient].
 */
class DefaultGooglePayClient(
    override val paymentsClient: PaymentsClient
) : GooglePayClient {

    override fun createIsReadyToPayRequest(config: GooglePayConfig): IsReadyToPayRequest {
        val json = GooglePayJsonFactory.createIsReadyToPayRequest(config)
        return IsReadyToPayRequest.fromJson(json.toString())
    }

    override suspend fun isReadyToPay(config: GooglePayConfig): Boolean {
        return try {
            val request = createIsReadyToPayRequest(config)
            isReadyToPay(request)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            false
        }
    }

    override suspend fun isReadyToPay(request: IsReadyToPayRequest): Boolean {
        return try {
            paymentsClient.isReadyToPay(request).await()
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            false
        }
    }

    override fun createPaymentDataRequestJson(request: PaymentRequest): String {
        return GooglePayJsonFactory.createPaymentDataRequest(request).toString()
    }

    override fun createPaymentDataRequest(request: PaymentRequest): PaymentDataRequest {
        val json = createPaymentDataRequestJson(request)
        return PaymentDataRequest.fromJson(json)
    }

    override suspend fun loadPaymentData(request: PaymentRequest): PaymentData {
        val paymentDataRequest = createPaymentDataRequest(request)
        return loadPaymentData(paymentDataRequest)
    }

    override suspend fun loadPaymentData(request: PaymentDataRequest): PaymentData {
        return paymentsClient.loadPaymentData(request).await()
    }

    override fun parsePaymentResult(paymentData: PaymentData, transactionId: String): PaymentResult {
        val json = paymentData.toJson()
        return parsePaymentResult(json, transactionId)
    }

    override fun parsePaymentResult(paymentDataJson: String, transactionId: String): PaymentResult {
        return GooglePayJsonFactory.parsePaymentResult(paymentDataJson, transactionId)
    }

    override fun parsePaymentResultFromIntent(
        resultCode: Int,
        data: Intent?,
        transactionId: String
    ): PaymentResult {
        return when (resultCode) {
            Activity.RESULT_OK -> {
                if (data != null) {
                    val paymentData = PaymentData.getFromIntent(data)
                    if (paymentData != null) {
                        parsePaymentResult(paymentData, transactionId)
                    } else {
                        PaymentResult.Failure(
                            errorCode = PaymentErrorCode.UNKNOWN,
                            message = "Google Pay returned empty payment data"
                        )
                    }
                } else {
                    PaymentResult.Failure(
                        errorCode = PaymentErrorCode.UNKNOWN,
                        message = "Google Pay returned null intent data"
                    )
                }
            }
            Activity.RESULT_CANCELED -> {
                PaymentResult.Canceled
            }
            AutoResolveHelper.RESULT_ERROR -> {
                val status = AutoResolveHelper.getStatusFromIntent(data)
                val statusCode = status?.statusCode ?: CommonStatusCodes.ERROR
                val message = status?.statusMessage ?: "Google Pay returned status code: $statusCode"
                when (statusCode) {
                    CommonStatusCodes.CANCELED -> PaymentResult.Canceled
                    CommonStatusCodes.NETWORK_ERROR,
                    CommonStatusCodes.TIMEOUT -> PaymentResult.Failure(
                        errorCode = PaymentErrorCode.NETWORK_ERROR,
                        message = message
                    )
                    CommonStatusCodes.DEVELOPER_ERROR,
                    CommonStatusCodes.API_NOT_CONNECTED,
                    CommonStatusCodes.SERVICE_VERSION_UPDATE_REQUIRED,
                    CommonStatusCodes.SERVICE_DISABLED -> PaymentResult.Failure(
                        errorCode = PaymentErrorCode.CONFIGURATION_ERROR,
                        message = message
                    )
                    CommonStatusCodes.INTERRUPTED -> PaymentResult.Failure(
                        errorCode = PaymentErrorCode.UNKNOWN,
                        message = message
                    )
                    else -> PaymentResult.Failure(
                        errorCode = PaymentErrorCode.GATEWAY_ERROR,
                        message = message
                    )
                }
            }
            else -> {
                PaymentResult.Failure(
                    errorCode = PaymentErrorCode.UNKNOWN,
                    message = "Unknown Activity result code: $resultCode"
                )
            }
        }
    }
}
