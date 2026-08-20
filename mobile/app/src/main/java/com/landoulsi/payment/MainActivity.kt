package com.landoulsi.payment

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.landoulsi.payment.shared.network.dto.CardTokenResponse
import com.landoulsi.payment.ui.GooglePayButton
import com.landoulsi.payment.ui.GooglePayButtonTheme
import com.landoulsi.payment.ui.GooglePayButtonType
import com.landoulsi.payment.ui.card.CardInputForm
import com.landoulsi.payment.ui.theme.PaymentsdkTheme

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
            when (val state = uiState) {
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
    modifier: Modifier = Modifier
) {
    var isCardExpanded by remember { mutableStateOf(false) }

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
                    CardInputForm(
                        onCardComplete = { tokenResponse ->
                            onCardComplete?.invoke(tokenResponse)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ProcessingPaymentCard(
    methodType: PaymentMethodType,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(36.dp),
                strokeWidth = 3.dp,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = stringResource(id = R.string.demo_payment_processing),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun PaymentSuccessCard(
    result: PaymentResult.Success,
    onResetClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E3A2F)
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFF34A853), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "✓",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
                Text(
                    text = stringResource(id = R.string.demo_payment_success),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE6F4EA)
                )
            }

            HorizontalDivider(color = Color(0xFF2E5948))

            PaymentDetailRow(label = stringResource(id = R.string.demo_transaction_id), value = result.transactionId, lightText = true)
            PaymentDetailRow(label = stringResource(id = R.string.demo_payment_method), value = result.paymentMethodType.name, lightText = true)
            val last4 = result.last4
            if (last4 != null) {
                PaymentDetailRow(
                    label = stringResource(id = R.string.demo_card_details),
                    value = "${result.cardNetwork?.networkName ?: "Card"} •••• $last4",
                    lightText = true
                )
            }
            val token = result.token
            if (token != null) {
                PaymentDetailRow(
                    label = stringResource(id = R.string.demo_token),
                    value = "${token.take(16)}…",
                    lightText = true
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = onResetClick,
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF34A853),
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(text = stringResource(id = R.string.demo_reset_checkout), fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun PaymentFailureCard(
    failure: PaymentResult.Failure,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(MaterialTheme.colorScheme.error, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "!",
                        color = MaterialTheme.colorScheme.onError,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
                Text(
                    text = stringResource(id = R.string.demo_payment_failed),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }

            Text(
                text = "${failure.errorCode.name}: ${failure.message}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )

            Button(
                onClick = onRetryClick,
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(text = stringResource(id = R.string.demo_try_again), fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun PaymentCanceledCard(
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(id = R.string.demo_payment_canceled),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "The payment sheet was dismissed. You can retry when ready.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedButton(
                onClick = onRetryClick,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                Text(text = stringResource(id = R.string.demo_try_again))
            }
        }
    }
}

@Composable
fun PaymentDetailRow(
    label: String,
    value: String,
    lightText: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = if (lightText) Color(0xFFA8D5BA) else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = if (lightText) Color.White else MaterialTheme.colorScheme.onSurface
        )
    }
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