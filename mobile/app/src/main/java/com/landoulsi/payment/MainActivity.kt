package com.landoulsi.payment

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.landoulsi.payment.shared.checkout.CheckoutUiState
import com.landoulsi.payment.shared.checkout.CheckoutViewModel
import com.landoulsi.payment.shared.googlepay.GooglePayPaymentTaskContract
import com.landoulsi.payment.shared.googlepay.GooglePayProvider
import com.landoulsi.payment.shared.model.CardNetwork
import com.landoulsi.payment.shared.model.Currency
import com.landoulsi.payment.shared.model.GooglePayAuthMethod
import com.landoulsi.payment.shared.model.GooglePayBillingAddressFormat
import com.landoulsi.payment.shared.model.GooglePayBillingAddressParameters
import com.landoulsi.payment.shared.model.GooglePayConfig
import com.landoulsi.payment.shared.model.GooglePayEnvironment
import com.landoulsi.payment.shared.model.GooglePayTokenizationSpecification
import com.landoulsi.payment.shared.model.Money
import com.landoulsi.payment.shared.model.PaymentErrorCode
import com.landoulsi.payment.shared.model.PaymentMethodType
import com.landoulsi.payment.shared.model.PaymentRequest
import com.landoulsi.payment.shared.model.PaymentResult
import com.landoulsi.payment.shared.network.dto.CardTokenRequest
import com.landoulsi.payment.shared.network.dto.CardTokenResponse
import com.landoulsi.payment.ui.GooglePayButton
import com.landoulsi.payment.ui.GooglePayButtonTheme
import com.landoulsi.payment.ui.GooglePayButtonType
import com.landoulsi.payment.ui.card.CardInputForm
import com.landoulsi.payment.ui.payment.PaymentCanceledCard
import com.landoulsi.payment.ui.payment.PaymentFailureCard
import com.landoulsi.payment.ui.payment.PaymentSuccessCard
import com.landoulsi.payment.ui.payment.ProcessingPaymentCard
import com.landoulsi.payment.ui.payment.TokenizationErrorBanner
import com.landoulsi.payment.ui.theme.PaymentsdkTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val googlePayConfig = createDemoGooglePayConfig()
        val googlePayProvider = GooglePayProvider(this, googlePayConfig)
        val initialRequest = createDemoPaymentRequest(googlePayConfig)
        val viewModel = CheckoutViewModel(
            initialRequest = initialRequest,
            providers = listOf(googlePayProvider)
        )

        setContent {
            PaymentsdkTheme {
                CheckoutScreen(
                    viewModel = viewModel,
                    googlePayProvider = googlePayProvider
                )
            }
        }
    }

    companion object {
        fun createDemoGooglePayConfig(): GooglePayConfig {
            return GooglePayConfig(
                environment = GooglePayEnvironment.TEST,
                merchantId = "12345678901234567890",
                merchantName = "Payment SDK Demo Store",
                allowedCardNetworks = listOf(
                    CardNetwork.VISA,
                    CardNetwork.MASTERCARD,
                    CardNetwork.AMEX,
                    CardNetwork.DISCOVER
                ),
                allowedAuthMethods = listOf(
                    GooglePayAuthMethod.PAN_ONLY,
                    GooglePayAuthMethod.CRYPTOGRAM_3DS
                ),
                tokenizationSpecification = GooglePayTokenizationSpecification.Gateway.stripe(
                    publishableKey = "pk_test_TYooMQauvdEDq54NiTphI7jx"
                ),
                billingAddressRequired = true,
                billingAddressParameters = GooglePayBillingAddressParameters(
                    format = GooglePayBillingAddressFormat.FULL,
                    phoneNumberRequired = true
                ),
                emailRequired = true
            )
        }

        fun createDemoPaymentRequest(googlePayConfig: GooglePayConfig): PaymentRequest {
            return PaymentRequest(
                id = "order_demo_1001",
                amount = Money.fromMajorUnits(29.99, Currency.USD),
                merchantName = "Payment SDK Demo Store",
                description = "Pro Developer License (1 Year)",
                allowedPaymentMethods = listOf(
                    PaymentMethodType.GOOGLE_PAY,
                    PaymentMethodType.CARD
                ),
                googlePayConfig = googlePayConfig,
                requireBillingAddress = true
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    viewModel: CheckoutViewModel,
    googlePayProvider: GooglePayProvider? = null,
    onPayWithCardClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    val googlePayLauncher = rememberLauncherForActivityResult(
        contract = GooglePayPaymentTaskContract()
    ) { result ->
        viewModel.handlePaymentResult(result)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.demo_checkout_title),
                        fontWeight = FontWeight.SemiBold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    titleContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Order Summary Card
            OrderSummaryCard(request = uiState.request)

            // Dynamic State Presentation
            AnimatedContent(
                targetState = uiState,
                transitionSpec = {
                    fadeIn(spring()) + slideInVertically { it / 4 } togetherWith fadeOut(spring())
                },
                label = "checkout_state"
            ) { state ->
                when (state) {
                    is CheckoutUiState.Initial,
                    is CheckoutUiState.CheckingAvailability -> {
                        CheckingAvailabilityCard()
                    }

                    is CheckoutUiState.Ready -> {
                        ReadyCheckoutSection(
                            state = state,
                            onGooglePayClick = {
                                if (googlePayProvider != null) {
                                    viewModel.startProcessing(PaymentMethodType.GOOGLE_PAY)
                                    val taskInput = googlePayProvider.createPaymentTaskInput(state.request)
                                    googlePayLauncher.launch(taskInput)
                                } else {
                                    viewModel.pay(PaymentMethodType.GOOGLE_PAY)
                                }
                            },
                            onPayWithCardClick = {
                                onPayWithCardClick()
                            },
                            onCardComplete = { tokenResponse ->
                                val card = tokenResponse.card
                                val network = card?.brand?.let { b ->
                                    try {
                                        CardNetwork.valueOf(b.uppercase())
                                    } catch (e: Exception) {
                                        null
                                    }
                                }
                                viewModel.startProcessing(PaymentMethodType.CARD)
                                coroutineScope.launch {
                                    delay(800)
                                    viewModel.handlePaymentResult(
                                        PaymentResult.Success(
                                            transactionId = "tx_card_${tokenResponse.id.takeLast(8)}",
                                            paymentMethodType = PaymentMethodType.CARD,
                                            cardNetwork = network,
                                            last4 = card?.last4,
                                            token = tokenResponse.id
                                        )
                                    )
                                }
                            }
                        )
                    }

                    is CheckoutUiState.Processing -> {
                        ProcessingPaymentCard(
                            methodType = state.paymentMethodType
                        )
                    }

                    is CheckoutUiState.Success -> {
                        PaymentSuccessCard(
                            result = state.result,
                            onResetClick = { viewModel.reset() }
                        )
                    }

                    is CheckoutUiState.Failure -> {
                        PaymentFailureCard(
                            failure = state.failure,
                            onRetryClick = { viewModel.reset() }
                        )
                    }

                    is CheckoutUiState.Canceled -> {
                        PaymentCanceledCard(
                            onRetryClick = { viewModel.reset() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OrderSummaryCard(
    request: PaymentRequest,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = request.merchantName ?: stringResource(id = R.string.demo_merchant_name),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "ID: ${request.id}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = request.description ?: stringResource(id = R.string.demo_order_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total Amount",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = request.amount.formattedWithSymbol(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun CheckingAvailabilityCard() {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.5.dp
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = stringResource(id = R.string.demo_checking_wallets),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun ReadyCheckoutSection(
    state: CheckoutUiState.Ready,
    onGooglePayClick: () -> Unit,
    onPayWithCardClick: () -> Unit = {},
    onCardComplete: ((CardTokenResponse) -> Unit)? = null,
    onCardTokenizeError: ((Throwable) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var isCardExpanded by remember { mutableStateOf(false) }
    var isTokenizing by remember { mutableStateOf(false) }
    var tokenizationError by remember { mutableStateOf<Throwable?>(null) }
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Express Wallets First (Google Pay)
        if (state.isGooglePayAvailable) {
            GooglePayButton(
                onClick = onGooglePayClick,
                type = GooglePayButtonType.PAY,
                buttonTheme = GooglePayButtonTheme.DARK,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(id = R.string.demo_gpay_unavailable),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        // Divider
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.outlineVariant
            )
            Text(
                text = stringResource(id = R.string.demo_or_pay_with_card),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.outlineVariant
            )
        }

        // Secondary / Card Checkout Option
        if (!isCardExpanded) {
            OutlinedButton(
                onClick = {
                    isCardExpanded = true
                    onPayWithCardClick()
                },
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.demo_pay_with_card),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        } else {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.demo_pay_with_card),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Box {
                        CardInputForm(
                            onFormReady = { formState ->
                                if (isTokenizing) return@CardInputForm

                                val month = formState.expiry.month
                                val rawYear = formState.expiry.year
                                if (month != null && rawYear != null && month in 1..12) {
                                    val fullYear = if (rawYear in 0..99) 2000 + rawYear else rawYear
                                    val rawPan = formState.number.rawValue
                                    val rawCvc = formState.cvc.rawValue
                                    val cardholderName = formState.cardholderName.takeIf { it.isNotBlank() }
                                    val detectedNetwork = formState.number.network

                                    val tokenRequest = CardTokenRequest(
                                        number = rawPan,
                                        expiryMonth = month,
                                        expiryYear = fullYear,
                                        cvc = rawCvc,
                                        cardholderName = cardholderName
                                    )

                                    tokenizationError = null
                                    isTokenizing = true

                                    coroutineScope.launch {
                                        try {
                                            val tokenResponse = createMockTokenResponse(
                                                tokenRequest, detectedNetwork
                                            )
                                            onCardComplete?.invoke(tokenResponse)
                                        } catch (t: Throwable) {
                                            tokenizationError = t
                                            onCardTokenizeError?.invoke(t)
                                        } finally {
                                            isTokenizing = false
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isTokenizing
                        )

                        if (isTokenizing) {
                            val processingDesc = stringResource(id = R.string.demo_payment_processing_card)
                            Surface(
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                                modifier = Modifier
                                    .matchParentSize()
                                    .semantics {
                                        contentDescription = processingDesc
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(36.dp),
                                        strokeWidth = 3.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }

                    if (tokenizationError != null) {
                        TokenizationErrorBanner(
                            error = tokenizationError,
                            onDismiss = { tokenizationError = null },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

private fun createMockTokenResponse(
    request: CardTokenRequest,
    network: com.landoulsi.payment.shared.model.CardNetwork?
): CardTokenResponse {
    val last4 = request.number.takeLast(4)
    return CardTokenResponse(
        id = "tok_card_${last4}_${System.currentTimeMillis()}",
        `object` = "token",
        created = System.currentTimeMillis() / 1000,
        livemode = false,
        type = "card",
        card = com.landoulsi.payment.shared.network.dto.CardDetails(
            brand = network?.name?.lowercase(),
            last4 = last4,
            expMonth = request.expiryMonth,
            expYear = request.expiryYear
        )
    )
}

@Preview(showBackground = true)
@Composable
fun CheckoutScreenReadyPreview() {
    PaymentsdkTheme {
        val config = MainActivity.createDemoGooglePayConfig()
        val request = MainActivity.createDemoPaymentRequest(config)
        val viewModel = CheckoutViewModel(request)
        CheckoutScreen(viewModel = viewModel)
    }
}