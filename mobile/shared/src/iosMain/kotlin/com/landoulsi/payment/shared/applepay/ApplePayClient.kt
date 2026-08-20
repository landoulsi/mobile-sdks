package com.landoulsi.payment.shared.applepay

import com.landoulsi.payment.shared.model.Address
import com.landoulsi.payment.shared.model.ApplePayConfig
import com.landoulsi.payment.shared.model.ApplePayContactField
import com.landoulsi.payment.shared.model.ApplePayMerchantCapability
import com.landoulsi.payment.shared.model.ApplePayShippingType
import com.landoulsi.payment.shared.model.ApplePaySummaryItem
import com.landoulsi.payment.shared.model.ApplePaySummaryItemType
import com.landoulsi.payment.shared.model.CardNetwork
import com.landoulsi.payment.shared.model.PaymentErrorCode
import com.landoulsi.payment.shared.model.PaymentMethodType
import com.landoulsi.payment.shared.model.PaymentRequest
import com.landoulsi.payment.shared.model.PaymentResult
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import platform.Foundation.NSData
import platform.Foundation.NSDecimalNumber
import platform.Foundation.NSJSONSerialization
import platform.Foundation.NSPersonNameComponents
import platform.Foundation.NSPersonNameComponentsFormatter
import platform.Foundation.NSSet
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataWithBytes
import platform.Foundation.setWithArray
import platform.PassKit.PKContact
import platform.PassKit.PKContactField
import platform.PassKit.PKContactFieldEmailAddress
import platform.PassKit.PKContactFieldName
import platform.PassKit.PKContactFieldPhoneNumber
import platform.PassKit.PKContactFieldPhoneticName
import platform.PassKit.PKContactFieldPostalAddress
import platform.PassKit.PKMerchantCapability
import platform.PassKit.PKMerchantCapability3DS
import platform.PassKit.PKMerchantCapabilityCredit
import platform.PassKit.PKMerchantCapabilityDebit
import platform.PassKit.PKMerchantCapabilityEMV
import platform.PassKit.PKPayment
import platform.PassKit.PKPaymentAuthorizationController
import platform.PassKit.PKPaymentAuthorizationControllerDelegateProtocol
import platform.PassKit.PKPaymentAuthorizationResult
import platform.PassKit.PKPaymentAuthorizationStatus
import platform.PassKit.PKPaymentMerchantSession
import platform.PassKit.PKPaymentNetwork
import platform.PassKit.PKPaymentNetworkAmex
import platform.PassKit.PKPaymentNetworkChinaUnionPay
import platform.PassKit.PKPaymentNetworkDiscover
import platform.PassKit.PKPaymentNetworkInterac
import platform.PassKit.PKPaymentNetworkJCB
import platform.PassKit.PKPaymentNetworkMasterCard
import platform.PassKit.PKPaymentNetworkVisa
import platform.PassKit.PKPaymentRequest
import platform.PassKit.PKPaymentRequestMerchantSessionUpdate
import platform.PassKit.PKPaymentSummaryItem
import platform.PassKit.PKPaymentSummaryItemType
import platform.PassKit.PKShippingType
import platform.UIKit.UIApplication
import platform.UIKit.UISceneActivationStateForegroundActive
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene
import platform.darwin.NSObject

/**
 * Client for interacting with Apple Pay via PassKit's [PKPaymentAuthorizationController].
 */
interface ApplePayClient {

    /**
     * Checks if the device hardware and configuration supports Apple Pay in general.
     */
    fun canMakePayments(): Boolean

    /**
     * Checks if the user can make payments with active cards matching the [config].
     */
    fun canMakePaymentsWithActiveCard(config: ApplePayConfig): Boolean

    /**
     * Suspendable check whether Apple Pay is ready to process payments for the given [config].
     */
    suspend fun isReadyToPay(config: ApplePayConfig): Boolean

    /**
     * Presents the Apple Pay payment sheet for the given [request] and awaits the payment result.
     */
    suspend fun presentPaymentSheet(request: PaymentRequest): PaymentResult

    /**
     * Constructs a [PKPaymentRequest] from the unified [PaymentRequest].
     */
    fun createPKPaymentRequest(request: PaymentRequest): PKPaymentRequest

    /**
     * Parses a completed [PKPayment] into a unified [PaymentResult].
     */
    fun parsePaymentResult(payment: PKPayment, transactionId: String): PaymentResult

