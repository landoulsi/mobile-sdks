package com.landoulsi.payment.ui.card

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.landoulsi.payment.R
import com.landoulsi.payment.shared.validation.CardFieldError
import com.landoulsi.payment.shared.validation.CardFormState
import com.landoulsi.payment.shared.validation.CardNumberUpdateResult
import com.landoulsi.payment.shared.validation.CvcUpdateResult
import com.landoulsi.payment.shared.validation.ExpiryUpdateResult
import com.landoulsi.payment.ui.theme.PaymentSpacing

/**
 * Card input form providing live formatting, Luhn validation, and submission readiness detection.
 *
 * When all fields are complete and valid, [onFormReady] is invoked with the validated
 * [CardFormState] so the caller can perform tokenization and payment routing.
 * This component does NOT perform tokenization itself -- that responsibility belongs to the
 * orchestrator to maintain consistent ViewModel state transitions (Ready -> Processing -> Result).
 */
@Composable
fun CardInputForm(
    onFormReady: (CardFormState) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    initialState: CardFormState = CardFormState.initial()
) {
    var formState by remember { mutableStateOf(initialState) }

    val expiryFocusRequester = remember { FocusRequester() }
    val cvcFocusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    val requiredError = stringResource(id = R.string.card_field_error_required)
    val incompleteError = stringResource(id = R.string.card_field_error_incomplete)
    val invalidNumberError = stringResource(id = R.string.card_field_error_invalid_card_number)
    val invalidExpiryError = stringResource(id = R.string.card_field_error_invalid_expiry)
    val invalidCvcError = stringResource(id = R.string.card_field_error_invalid_cvc)

    fun CardFieldError.toLocalizedString(): String = when (this) {
        CardFieldError.REQUIRED -> requiredError
        CardFieldError.INCOMPLETE -> incompleteError
        CardFieldError.INVALID_CARD_NUMBER -> invalidNumberError
        CardFieldError.INVALID_EXPIRY -> invalidExpiryError
        CardFieldError.INVALID_CVC -> invalidCvcError
    }

    val showNumberError = formState.numberDisplayError != null
    val showExpiryError = formState.expiryDisplayError != null
    val showCvcError = formState.cvcDisplayError != null

    val onNumberChange = { result: CardNumberUpdateResult ->
        var updated = formState.copy(number = result.newState)
        if (result.networkChanged) {
            updated = updated.copy(
                cvc = updated.cvc.copy(
                    rawValue = "",
                    isValid = false,
                    isComplete = false,
                    error = null
                )
            )
        }
        formState = updated
        if (result.newState.isComplete && result.newState.isValid) {
            expiryFocusRequester.requestFocus()
        }
    }

    val onExpiryChange = { result: ExpiryUpdateResult ->
        val updated = formState.copy(expiry = result.newState)
        formState = updated
        if (result.newState.isComplete && result.newState.isValid) {
            cvcFocusRequester.requestFocus()
        }
    }

    val onCvcChange = { result: CvcUpdateResult ->
        val updated = formState.copy(cvc = result.newState)
        formState = updated
        if (result.newState.isComplete && result.newState.isValid) {
            focusManager.clearFocus()
            val submitted = updated.markSubmissionAttempted()
            formState = submitted
            if (submitted.isFormComplete && submitted.isFormValid) {
                onFormReady(submitted)
            }
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(PaymentSpacing.md)
    ) {
        CardNumberInput(
            state = formState.number,
            onValueChange = onNumberChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            isError = showNumberError,
            supportingText = formState.numberDisplayError?.toLocalizedString(),
            keyboardActions = KeyboardActions(onNext = { expiryFocusRequester.requestFocus() })
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(PaymentSpacing.md)
        ) {
            ExpiryInput(
                state = formState.expiry,
                onValueChange = onExpiryChange,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(expiryFocusRequester),
                enabled = enabled,
                isError = showExpiryError,
                supportingText = formState.expiryDisplayError?.toLocalizedString(),
                keyboardActions = KeyboardActions(onNext = { cvcFocusRequester.requestFocus() })
            )

            CvcInput(
                state = formState.cvc,
                onValueChange = onCvcChange,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(cvcFocusRequester),
                enabled = enabled,
                network = formState.number.network,
                isError = showCvcError,
                supportingText = formState.cvcDisplayError?.toLocalizedString(),
                keyboardActions = KeyboardActions(onDone = {
                    focusManager.clearFocus()
                    val submitted = formState.markSubmissionAttempted()
                    formState = submitted
                    if (submitted.isFormComplete && submitted.isFormValid) {
                        onFormReady(submitted)
                    }
                })
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CardInputFormPreview() {
    Surface(
        modifier = Modifier.padding(PaymentSpacing.lg)
    ) {
        CardInputForm(
            onFormReady = { _ -> }
        )
    }
}
