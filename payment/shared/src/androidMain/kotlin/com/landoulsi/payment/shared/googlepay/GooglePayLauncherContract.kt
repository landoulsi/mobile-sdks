package com.landoulsi.payment.shared.googlepay

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.tasks.Task
import com.google.android.gms.wallet.AutoResolveHelper
import com.google.android.gms.wallet.PaymentData
import com.google.android.gms.wallet.contract.TaskResultContracts
import com.landoulsi.payment.shared.model.PaymentErrorCode
import com.landoulsi.payment.shared.model.PaymentResult

/**
 * Input data class for [GooglePayPaymentTaskContract].
 *
 * @property task The [Task] returning [PaymentData] produced by [com.google.android.gms.wallet.PaymentsClient.loadPaymentData].
 * @property transactionId Optional transaction or payment intent ID to associate with the resulting [PaymentResult].
 */
data class GooglePayPaymentTaskInput(
    val task: Task<PaymentData>,
    val transactionId: String = ""
)

/**
 * Input data class for [GooglePayIntentResultContract].
 *
 * @property intent The resolution [Intent] received from Google Pay or [AutoResolveHelper].
 * @property transactionId Optional transaction or payment intent ID to associate with the resulting [PaymentResult].
 */
data class GooglePayIntentResultInput(
    val intent: Intent,
    val transactionId: String = ""
)

/**
 * An [ActivityResultContract] that takes a [GooglePayPaymentTaskInput] containing a [Task]<[PaymentData]> produced by
 * [com.google.android.gms.wallet.PaymentsClient.loadPaymentData] and returns a parsed [PaymentResult].
 *
 * This contract delegates directly to Google Play Services' official
 * [TaskResultContracts.GetPaymentDataResult] and translates the result status into a typed [PaymentResult].
 *
 * Example usage with Jetpack Compose:
 * ```kotlin
 * val launcher = rememberLauncherForActivityResult(GooglePayPaymentTaskContract()) { result ->
 *     when (result) {
 *         is PaymentResult.Success -> handleSuccess(result)
 *         is PaymentResult.Failure -> handleError(result)
 *         is PaymentResult.Canceled -> handleCancellation()
 *     }
 * }
 *
 * val task = client.paymentsClient.loadPaymentData(client.createPaymentDataRequest(paymentRequest))
 * launcher.launch(GooglePayPaymentTaskInput(task = task, transactionId = paymentRequest.id))
 * ```
 */
class GooglePayPaymentTaskContract(
    private val defaultTransactionId: String = ""
) : ActivityResultContract<GooglePayPaymentTaskInput, PaymentResult>() {

    private val delegate = TaskResultContracts.GetPaymentDataResult()
    private var pendingTransactionId: String = defaultTransactionId

    override fun createIntent(context: Context, input: GooglePayPaymentTaskInput): Intent {
        pendingTransactionId = input.transactionId.ifEmpty { defaultTransactionId }
        return delegate.createIntent(context, input.task)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): PaymentResult {
        val transactionId = pendingTransactionId
        val apiTaskResult = delegate.parseResult(resultCode, intent)
        val status = apiTaskResult.status
        val paymentData = apiTaskResult.result

        if (status.isSuccess && paymentData != null) {
            return GooglePayJsonFactory.parsePaymentResult(paymentData.toJson(), transactionId)
        }

        if (resultCode == Activity.RESULT_CANCELED || status.statusCode == CommonStatusCodes.CANCELED) {
            return PaymentResult.Canceled
        }

        val statusCode = status.statusCode
        val message = status.statusMessage ?: "Google Pay returned status code: $statusCode"
        return when (statusCode) {
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
}

/**
 * An [ActivityResultContract] that receives a [GooglePayIntentResultInput] containing an Activity resolution Intent
 * (from [AutoResolveHelper]) and parses the intent result into a typed [PaymentResult].
 */
class GooglePayIntentResultContract(
    private val client: GooglePayClient? = null,
    private val defaultTransactionId: String = ""
) : ActivityResultContract<GooglePayIntentResultInput, PaymentResult>() {

    private var pendingTransactionId: String = defaultTransactionId

    override fun createIntent(context: Context, input: GooglePayIntentResultInput): Intent {
        pendingTransactionId = input.transactionId.ifEmpty { defaultTransactionId }
        return input.intent
    }

    override fun parseResult(resultCode: Int, intent: Intent?): PaymentResult {
        val transactionId = pendingTransactionId
        client?.let {
            return it.parsePaymentResultFromIntent(resultCode, intent, transactionId)
        }

        return when (resultCode) {
            Activity.RESULT_OK -> {
                if (intent != null) {
                    val paymentData = PaymentData.getFromIntent(intent)
                    if (paymentData != null) {
                        GooglePayJsonFactory.parsePaymentResult(paymentData.toJson(), transactionId)
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
                val status = AutoResolveHelper.getStatusFromIntent(intent)
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