    companion object {
        /**
         * Creates a default implementation of [ApplePayClient].
         */
        fun create(
            customWindowProvider: (() -> UIWindow?)? = null,
            coroutineScope: CoroutineScope = MainScope(),
            mainDispatcher: CoroutineDispatcher = Dispatchers.Main
        ): ApplePayClient {
            return DefaultApplePayClient(
                customWindowProvider = customWindowProvider,
                coroutineScope = coroutineScope,
                mainDispatcher = mainDispatcher
            )
        }

        /**
         * Standalone pure parser for converting a [PKPayment] into a [PaymentResult].
         */
        @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
        fun parsePayment(payment: PKPayment, transactionId: String): PaymentResult {
            val tokenData = payment.token.paymentData
            val tokenString = if (tokenData.length > 0uL) {
                NSString.create(tokenData, NSUTF8StringEncoding)?.toString()
            } else null

            if (tokenString.isNullOrBlank()) {
                return PaymentResult.Failure(
                    errorCode = PaymentErrorCode.PAYMENT_METHOD_UNAVAILABLE,
                    message = "Apple Pay payment token data is missing or empty"
                )
            }

            val networkName = payment.token.paymentMethod.network?.let { CardNetwork.fromName(it) }
            val displayName = payment.token.paymentMethod.displayName
            val last4 = extractLast4Digits(displayName)

            val billingAddress = payment.billingContact?.let { extractAddress(it) }
            val shippingAddress = payment.shippingContact?.let { extractAddress(it) }
            val email = payment.shippingContact?.emailAddress ?: payment.billingContact?.emailAddress

            val txId = transactionId.ifEmpty { payment.token.transactionIdentifier }

            return PaymentResult.Success(
                transactionId = txId,
                paymentMethodType = PaymentMethodType.APPLE_PAY,
                token = tokenString,
                rawPaymentData = tokenString,
                last4 = last4,
                cardNetwork = networkName,
                billingAddress = billingAddress,
                shippingAddress = shippingAddress,
                email = email
            )
        }

        private fun extractLast4Digits(displayName: String?): String? {
            if (displayName.isNullOrBlank()) return null
            val digits = displayName.filter { it.isDigit() }
            return if (digits.length >= 4) digits.takeLast(4) else null
        }

        private fun extractAddress(contact: PKContact): Address {
            val name = contact.name?.let { formatPersonName(it) }
            val postal = contact.postalAddress
            return Address(
                name = name,
                address1 = postal?.street,
                address2 = postal?.subLocality,
                city = postal?.city,
                state = postal?.state,
                postalCode = postal?.postalCode,
                countryCode = postal?.ISOCountryCode,
                phoneNumber = contact.phoneNumber?.stringValue,
                email = contact.emailAddress
            )
        }

        private fun formatPersonName(nameComponents: NSPersonNameComponents): String {
            val formatter = NSPersonNameComponentsFormatter()
            return formatter.stringFromPersonNameComponents(nameComponents)
        }
    }
}

