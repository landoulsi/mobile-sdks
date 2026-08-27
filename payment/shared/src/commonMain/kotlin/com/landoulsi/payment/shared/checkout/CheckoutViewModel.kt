package com.landoulsi.payment.shared.checkout

import com.landoulsi.payment.shared.model.PaymentErrorCode
import com.landoulsi.payment.shared.model.PaymentMethodType
import com.landoulsi.payment.shared.model.PaymentRequest
import com.landoulsi.payment.shared.model.PaymentResult
import com.landoulsi.payment.shared.model.ThreeDSChallenge
import com.landoulsi.payment.shared.model.ThreeDSResult
import com.landoulsi.payment.shared.network.GatewayClient
import com.landoulsi.payment.shared.network.GatewayException
import com.landoulsi.payment.shared.network.dto.PaymentIntentConfirmResponse
import com.landoulsi.payment.shared.provider.PaymentProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Common Multiplatform ViewModel / state orchestrator for checkout sessions.
 *
 * Coordinates payment provider readiness checks (such as Google Pay `isReadyToPay`),
 * manages explicit UI state transitions (Initial -> CheckingAvailability -> Ready -> Processing -> RequiresAuthentication -> Success/Failure/Canceled),
 * and handles both direct asynchronous provider execution, 3D Secure step-up challenges, and ActivityResult-based wallet contract outcomes.
 *
 * @property initialRequest The initial [PaymentRequest] for this checkout session.
 * @property providers Map of registered [PaymentProvider] instances keyed by [PaymentMethodType].
 * @property gatewayClient Optional [GatewayClient] used for card confirmation and 3DS round-trips.
 * @property coroutineScope The [CoroutineScope] in which background checks and payments run.
 */
