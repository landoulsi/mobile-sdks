package com.landoulsi.payment.shared.network

import com.landoulsi.payment.shared.model.PaymentErrorCode
import com.landoulsi.payment.shared.network.dto.GatewayErrorResponse
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess

/**
 * Exception thrown when the payment gateway returns a non-success HTTP status.
 *
 * @property statusCode The HTTP status code from the gateway response.
 * @property errorCode SDK-level [PaymentErrorCode] mapped from the HTTP status.
 * @property gatewayMessage Human-readable message extracted from the gateway error body.
 * @property gatewayCode Machine-readable gateway error code (e.g., `"card_declined"`).
 * @property declineCode Optional decline code for card errors.
 */
class GatewayException(
    val statusCode: Int,
    override val message: String,
    val errorCode: PaymentErrorCode,
    val gatewayCode: String? = null,
    val declineCode: String? = null,
    cause: Throwable? = null
) : Exception(message, cause)

/**
 * Maps an HTTP [statusCode] to a [PaymentErrorCode].
 *
 * @param gatewayCode Optional machine-readable code from the gateway error body.
 */
internal fun mapStatusToErrorCode(statusCode: Int, gatewayCode: String? = null): PaymentErrorCode {
    // Map specific gateway decline codes first
    return when (gatewayCode) {
        "card_declined" -> PaymentErrorCode.CARD_DECLINED
        "expired_card" -> PaymentErrorCode.EXPIRED_CARD
        "insufficient_funds" -> PaymentErrorCode.INSUFFICIENT_FUNDS
        "authentication_required" -> PaymentErrorCode.AUTHENTICATION_FAILED
        else -> when (statusCode) {
            HttpStatusCode.Unauthorized.value,
            HttpStatusCode.Forbidden.value -> PaymentErrorCode.CONFIGURATION_ERROR
            HttpStatusCode.UnprocessableEntity.value,
            HttpStatusCode.PaymentRequired.value -> PaymentErrorCode.GATEWAY_ERROR
            in 500..599 -> PaymentErrorCode.GATEWAY_ERROR
            else -> PaymentErrorCode.UNKNOWN
        }
    }
}

/**
 * Throws a [GatewayException] if the HTTP response is not successful (2xx).
 * Attempts to decode the response body as [GatewayErrorResponse] to extract a structured error.
 *
 * @throws GatewayException when [HttpResponse.status] is not in the 2xx range.
 */
internal suspend fun HttpResponse.throwIfError() {
    if (status.isSuccess()) return

    val statusCode = status.value
    val rawBody = runCatching { bodyAsText() }.getOrNull().orEmpty()

    val errorResponse = runCatching {
        PaymentJson.decodeFromString(GatewayErrorResponse.serializer(), rawBody)
    }.getOrNull()

    val gatewayError = errorResponse?.error
    val gatewayCode = gatewayError?.code
    val declineCode = gatewayError?.declineCode
    val gatewayMessage = gatewayError?.message
        ?: "Gateway error (HTTP $statusCode): $rawBody"

    throw GatewayException(
        statusCode = statusCode,
        message = gatewayMessage,
        errorCode = mapStatusToErrorCode(statusCode, gatewayCode),
        gatewayCode = gatewayCode,
        declineCode = declineCode
    )
}