/**
 * Default implementation of [ApplePayClient] wrapping [PKPaymentAuthorizationController].
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class DefaultApplePayClient(
    private val customWindowProvider: (() -> UIWindow?)? = null,
    private val coroutineScope: CoroutineScope = MainScope(),
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main
) : ApplePayClient {

    private val activeControllers = mutableSetOf<Pair<PKPaymentAuthorizationController, PKPaymentAuthorizationControllerDelegateProtocol>>()

    override fun canMakePayments(): Boolean {
        return PKPaymentAuthorizationController.canMakePayments()
    }

    override fun canMakePaymentsWithActiveCard(config: ApplePayConfig): Boolean {
        val networks = mapCardNetworks(config.allowedCardNetworks)
        val capabilities = mapMerchantCapabilities(config.merchantCapabilities)
        return PKPaymentAuthorizationController.canMakePaymentsUsingNetworks(networks, capabilities)
    }

    override suspend fun isReadyToPay(config: ApplePayConfig): Boolean {
        return try {
            canMakePaymentsWithActiveCard(config)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            false
        }
    }

    override fun createPKPaymentRequest(request: PaymentRequest): PKPaymentRequest {
        val config = requireNotNull(request.applePayConfig) {
            "PaymentRequest must have applePayConfig set"
        }
        require(config.merchantIdentifier.isNotBlank()) {
            "Apple Pay merchantIdentifier cannot be empty"
        }
        require(request.amount.amountMinorUnits > 0L) {
            "Payment amount must be greater than 0"
        }

        val pkRequest = PKPaymentRequest()
        pkRequest.merchantIdentifier = config.merchantIdentifier
        pkRequest.countryCode = config.countryCode
        pkRequest.currencyCode = request.amount.currency.code
        pkRequest.merchantCapabilities = mapMerchantCapabilities(config.merchantCapabilities)
        pkRequest.supportedNetworks = mapCardNetworks(config.allowedCardNetworks)

        if (config.supportedCountries.isNotEmpty()) {
            pkRequest.supportedCountries = NSSet.setWithArray(config.supportedCountries.toList())
        }

        pkRequest.shippingType = mapShippingType(config.shippingType)

        val billingFields = buildSet {
            if (request.requireBillingAddress) {
                add(ApplePayContactField.POSTAL_ADDRESS)
            }
            addAll(config.requiredBillingContactFields)
        }
        if (billingFields.isNotEmpty()) {
            pkRequest.requiredBillingContactFields = NSSet.setWithArray(
                billingFields.mapNotNull { mapContactField(it) }
            )
        }

        val shippingFields = buildSet {
            if (request.requireShipping) {
                add(ApplePayContactField.POSTAL_ADDRESS)
            }
            addAll(config.requiredShippingContactFields)
        }
        if (shippingFields.isNotEmpty()) {
            pkRequest.requiredShippingContactFields = NSSet.setWithArray(
                shippingFields.mapNotNull { mapContactField(it) }
            )
        }

        if (config.summaryItems.isNotEmpty()) {
            require(config.summaryItems.all { it.amount.currency == request.amount.currency }) {
                "All Apple Pay summary item currencies must match the payment request currency (${request.amount.currency.code})"
            }
            require(config.summaryItems.last().amount == request.amount) {
                "The last Apple Pay summary item must match the total request amount (${request.amount.formattedAmount()} ${request.amount.currency.code})"
            }

            pkRequest.paymentSummaryItems = config.summaryItems.map { item ->
                val amount = NSDecimalNumber.decimalNumberWithString(item.amount.formattedAmount())
                val itemType = when (item.type) {
                    ApplePaySummaryItemType.FINAL -> PKPaymentSummaryItemType.PKPaymentSummaryItemTypeFinal
                    ApplePaySummaryItemType.PENDING -> PKPaymentSummaryItemType.PKPaymentSummaryItemTypePending
                }
                PKPaymentSummaryItem.summaryItemWithLabel(item.label, amount = amount, type = itemType)
            }
        } else {
            val totalLabel = request.merchantName ?: "Total"
            val totalAmount = NSDecimalNumber.decimalNumberWithString(request.amount.formattedAmount())
            pkRequest.paymentSummaryItems = listOf(
                PKPaymentSummaryItem.summaryItemWithLabel(
                    label = totalLabel,
                    amount = totalAmount,
                    type = PKPaymentSummaryItemType.PKPaymentSummaryItemTypeFinal
                )
            )
        }

        return pkRequest
    }

    override suspend fun presentPaymentSheet(request: PaymentRequest): PaymentResult {
        // Validate request parameters before dispatching to the main thread
        val config = request.applePayConfig
        if (config == null || config.merchantIdentifier.isBlank()) {
            return PaymentResult.Failure(
                errorCode = PaymentErrorCode.CONFIGURATION_ERROR,
                message = "Apple Pay merchantIdentifier cannot be empty"
            )
        }
        if (!config.merchantIdentifier.startsWith("merchant.")) {
            return PaymentResult.Failure(
                errorCode = PaymentErrorCode.CONFIGURATION_ERROR,
                message = "Apple Pay merchantIdentifier must start with 'merchant.'"
            )
        }
        if (config.countryCode.length != 2) {
            return PaymentResult.Failure(
                errorCode = PaymentErrorCode.CONFIGURATION_ERROR,
                message = "Apple Pay countryCode must be a 2-letter ISO 3166-1 alpha-2 country code"
            )
        }
        if (config.supportedCountries.any { it.length != 2 }) {
            return PaymentResult.Failure(
                errorCode = PaymentErrorCode.CONFIGURATION_ERROR,
                message = "Apple Pay supportedCountries must contain 2-letter ISO country codes"
            )
        }
        if (request.amount.amountMinorUnits <= 0L) {
            return PaymentResult.Failure(
                errorCode = PaymentErrorCode.CONFIGURATION_ERROR,
                message = "Payment amount must be greater than 0"
            )
        }
        if (config.summaryItems.isNotEmpty()) {
            if (config.summaryItems.any { it.amount.currency != request.amount.currency }) {
                return PaymentResult.Failure(
                    errorCode = PaymentErrorCode.CONFIGURATION_ERROR,
                    message = "All Apple Pay summary item currencies must match the payment request currency (${request.amount.currency.code})"
                )
            }
            if (config.summaryItems.last().amount != request.amount) {
                return PaymentResult.Failure(
                    errorCode = PaymentErrorCode.CONFIGURATION_ERROR,
                    message = "The last Apple Pay summary item must match the total request amount (${request.amount.formattedAmount()} ${request.amount.currency.code})"
                )
            }
        }

        return withContext(mainDispatcher) {
            val pkRequest = createPKPaymentRequest(request)
            val controller = PKPaymentAuthorizationController(paymentRequest = pkRequest)
            val deferredResult = CompletableDeferred<PaymentResult>()

            val delegate = ApplePayAuthorizationDelegate(
                request = request,
                config = config,
                coroutineScope = coroutineScope,
                deferredResult = deferredResult,
                customWindowProvider = customWindowProvider,
                paymentParser = ::parsePaymentResult
            )
            controller.delegate = delegate

            // Explicitly retain controller and delegate pairs to prevent premature collection
            val session = controller to delegate
            activeControllers.add(session)

            var presentedSuccessfully = false
            try {
                val presented = CompletableDeferred<Boolean>()
                controller.presentWithCompletion { success ->
                    presented.complete(success)
                }

                presentedSuccessfully = presented.await()
                if (!presentedSuccessfully) {
                    return@withContext PaymentResult.Failure(
                        errorCode = PaymentErrorCode.PAYMENT_METHOD_UNAVAILABLE,
                        message = "Failed to present Apple Pay authorization controller"
                    )
                }

                deferredResult.await()
            } finally {
                if (presentedSuccessfully && !deferredResult.isCompleted) {
                    // Sheet was presented but coroutine was cancelled; dismiss controller to avoid stuck UI
                    controller.dismissWithCompletion(null)
                }
                controller.delegate = null
                activeControllers.remove(session)
            }
        }
    }

    override fun parsePaymentResult(payment: PKPayment, transactionId: String): PaymentResult {
        return ApplePayClient.parsePayment(payment, transactionId)
    }

    private fun mapCardNetworks(networks: List<CardNetwork>): List<PKPaymentNetwork> {
        return networks.mapNotNull { network ->
            when (network) {
                CardNetwork.VISA -> PKPaymentNetworkVisa
                CardNetwork.MASTERCARD -> PKPaymentNetworkMasterCard
                CardNetwork.AMEX -> PKPaymentNetworkAmex
                CardNetwork.DISCOVER -> PKPaymentNetworkDiscover
                CardNetwork.JCB -> PKPaymentNetworkJCB
                CardNetwork.INTERAC -> PKPaymentNetworkInterac
                CardNetwork.UNION_PAY -> PKPaymentNetworkChinaUnionPay
                CardNetwork.DINERS_CLUB -> PKPaymentNetworkDiscover
            }
        }
    }

    private fun mapMerchantCapabilities(capabilities: List<ApplePayMerchantCapability>): PKMerchantCapability {
        var result: PKMerchantCapability = 0uL
        capabilities.forEach { capability ->
            when (capability) {
                ApplePayMerchantCapability.THREE_D_SECURE -> result = result or PKMerchantCapability3DS
                ApplePayMerchantCapability.EMV -> result = result or PKMerchantCapabilityEMV
                ApplePayMerchantCapability.CREDIT -> result = result or PKMerchantCapabilityCredit
                ApplePayMerchantCapability.DEBIT -> result = result or PKMerchantCapabilityDebit
            }
        }
        return if (result == 0uL) PKMerchantCapability3DS else result
    }

    private fun mapShippingType(type: ApplePayShippingType): PKShippingType {
        return when (type) {
            ApplePayShippingType.SHIPPING -> PKShippingType.PKShippingTypeShipping
            ApplePayShippingType.DELIVERY -> PKShippingType.PKShippingTypeDelivery
            ApplePayShippingType.STORE_PICKUP -> PKShippingType.PKShippingTypeStorePickup
            ApplePayShippingType.SERVICE_PICKUP -> PKShippingType.PKShippingTypeServicePickup
        }
    }

    private fun mapContactField(field: ApplePayContactField): PKContactField? {
        return when (field) {
            ApplePayContactField.POSTAL_ADDRESS -> PKContactFieldPostalAddress
            ApplePayContactField.EMAIL -> PKContactFieldEmailAddress
            ApplePayContactField.PHONE_NUMBER -> PKContactFieldPhoneNumber
            ApplePayContactField.NAME -> PKContactFieldName
            ApplePayContactField.PHONETIC_NAME -> PKContactFieldPhoneticName
        }
    }
}

/**
 * Delegate implementation for [PKPaymentAuthorizationControllerDelegateProtocol].
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private class ApplePayAuthorizationDelegate(
    private val request: PaymentRequest,
    private val config: ApplePayConfig,
    private val coroutineScope: CoroutineScope,
    private val deferredResult: CompletableDeferred<PaymentResult>,
    private val customWindowProvider: (() -> UIWindow?)? = null,
    private val paymentParser: (PKPayment, String) -> PaymentResult
) : NSObject(), PKPaymentAuthorizationControllerDelegateProtocol {

    private var authorizedPaymentResult: PaymentResult? = null

    override fun presentationWindowForPaymentAuthorizationController(
        controller: PKPaymentAuthorizationController
    ): UIWindow? {
        customWindowProvider?.invoke()?.let { return it }

        val app = UIApplication.sharedApplication
        val scenes = app.connectedScenes
        for (scene in scenes) {
            if (scene is UIWindowScene && scene.activationState == UISceneActivationStateForegroundActive) {
                val window = scene.keyWindow ?: scene.windows.filterIsInstance<UIWindow>().firstOrNull { it.isKeyWindow() }
                if (window != null) return window
            }
        }
        return app.keyWindow
    }

    override fun paymentAuthorizationController(
        controller: PKPaymentAuthorizationController,
        didAuthorizePayment: PKPayment,
        handler: (PKPaymentAuthorizationResult?) -> Unit
    ) {
        val result = paymentParser(didAuthorizePayment, request.id)
        authorizedPaymentResult = result

        when (result) {
            is PaymentResult.Success -> {
                handler(
                    PKPaymentAuthorizationResult(
                        status = PKPaymentAuthorizationStatus.PKPaymentAuthorizationStatusSuccess,
                        errors = null
                    )
                )
            }
            is PaymentResult.Failure, is PaymentResult.Canceled -> {
                handler(
                    PKPaymentAuthorizationResult(
                        status = PKPaymentAuthorizationStatus.PKPaymentAuthorizationStatusFailure,
                        errors = null
                    )
                )
            }
        }
    }

    override fun paymentAuthorizationController(
        controller: PKPaymentAuthorizationController,
        didRequestMerchantSessionUpdate: (PKPaymentRequestMerchantSessionUpdate?) -> Unit
    ) {
        val provider = config.merchantSessionProvider
        if (provider == null) {
            didRequestMerchantSessionUpdate(
                PKPaymentRequestMerchantSessionUpdate(
                    status = PKPaymentAuthorizationStatus.PKPaymentAuthorizationStatusFailure,
                    merchantSession = null
                )
            )
            return
        }

        coroutineScope.launch {
            try {
                val validationUrl = "https://apple-pay-gateway.apple.com/paymentservices/startSession"
                val sessionJson = provider.provideMerchantSession(validationUrl)
                val bytes = sessionJson.encodeToByteArray()
                val sessionData = bytes.usePinned { pinned ->
                    NSData.dataWithBytes(pinned.addressOf(0), bytes.size.toULong())
                }

                @Suppress("UNCHECKED_CAST")
                val dict = NSJSONSerialization.JSONObjectWithData(
                    data = sessionData,
                    options = 0uL,
                    error = null
                ) as? Map<Any?, *>

                if (dict != null) {
                    val merchantSession = PKPaymentMerchantSession(dictionary = dict)
                    didRequestMerchantSessionUpdate(
                        PKPaymentRequestMerchantSessionUpdate(
                            status = PKPaymentAuthorizationStatus.PKPaymentAuthorizationStatusSuccess,
                            merchantSession = merchantSession
                        )
                    )
                    return@launch
                }

                didRequestMerchantSessionUpdate(
                    PKPaymentRequestMerchantSessionUpdate(
                        status = PKPaymentAuthorizationStatus.PKPaymentAuthorizationStatusFailure,
                        merchantSession = null
                    )
                )
            } catch (e: Throwable) {
                if (e is CancellationException) throw e
                didRequestMerchantSessionUpdate(
                    PKPaymentRequestMerchantSessionUpdate(
                        status = PKPaymentAuthorizationStatus.PKPaymentAuthorizationStatusFailure,
                        merchantSession = null
                    )
                )
            }
        }
    }

    override fun paymentAuthorizationControllerDidFinish(controller: PKPaymentAuthorizationController) {
        controller.dismissWithCompletion {
            val finalResult = authorizedPaymentResult ?: PaymentResult.Canceled
            if (!deferredResult.isCompleted) {
                deferredResult.complete(finalResult)
            }
        }
    }
}
