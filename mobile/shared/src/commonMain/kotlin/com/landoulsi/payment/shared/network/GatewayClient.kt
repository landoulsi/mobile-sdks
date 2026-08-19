package com.landoulsi.payment.shared.network

import com.landoulsi.payment.shared.network.dto.CardTokenRequest
import com.landoulsi.payment.shared.network.dto.CardTokenResponse
import com.landoulsi.payment.shared.network.dto.GooglePayGatewayToken
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders

/**
 * Abstraction over the HTTP payment gateway for tokenization requests.
 *
 * This interface decouples the checkout domain from the concrete Ktor implementation,
 * enabling straightforward substitution with a mock in unit tests.
 */
interface GatewayClient {

    /**
     * Tokenizes a raw card payload with the payment gateway and returns a [CardTokenResponse]
     * containing a short-lived payment method token.
     *
     * @param request Card details to tokenize.
     * @return [CardTokenResponse] with the gateway token and card info.
     * @throws GatewayException if the gateway returns a non-success HTTP response.
     * @throws kotlinx.coroutines.CancellationException if the coroutine is cancelled.
     */
    suspend fun tokenizeCard(request: CardTokenRequest): CardTokenResponse

    /**
     * Tokenizes a Google Pay gateway token with the payment gateway.
     *
     * Some gateways (e.g., Stripe) receive the raw Google Pay token directly via their API
     * and return a server-side payment method in exchange. This method sends the Google Pay
     * token string to the configured endpoint and returns the decoded [GooglePayGatewayToken].
     *
     * @param googlePayToken The raw Google Pay token string from `paymentData.paymentMethodData.tokenizationData.token`.
     * @return [GooglePayGatewayToken] returned by the gateway.
     * @throws GatewayException if the gateway returns a non-success HTTP response.
     */
    suspend fun tokenizeGooglePay(googlePayToken: String): GooglePayGatewayToken
}

/**
 * Ktor-backed implementation of [GatewayClient] that communicates with a Stripe-compatible API.
 *
 * The client:
 * - Sends `Authorization: Bearer <publishableKey>` on every request.
 * - Handles non-2xx responses by throwing [GatewayException] with structured error information.
 * - Decodes all responses using [PaymentJson] (lenient, ignores unknown keys).
 *
 * @property httpClient The configured Ktor [HttpClient] instance (use [createPaymentHttpClient]).
 * @property baseUrl Base URL of the payment gateway API (e.g., `"https://api.stripe.com/v1"`).
 * @property publishableKey API key sent as Bearer token for authentication.
 */
class KtorGatewayClient(
    private val httpClient: HttpClient,
    private val baseUrl: String,
    private val publishableKey: String
) : GatewayClient {

    /**
     * Tokenizes a card using the gateway's card token creation endpoint (`POST /tokens`).
     *
     * Request body is form-encoded as per Stripe API convention but sent as JSON here
     * for consistency with the content-negotiation plugin.
     */
    override suspend fun tokenizeCard(request: CardTokenRequest): CardTokenResponse {
        val response: HttpResponse = httpClient.post("$baseUrl/tokens") {
            header(HttpHeaders.Authorization, "Bearer $publishableKey")
            setBody(request)
        }
        response.throwIfError()
        return response.body()
    }

    /**
     * Tokenizes a Google Pay token using the gateway's payment method creation endpoint (`POST /payment_methods`).
     *
     * Sends the serialized Google Pay token for server-side processing.
     */
    override suspend fun tokenizeGooglePay(googlePayToken: String): GooglePayGatewayToken {
        val response: HttpResponse = httpClient.post("$baseUrl/payment_methods") {
            header(HttpHeaders.Authorization, "Bearer $publishableKey")
            setBody(GooglePayTokenizeRequest(googlePayToken = googlePayToken))
        }
        response.throwIfError()
        return response.body()
    }
}

/**
 * Internal request body for Google Pay tokenization.
 *
 * @property googlePayToken The raw Google Pay token string.
 */
@kotlinx.serialization.Serializable
internal data class GooglePayTokenizeRequest(
    @kotlinx.serialization.SerialName("google_pay_token") val googlePayToken: String
)
