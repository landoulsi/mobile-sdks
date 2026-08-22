package com.landoulsi.payment.shared.model

import kotlinx.serialization.Serializable

/**
 * Encapsulates the parameters required to execute a 3D Secure (3DS) authentication challenge.
 *
 * @property paymentIntentId The identifier of the PaymentIntent or transaction requiring authentication.
 * @property clientSecret The client secret used to confirm or authenticate the PaymentIntent.
 * @property redirectUrl The ACS / challenge URL to open in an in-app browser or WebView.
 * @property returnUrl The callback URL or custom scheme intercepted upon challenge completion.
 * @property acsUrl Optional Access Control Server (ACS) URL for native 3DS2 flow.
 * @property cReq Optional challenge request payload for 3DS2.
 * @property threeDSServerTransId Optional 3DS Server Transaction ID.
 */
@Serializable
data class ThreeDSChallenge(
    val paymentIntentId: String,
    val clientSecret: String,
    val redirectUrl: String,
    val returnUrl: String = DEFAULT_RETURN_URL,
    val acsUrl: String? = null,
    val cReq: String? = null,
    val threeDSServerTransId: String? = null
) {
    companion object {
        const val DEFAULT_RETURN_URL = "paymentsdk://3ds-complete"
    }

    /**
     * Redacted string representation that never exposes the [clientSecret] or [cReq] payload.
     */
    override fun toString(): String {
        return "ThreeDSChallenge(" +
            "paymentIntentId=$paymentIntentId, " +
            "clientSecret=[REDACTED], " +
            "redirectUrl=$redirectUrl, " +
            "returnUrl=$returnUrl, " +
            "acsUrl=$acsUrl, " +
            "cReq=[REDACTED], " +
            "threeDSServerTransId=$threeDSServerTransId)"
    }
}

/**
 * Sealed hierarchy representing the outcome of a 3D Secure challenge interaction.
 */
sealed interface ThreeDSResult {

    /**
     * Shopper completed the 3DS authentication challenge.
     *
     * @property returnPayload Optional query string or redirect payload captured upon return.
     */
    data class Completed(val returnPayload: String? = null) : ThreeDSResult

    /**
     * 3DS authentication failed with an error.
     *
     * @property errorCode Standardized error code.
     * @property message Human-readable error description.
     */
    data class Failed(val errorCode: PaymentErrorCode, val message: String) : ThreeDSResult

    /**
     * Shopper canceled or dismissed the 3DS challenge.
     */
    data object Canceled : ThreeDSResult
}

/**
 * Evaluates a navigation URL against the expected 3DS return URL and classifies
 * the authentication outcome according to 3DS2 / gateway redirect conventions.
 *
 * @param url The current or intercepted navigation URL.
 * @param expectedReturnUrl The expected callback URL / scheme configured for this challenge.
 * @return [ThreeDSResult] if [url] matches [expectedReturnUrl], or `null` if navigation should continue.
 */
fun parseThreeDSReturnUrl(url: String, expectedReturnUrl: String): ThreeDSResult? {
    if (url.isBlank() || expectedReturnUrl.isBlank()) return null

    // Reject mismatched schemes before comparing the rest of the URL.
    val expectedScheme = expectedReturnUrl.substringBefore("://", "").lowercase()
    val urlScheme = url.substringBefore("://", "").lowercase()
    if (expectedScheme.isBlank() || urlScheme != expectedScheme) {
        return null
    }

    val urlBase = url.substringBefore('?').substringBefore('#').trimEnd('/')
    val expectedBase = expectedReturnUrl.substringBefore('?').substringBefore('#').trimEnd('/')

    if (!urlBase.equals(expectedBase, ignoreCase = true)) {
        return null
    }

    val queryString = url.substringAfter('?', "")
    val params = if (queryString.isNotBlank()) {
        queryString.substringBefore('#').split('&').mapNotNull { param ->
            val parts = param.split('=', limit = 2)
            if (parts.isNotEmpty() && parts[0].isNotBlank()) {
                val key = decodeUrlComponent(parts[0]).lowercase()
                val value = if (parts.size > 1) decodeUrlComponent(parts[1]) else ""
                key to value
            } else null
        }.toMap()
    } else {
        emptyMap()
    }

    val status = params["status"]?.lowercase()
    val transStatus = params["transstatus"]?.uppercase()
    val isCanceled = status == "canceled" || status == "cancelled" || params["canceled"] == "true" || params["cancelled"] == "true"

    if (isCanceled) {
        return ThreeDSResult.Canceled
    }

    val isExplicitFailure = params.containsKey("error") ||
            params.containsKey("error_description") ||
            params.containsKey("error_code") ||
            status in listOf("failed", "declined", "error", "failure", "requires_payment_method") ||
            transStatus in listOf("N", "R", "U")

    if (isExplicitFailure) {
        return ThreeDSResult.Failed(
            errorCode = PaymentErrorCode.AUTHENTICATION_FAILED,
            message = "3D Secure authentication failed or declined by issuer"
        )
    }

    val isExplicitSuccess = transStatus in listOf("Y", "A") ||
            status in listOf("succeeded", "success")

    if (isExplicitSuccess) {
        return ThreeDSResult.Completed(returnPayload = url)
    }

    // Fail-closed: any non-explicit outcome (unknown params, transStatus=C/D/I, bare return URL without success indicator) is treated as failed
    return ThreeDSResult.Failed(
        errorCode = PaymentErrorCode.AUTHENTICATION_FAILED,
        message = "3D Secure authentication incomplete or returned indeterminate status"
    )
}

private fun decodeUrlComponent(encoded: String): String {
    val sb = StringBuilder()
    var i = 0
    while (i < encoded.length) {
        val c = encoded[i]
        when (c) {
            '+' -> {
                sb.append(' ')
                i++
            }
            '%' -> {
                if (i + 2 < encoded.length) {
                    val hex = encoded.substring(i + 1, i + 3)
                    val code = hex.toIntOrNull(16)
                    if (code != null) {
                        sb.append(code.toChar())
                        i += 3
                    } else {
                        sb.append('%')
                        i++
                    }
                } else {
                    sb.append('%')
                    i++
                }
            }
            else -> {
                sb.append(c)
                i++
            }
        }
    }
    return sb.toString()
}
