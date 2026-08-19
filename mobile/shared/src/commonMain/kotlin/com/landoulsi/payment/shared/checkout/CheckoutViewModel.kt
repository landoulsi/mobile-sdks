package com.landoulsi.payment.shared.checkout

import com.landoulsi.payment.shared.model.PaymentErrorCode
import com.landoulsi.payment.shared.model.PaymentMethodType
import com.landoulsi.payment.shared.model.PaymentRequest
import com.landoulsi.payment.shared.model.PaymentResult
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
 * manages explicit UI state transitions (Initial -> CheckingAvailability -> Ready -> Processing -> Success/Failure/Canceled),
 * and handles both direct asynchronous provider execution and ActivityResult-based wallet contract outcomes.
 *
 * @property initialRequest The initial [PaymentRequest] for this checkout session.
 * @property providers Map of registered [PaymentProvider] instances keyed by [PaymentMethodType].
 * @property coroutineScope The [CoroutineScope] in which background checks and payments run.
 */
class CheckoutViewModel(
    initialRequest: PaymentRequest,
    private val providers: Map<PaymentMethodType, PaymentProvider> = emptyMap(),
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) {

    constructor(
        initialRequest: PaymentRequest,
        providers: List<PaymentProvider>,
        coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    ) : this(
        initialRequest = initialRequest,
        providers = providers.associateBy { it.paymentMethodType },
        coroutineScope = coroutineScope
    )

    constructor(
        initialRequest: PaymentRequest,
        vararg providers: PaymentProvider,
        coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    ) : this(
        initialRequest = initialRequest,
        providers = providers.associateBy { it.paymentMethodType },
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
