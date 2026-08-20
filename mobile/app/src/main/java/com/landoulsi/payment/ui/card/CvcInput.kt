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
import com.landoulsi.payment.shared.validation.CardValidation
import com.landoulsi.payment.shared.validation.CvcState
import com.landoulsi.payment.shared.validation.CvcUpdateResult
import com.landoulsi.payment.ui.theme.PaymentRadius

// Opt-in for Material 3 OutlinedTextField APIs that are currently experimental
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CvcInput(
    state: CvcState,
    onValueChange: (CvcUpdateResult) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: String = "CVC",
    placeholder: String = "CVC",
    supportingText: String? = null,
    isError: Boolean = false,
    network: CardNetwork? = null,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    val keyboardOptions = KeyboardOptions(
        keyboardType = KeyboardType.Number,
        imeAction = ImeAction.Done
    )

    val visualTransformation = remember { CvcVisualTransformation() }

    val trailingIcon: (@Composable () -> Unit)? = when {
        state.isComplete && state.isValid -> {
            {
                Text(
                    text = "✓",
                    color = com.landoulsi.payment.ui.theme.PaymentColorTokens.success,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        }
        state.isComplete && !state.isValid -> {
            {
                Text(
                    text = "!",
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        }
        else -> null
    }

    val expectedLength = CardValidation.cvcLength(network)
    val stateDesc = when {
        state.isValid -> "CVC, $expectedLength digits, complete, valid"
        state.isComplete -> "CVC, $expectedLength digits, complete, invalid"
        else -> "CVC, $expectedLength digits, incomplete"
    }

    OutlinedTextField(
        value = state.rawValue,
        onValueChange = { newText: String ->
            val digitsOnly = newText.filter { it.isDigit() }
            val formatted = CardValidation.formatCvc(digitsOnly, network)
            val isComplete = CardValidation.isCvcComplete(formatted, network)
            val isValid = if (isComplete) CardValidation.isCvcValid(formatted, network) else false
            val error = if (isComplete && !isValid) "Invalid CVC" else null

            val newState = CvcState(
                rawValue = formatted,
                isValid = isValid,
                isComplete = isComplete,
                error = error
            )
            onValueChange(CvcUpdateResult(newState))
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

class CvcVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val masked = "•".repeat(text.text.length)
        return TransformedText(
            AnnotatedString(masked),
            OffsetMapping.Identity
        )
    }
}