class CheckoutViewModel(
    initialRequest: PaymentRequest,
    providers: List<PaymentProvider> = emptyList(),
    private val gatewayClient: GatewayClient? = null,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) {
    private val providers: Map<PaymentMethodType, PaymentProvider> = providers.associateBy { it.paymentMethodType }

    constructor(
        initialRequest: PaymentRequest,
        vararg providers: PaymentProvider
    ) : this(
        initialRequest = initialRequest,
        providers = providers.toList(),
        gatewayClient = null
    )

    constructor(
        initialRequest: PaymentRequest,
        gatewayClient: GatewayClient?,
        coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    ) : this(
        initialRequest = initialRequest,
        providers = emptyList(),
        gatewayClient = gatewayClient,
        coroutineScope = coroutineScope
    )

    private val _uiState = MutableStateFlow<CheckoutUiState>(CheckoutUiState.Initial(initialRequest))
    val uiState: StateFlow<CheckoutUiState> = _uiState.asStateFlow()

    private var currentRequest: PaymentRequest = initialRequest
    private val availabilityCache = mutableMapOf<PaymentMethodType, Boolean>()

    init {
        checkAvailability()
    }

    /**
     * Checks readiness for all configured providers and updates [uiState] to [CheckoutUiState.Ready].
     */
    fun checkAvailability() {
        val request = currentRequest
        _uiState.value = CheckoutUiState.CheckingAvailability(request)
        coroutineScope.launch {
            val availableMethods = mutableSetOf<PaymentMethodType>()
            var googlePayAvailable = false
            var applePayAvailable = false

            for (type in request.allowedPaymentMethods) {
                val provider = providers[type]
                val isReady = if (provider != null) {
                    try {
                        provider.isReadyToPay()
                    } catch (e: Throwable) {
                        if (e is CancellationException) throw e
                        false
                    }
                } else {
                    // Methods not requiring a native wallet provider (e.g. CARD) are treated as ready if allowed in the request
                    type == PaymentMethodType.CARD
                }

                availabilityCache[type] = isReady
                if (isReady) {
                    availableMethods.add(type)
                    if (type == PaymentMethodType.GOOGLE_PAY) googlePayAvailable = true
                    if (type == PaymentMethodType.APPLE_PAY) applePayAvailable = true
                }
            }

            _uiState.value = CheckoutUiState.Ready(
                request = request,
                isGooglePayAvailable = googlePayAvailable,
                isApplePayAvailable = applePayAvailable,
                availablePaymentMethods = availableMethods
            )
        }
    }

    /**
     * Signals that payment processing has started for a specific [paymentMethodType].
     */
    fun startProcessing(paymentMethodType: PaymentMethodType) {
        _uiState.value = CheckoutUiState.Processing(currentRequest, paymentMethodType)
    }

    /**
     * Executes the payment flow for the given [paymentMethodType] using its registered [PaymentProvider].
     */
    fun pay(paymentMethodType: PaymentMethodType) {
        val provider = providers[paymentMethodType]
        if (provider == null) {
            _uiState.value = CheckoutUiState.Failure(
                request = currentRequest,
                failure = PaymentResult.Failure(
                    errorCode = PaymentErrorCode.PAYMENT_METHOD_UNAVAILABLE,
                    message = "No provider registered for payment method: ${paymentMethodType.identifier}"
                )
            )
            return
        }

        startProcessing(paymentMethodType)
        coroutineScope.launch {
            try {
                val result = provider.pay(currentRequest)
                handlePaymentResult(result)
            } catch (e: Throwable) {
                if (e is CancellationException) throw e
                handlePaymentResult(
                    PaymentResult.Failure(
                        errorCode = PaymentErrorCode.UNKNOWN,
                        message = e.message ?: "Payment execution failed unexpectedly",
                        cause = e
                    )
                )
            }
        }
    }

    companion object {
        private const val MAX_3DS_AUTH_ATTEMPTS = 2
    }

    private var authAttemptCount: Int = 0

    /**
     * Confirms a PaymentIntent using the provided payment method ID and client secret,
     * transitioning into [CheckoutUiState.RequiresAuthentication] if 3DS verification is required.
     *
     * @param paymentIntentId The identifier of the PaymentIntent to confirm (e.g., `"pi_xxx"`).
     * @param paymentMethodId The payment method token or ID (e.g., `"pm_xxx"` or `"tok_xxx"`).
     * @param clientSecret The client secret for confirming the PaymentIntent.
     * @param paymentMethodType The payment method being used (defaults to [PaymentMethodType.CARD]).
     */
    fun confirmPayment(
        paymentIntentId: String,
        paymentMethodId: String,
        clientSecret: String,
        paymentMethodType: PaymentMethodType = PaymentMethodType.CARD
    ) {
        val gateway = gatewayClient
        if (gateway == null) {
            _uiState.value = CheckoutUiState.Failure(
                request = currentRequest,
                failure = PaymentResult.Failure(
                    errorCode = PaymentErrorCode.CONFIGURATION_ERROR,
                    message = "No GatewayClient configured for payment confirmation"
                )
            )
            return
        }

        authAttemptCount = 0
        startProcessing(paymentMethodType)
        val returnUrl = currentRequest.returnUrl?.takeIf { it.isNotBlank() } ?: ThreeDSChallenge.DEFAULT_RETURN_URL
        coroutineScope.launch {
            try {
                val response = gateway.confirmPayment(
                    paymentIntentId = paymentIntentId,
                    paymentMethodId = paymentMethodId,
                    clientSecret = clientSecret,
                    returnUrl = returnUrl
                )
                handlePaymentIntentResponse(response, paymentMethodType, clientSecret, returnUrl)
            } catch (e: Throwable) {
                if (e is CancellationException) throw e
                handlePaymentResult(
                    PaymentResult.Failure(
                        errorCode = if (e is GatewayException) PaymentErrorCode.GATEWAY_ERROR else PaymentErrorCode.UNKNOWN,
                        message = e.message ?: "Payment confirmation failed",
                        cause = e
                    )
                )
            }
        }
    }

    /**
     * Manually triggers or presents a 3DS challenge in the checkout state machine.
     *
     * @param challenge The [ThreeDSChallenge] to present to the user.
     * @param paymentMethodType The [PaymentMethodType] associated with the challenge.
     */
    fun requireAuthentication(
        challenge: ThreeDSChallenge,
        paymentMethodType: PaymentMethodType = PaymentMethodType.CARD
    ) {
        authAttemptCount = 0
        _uiState.value = CheckoutUiState.RequiresAuthentication(
            request = currentRequest,
            challenge = challenge,
            paymentMethodType = paymentMethodType
        )
    }

    /**
     * Handles the outcome of a 3D Secure authentication challenge.
     *
     * - [ThreeDSResult.Completed]: Resumes/finalizes the PaymentIntent with the gateway if available, or reports configuration error if gatewayClient is missing.
     * - [ThreeDSResult.Canceled]: Transitions the UI state to [CheckoutUiState.Canceled].
     * - [ThreeDSResult.Failed]: Transitions the UI state to [CheckoutUiState.Failure].
     */
    fun handle3DSResult(result: ThreeDSResult) {
        val currentState = _uiState.value
        if (currentState !is CheckoutUiState.RequiresAuthentication) {
            // Idempotent guard: if not in RequiresAuthentication, ignore duplicate / late delivery without destructively overwriting terminal state
            return
        }

        val challenge = currentState.challenge
        val methodType = currentState.paymentMethodType

        when (result) {
            is ThreeDSResult.Canceled -> {
                _uiState.value = CheckoutUiState.Canceled(currentRequest)
            }
            is ThreeDSResult.Failed -> {
                _uiState.value = CheckoutUiState.Failure(
                    request = currentRequest,
                    failure = PaymentResult.Failure(
                        errorCode = result.errorCode,
                        message = result.message
                    )
                )
            }
            is ThreeDSResult.Completed -> {
                val gateway = gatewayClient
                if (gateway == null) {
                    _uiState.value = CheckoutUiState.Failure(
                        request = currentRequest,
                        failure = PaymentResult.Failure(
                            errorCode = PaymentErrorCode.CONFIGURATION_ERROR,
                            message = "Cannot complete 3D Secure authentication: No GatewayClient configured"
                        )
                    )
                    return
                }

                startProcessing(methodType)
                coroutineScope.launch {
                    try {
                        val response = gateway.complete3DSAuthentication(
                            paymentIntentId = challenge.paymentIntentId,
                            clientSecret = challenge.clientSecret
                        )
                        handlePaymentIntentResponse(
                            response = response,
                            paymentMethodType = methodType,
                            clientSecret = challenge.clientSecret,
                            returnUrl = challenge.returnUrl
                        )
                    } catch (e: Throwable) {
                        if (e is CancellationException) throw e
                        handlePaymentResult(
                            PaymentResult.Failure(
                                errorCode = if (e is GatewayException) PaymentErrorCode.GATEWAY_ERROR else PaymentErrorCode.AUTHENTICATION_FAILED,
                                message = e.message ?: "3D Secure authentication confirmation failed",
                                cause = e
                            )
                        )
                    }
                }
            }
        }
    }

    private fun handlePaymentIntentResponse(
        response: PaymentIntentConfirmResponse,
        paymentMethodType: PaymentMethodType,
        clientSecret: String,
        returnUrl: String
    ) {
        when (response.status) {
            "succeeded" -> {
                handlePaymentResult(
                    PaymentResult.Success(
                        transactionId = response.id,
                        paymentMethodType = paymentMethodType,
                        token = response.paymentMethod
                    )
                )
            }
            "requires_action" -> {
                authAttemptCount++
                if (authAttemptCount > MAX_3DS_AUTH_ATTEMPTS) {
                    handlePaymentResult(
                        PaymentResult.Failure(
                            errorCode = PaymentErrorCode.AUTHENTICATION_FAILED,
                            message = "3D Secure authentication exceeded maximum allowed attempts"
                        )
                    )
                    return
                }

                val redirectUrl = response.nextAction?.redirectToUrl?.url
                    ?: response.nextAction?.useStripeSdk?.stripeJs

                if (redirectUrl.isNullOrBlank()) {
                    handlePaymentResult(
                        PaymentResult.Failure(
                            errorCode = PaymentErrorCode.GATEWAY_ERROR,
                            message = "Payment requires authentication but no 3DS redirect URL was provided by the gateway"
                        )
                    )
                    return
                }

                val effectiveReturnUrl = response.nextAction?.redirectToUrl?.returnUrl?.takeIf { it.isNotBlank() }
                    ?: returnUrl.takeIf { it.isNotBlank() }
                    ?: ThreeDSChallenge.DEFAULT_RETURN_URL

                val challenge = ThreeDSChallenge(
                    paymentIntentId = response.id,
                    clientSecret = response.clientSecret ?: clientSecret,
                    redirectUrl = redirectUrl,
                    returnUrl = effectiveReturnUrl,
                    acsUrl = response.nextAction?.useStripeSdk?.acsUrl,
                    cReq = response.nextAction?.useStripeSdk?.cReq,
                    threeDSServerTransId = response.nextAction?.useStripeSdk?.threeDSServerTransId
                )
                _uiState.value = CheckoutUiState.RequiresAuthentication(
                    request = currentRequest,
                    challenge = challenge,
                    paymentMethodType = paymentMethodType
                )
            }
            "requires_payment_method" -> {
                val errMessage = response.lastPaymentError?.message ?: "Payment method required or declined"
                val declineCode = response.lastPaymentError?.declineCode
                val errCode = when (declineCode) {
                    "insufficient_funds" -> PaymentErrorCode.INSUFFICIENT_FUNDS
                    "expired_card" -> PaymentErrorCode.EXPIRED_CARD
                    else -> PaymentErrorCode.CARD_DECLINED
                }
                handlePaymentResult(
                    PaymentResult.Failure(
                        errorCode = errCode,
                        message = errMessage
                    )
                )
            }
            "canceled" -> {
                handlePaymentResult(PaymentResult.Canceled)
            }
            else -> {
                handlePaymentResult(
                    PaymentResult.Failure(
                        errorCode = PaymentErrorCode.GATEWAY_ERROR,
                        message = "Unhandled payment intent status: ${response.status}"
                    )
                )
            }
        }
    }

    /**
     * Handles a [PaymentResult] received from either a provider or an Activity result contract callback.
     */
    fun handlePaymentResult(result: PaymentResult) {
        val request = currentRequest
        _uiState.value = when (result) {
            is PaymentResult.Success -> CheckoutUiState.Success(request, result)
            is PaymentResult.Failure -> CheckoutUiState.Failure(request, result)
            is PaymentResult.Canceled -> CheckoutUiState.Canceled(request)
        }
    }

    /**
     * Resets checkout state back to [CheckoutUiState.Ready] for another payment attempt.
     */
    fun reset() {
        val request = currentRequest
        val googlePayAvailable = availabilityCache[PaymentMethodType.GOOGLE_PAY] ?: false
        val applePayAvailable = availabilityCache[PaymentMethodType.APPLE_PAY] ?: false
        val availableMethods = availabilityCache.filterValues { it }.keys.toSet()

        _uiState.value = CheckoutUiState.Ready(
            request = request,
            isGooglePayAvailable = googlePayAvailable,
            isApplePayAvailable = applePayAvailable,
            availablePaymentMethods = availableMethods
        )
    }

    /**
     * Updates the active [PaymentRequest] and refreshes availability.
     */
    fun updatePaymentRequest(newRequest: PaymentRequest, refreshAvailability: Boolean = true) {
        currentRequest = newRequest
        if (refreshAvailability) {
            checkAvailability()
        } else {
            reset()
        }
    }

    /**
     * Retrieves the provider registered for [paymentMethodType], if any.
     */
    fun getProvider(paymentMethodType: PaymentMethodType): PaymentProvider? {
        return providers[paymentMethodType]
    }

    /**
     * Checks whether [paymentMethodType] is currently recorded as ready and available.
     */
    fun isPaymentMethodAvailable(paymentMethodType: PaymentMethodType): Boolean {
        return availabilityCache[paymentMethodType] ?: false
    }
}
