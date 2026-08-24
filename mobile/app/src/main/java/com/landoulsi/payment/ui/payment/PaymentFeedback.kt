package com.landoulsi.payment.ui.payment

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.landoulsi.payment.R
import com.landoulsi.payment.shared.model.PaymentErrorCode
import com.landoulsi.payment.shared.model.PaymentMethodType
import com.landoulsi.payment.shared.model.PaymentResult
import com.landoulsi.payment.ui.theme.PaymentColorTokens

@Composable
fun ProcessingPaymentCard(
    methodType: PaymentMethodType,
    modifier: Modifier = Modifier
) {
    val subtitle = when (methodType) {
        PaymentMethodType.GOOGLE_PAY -> stringResource(id = R.string.demo_payment_processing_google_pay)
        PaymentMethodType.CARD -> stringResource(id = R.string.demo_payment_processing_card)
        else -> stringResource(id = R.string.demo_payment_processing)
    }
    val contentDesc = stringResource(id = R.string.demo_payment_processing)

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
                .padding(28.dp)
                .semantics(mergeDescendants = true) {
                    contentDescription = contentDesc
                    liveRegion = androidx.compose.ui.semantics.LiveRegionMode.Polite
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
    val scale = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 300, easing = LinearEasing)
        )
    }

    val paymentMethodLabel = when (result.paymentMethodType) {
        PaymentMethodType.GOOGLE_PAY -> stringResource(id = R.string.payment_method_google_pay)
        PaymentMethodType.CARD -> stringResource(id = R.string.payment_method_card)
        else -> result.paymentMethodType.name
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        modifier = modifier
            .fillMaxWidth()
            .scale(scale.value)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .semantics(mergeDescendants = true) {
                    liveRegion = androidx.compose.ui.semantics.LiveRegionMode.Polite
                },
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(PaymentColorTokens.success, CircleShape)
                        .clearAndSetSemantics {},
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "\u2713",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
                Text(
                    text = stringResource(id = R.string.demo_payment_success),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.semantics { heading() }
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            PaymentDetailRow(label = stringResource(id = R.string.demo_transaction_id), value = result.transactionId, lightText = true)
            PaymentDetailRow(label = stringResource(id = R.string.demo_payment_method), value = paymentMethodLabel, lightText = true)
            val last4 = result.last4
            if (last4 != null) {
                PaymentDetailRow(
                    label = stringResource(id = R.string.demo_card_details),
                    value = "${result.cardNetwork?.networkName ?: "Card"} \u2022\u2022\u2022\u2022 $last4",
                    lightText = true
                )
            }
            val token = result.token
            if (token != null) {
                PaymentDetailRow(
                    label = stringResource(id = R.string.demo_token),
                    value = "${token.take(16)}\u2026",
                    lightText = true
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = onResetClick,
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PaymentColorTokens.success,
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
    val errorCodeLabel = when (failure.errorCode) {
        PaymentErrorCode.NETWORK_ERROR -> stringResource(id = R.string.payment_error_network)
        PaymentErrorCode.GATEWAY_ERROR -> stringResource(id = R.string.payment_error_gateway)
        PaymentErrorCode.CONFIGURATION_ERROR -> stringResource(id = R.string.payment_error_configuration)
        PaymentErrorCode.PAYMENT_METHOD_UNAVAILABLE -> stringResource(id = R.string.payment_error_method_unavailable)
        PaymentErrorCode.CARD_DECLINED -> stringResource(id = R.string.payment_error_card_declined)
        PaymentErrorCode.EXPIRED_CARD -> stringResource(id = R.string.payment_error_expired_card)
        PaymentErrorCode.INSUFFICIENT_FUNDS -> stringResource(id = R.string.payment_error_insufficient_funds)
        PaymentErrorCode.AUTHENTICATION_FAILED -> stringResource(id = R.string.payment_error_authentication)
        PaymentErrorCode.USER_CANCELED -> stringResource(id = R.string.payment_error_user_canceled)
        PaymentErrorCode.UNKNOWN -> stringResource(id = R.string.payment_error_unknown)
    }

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
                .padding(24.dp)
                .semantics(mergeDescendants = true) {
                    liveRegion = androidx.compose.ui.semantics.LiveRegionMode.Polite
                },
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(MaterialTheme.colorScheme.error, CircleShape)
                        .clearAndSetSemantics {},
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
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.semantics { heading() }
                )
            }

            Text(
                text = "$errorCodeLabel: ${failure.message}",
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
                .padding(20.dp)
                .semantics(mergeDescendants = true) {
                    liveRegion = androidx.compose.ui.semantics.LiveRegionMode.Polite
                },
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(id = R.string.demo_payment_canceled),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.semantics { heading() }
            )
            Text(
                text = stringResource(id = R.string.demo_canceled_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedButton(
                onClick = onRetryClick,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
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
            color = if (lightText) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = if (lightText) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun TokenizationErrorBanner(
    error: Throwable?,
    onDismiss: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val errorMessage = error?.localizedMessage
        ?: error?.message
        ?: stringResource(id = R.string.demo_tokenization_error_default)
    Surface(
        modifier = modifier.semantics(mergeDescendants = true) {
            liveRegion = androidx.compose.ui.semantics.LiveRegionMode.Polite
        },
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f)
            )
            if (onDismiss != null) {
                androidx.compose.material3.TextButton(onClick = onDismiss) {
                    Text(
                        text = stringResource(id = R.string.demo_tokenization_error_dismiss),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}
