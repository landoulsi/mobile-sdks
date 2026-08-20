package com.landoulsi.payment.ui.card

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.landoulsi.payment.shared.model.CardNetwork
import com.landoulsi.payment.shared.validation.CardNumberState
import com.landoulsi.payment.shared.validation.CardNumberUpdateResult
import com.landoulsi.payment.shared.validation.CardValidation
import com.landoulsi.payment.ui.theme.PaymentRadius
import com.landoulsi.payment.ui.theme.PaymentSpacing

// Opt-in for Material 3 OutlinedTextField APIs that are currently experimental
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardNumberInput(
    state: CardNumberState,
    onValueChange: (CardNumberUpdateResult) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: String = "Card number",
    placeholder: String = "0000 0000 0000 0000",
    supportingText: String? = null,
    isError: Boolean = false,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    val keyboardOptions = KeyboardOptions(
        keyboardType = KeyboardType.Number,
        imeAction = ImeAction.Next
    )

    val visualTransformation = remember(state.network) { CardNumberVisualTransformation(state.network) }

    val trailingIcon: (@Composable () -> Unit)? = if (state.network != null || state.isComplete) {
        {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(end = PaymentSpacing.xs)
            ) {
                state.network?.let { network ->
                    CardNetworkIcon(network = network)
                }
                if (state.isComplete) {
                    if (state.network != null) {
                        Spacer(modifier = Modifier.width(PaymentSpacing.xs))
                    }
                    if (state.isValid) {
                        Text(
                            text = "✓",
                            color = com.landoulsi.payment.ui.theme.PaymentColorTokens.success,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    } else {
                        Text(
                            text = "!",
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                }
            }
        }
    } else null

    val network = state.network
    val stateDesc = when {
        network != null -> "Card number, ${network.networkName}, ${if (state.isValid) "valid" else if (state.isComplete) "invalid" else "incomplete"}"
        else -> "Card number, ${if (state.isComplete) "complete" else "incomplete"}"
    }

    OutlinedTextField(
        value = state.rawValue,
        onValueChange = { newText: String ->
            val digitsOnly = newText.filter { it.isDigit() }
            val detectedNetwork = CardValidation.detectNetwork(digitsOnly)
            val maxLen = CardValidation.maxCardLength(detectedNetwork)
            val limitedDigits = if (digitsOnly.length > maxLen) digitsOnly.substring(0, maxLen) else digitsOnly

            val isComplete = CardValidation.isCardNumberComplete(limitedDigits)
            val isValid = if (isComplete) CardValidation.isCardNumberValid(limitedDigits) else false
            val error = if (isComplete && !isValid) "Invalid card number" else null

            val newState = CardNumberState(
                rawValue = limitedDigits,
                network = detectedNetwork,
                isValid = isValid,
                isComplete = isComplete,
                error = error
            )
            onValueChange(CardNumberUpdateResult(newState, detectedNetwork != state.network))
        },
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = label
                stateDescription = stateDesc
            },
        enabled = enabled,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        visualTransformation = visualTransformation,
        singleLine = true,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        leadingIcon = null,
        trailingIcon = trailingIcon,
        isError = isError,
        supportingText = if (supportingText != null) { { Text(supportingText) } } else null,
        textStyle = MaterialTheme.typography.bodyLarge,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
            errorBorderColor = MaterialTheme.colorScheme.error
        ),
        shape = RoundedCornerShape(PaymentRadius.md)
    )
}

class CardNumberVisualTransformation(private val network: CardNetwork?) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val digitsOnly = text.text.filter { it.isDigit() }
        val formatted = CardValidation.formatCardNumber(digitsOnly, network)

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                val clampedOffset = offset.coerceIn(0, digitsOnly.length)
                if (network == CardNetwork.AMEX) {
                    // Amex: 4-6-5 (spaces after index 4 and index 10)
                    val spaces = when {
                        clampedOffset < 4 -> 0
                        clampedOffset < 10 -> 1
                        else -> 2
                    }
                    return (clampedOffset + spaces).coerceAtMost(formatted.length)
                } else {
                    // Standard: spaces after every 4 digits (indices 4, 8, 12)
                    val spaces = when {
                        clampedOffset < 4 -> 0
                        clampedOffset < 8 -> 1
                        clampedOffset < 12 -> 2
                        else -> 3
                    }
                    return (clampedOffset + spaces).coerceAtMost(formatted.length)
                }
            }

            override fun transformedToOriginal(offset: Int): Int {
                val clampedOffset = offset.coerceIn(0, formatted.length)
                if (network == CardNetwork.AMEX) {
                    return when {
                        clampedOffset <= 4 -> clampedOffset
                        clampedOffset <= 11 -> (clampedOffset - 1).coerceAtLeast(4)
                        else -> (clampedOffset - 2).coerceAtLeast(10)
                    }.coerceIn(0, digitsOnly.length)
                } else {
                    // Standard: space at 4, 9, 14
                    val spaces = clampedOffset / 5
                    return (clampedOffset - spaces).coerceIn(0, digitsOnly.length)
                }
            }
        }

        return TransformedText(AnnotatedString(formatted), offsetMapping)
    }
}

@Composable
fun CardNetworkIcon(
    network: CardNetwork,
    modifier: Modifier = Modifier
) {
    val (brandText, brandColor) = when (network) {
        CardNetwork.VISA -> "VISA" to com.landoulsi.payment.ui.theme.PaymentColorTokens.cardVisa
        CardNetwork.MASTERCARD -> "MC" to com.landoulsi.payment.ui.theme.PaymentColorTokens.cardMastercard
        CardNetwork.AMEX -> "AMEX" to com.landoulsi.payment.ui.theme.PaymentColorTokens.cardAmex
        CardNetwork.DISCOVER -> "DISC" to com.landoulsi.payment.ui.theme.PaymentColorTokens.cardDiscover
        CardNetwork.JCB -> "JCB" to com.landoulsi.payment.ui.theme.PaymentColorTokens.cardJcb
        CardNetwork.DINERS_CLUB -> "DINERS" to com.landoulsi.payment.ui.theme.PaymentColorTokens.cardDinersClub
        CardNetwork.UNION_PAY -> "UPI" to com.landoulsi.payment.ui.theme.PaymentColorTokens.cardUnionPay
        CardNetwork.INTERAC -> "INTERAC" to com.landoulsi.payment.ui.theme.PaymentColorTokens.cardInterac
    }

    Surface(
        shape = RoundedCornerShape(PaymentRadius.xs),
        color = brandColor.copy(alpha = 0.15f),
        modifier = modifier.semantics {
            contentDescription = "Network: ${network.networkName}"
        }
    ) {
        Text(
            text = brandText,
            color = brandColor,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(horizontal = PaymentSpacing.xs, vertical = PaymentSpacing.xxs)
        )
    }
}