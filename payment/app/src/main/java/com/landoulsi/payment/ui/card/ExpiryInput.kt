package com.landoulsi.payment.ui.card

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
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
import com.landoulsi.payment.shared.validation.CardValidation
import com.landoulsi.payment.shared.validation.ExpiryState
import com.landoulsi.payment.shared.validation.ExpiryUpdateResult
import com.landoulsi.payment.ui.theme.PaymentRadius

// Opt-in for Material 3 OutlinedTextField APIs that are currently experimental
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpiryInput(
    state: ExpiryState,
    onValueChange: (ExpiryUpdateResult) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: String = "Expiry",
    placeholder: String = "MM/YY",
    supportingText: String? = null,
    isError: Boolean = false,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    val keyboardOptions = KeyboardOptions(
        keyboardType = KeyboardType.Number,
        imeAction = ImeAction.Next
    )

    val visualTransformation = remember { ExpiryVisualTransformation() }

    val trailingIcon: (@Composable () -> Unit)? = when {
        state.isComplete && state.isValid -> {
            {
                Text(
                    text = "✓",
                    color = com.landoulsi.payment.ui.theme.PaymentColorTokens.success,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.clearAndSetSemantics {}
                )
            }
        }
        state.isComplete && !state.isValid -> {
            {
                Text(
                    text = "!",
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.clearAndSetSemantics {}
                )
            }
        }
        else -> null
    }

    val stateDesc = when {
        state.isValid -> "Expiry date, complete, valid"
        state.isComplete -> "Expiry date, complete, invalid"
        else -> "Expiry date, incomplete"
    }

    // rawValue holds the raw 1-4 numeric digits, while ExpiryVisualTransformation applies the "MM/YY" format for display
    OutlinedTextField(
        value = state.rawValue,
        onValueChange = { newText: String ->
            val digitsOnly = newText.filter { it.isDigit() }.take(4)
            val formatted = CardValidation.formatExpiry(digitsOnly)
            val parsed = CardValidation.parseExpiry(formatted)
            val isComplete = CardValidation.isExpiryComplete(formatted)
            val isValid = if (isComplete) CardValidation.isExpiryValid(formatted) else false
            val (month, year) = parsed ?: (null to null)
            val error = if (isComplete && !isValid) "Expired or invalid date" else null

            val newState = ExpiryState(
                rawValue = digitsOnly,
                month = month,
                year = year,
                isValid = isValid,
                isComplete = isComplete,
                error = error
            )
            onValueChange(ExpiryUpdateResult(newState))
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

class ExpiryVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val digitsOnly = text.text.filter { it.isDigit() }.take(4)
        val formatted = CardValidation.formatExpiry(digitsOnly)

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                val clamped = offset.coerceIn(0, digitsOnly.length)
                return when {
                    clamped <= 2 -> clamped
                    else -> (clamped + 1).coerceAtMost(formatted.length)
                }
            }

            override fun transformedToOriginal(offset: Int): Int {
                val clamped = offset.coerceIn(0, formatted.length)
                return when {
                    clamped <= 2 -> clamped
                    else -> (clamped - 1).coerceIn(0, digitsOnly.length)
                }
            }
        }

        return TransformedText(AnnotatedString(formatted), offsetMapping)
    }
}