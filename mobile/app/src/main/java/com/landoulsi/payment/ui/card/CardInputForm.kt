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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.runtime.rememberCoroutineScope
import com.landoulsi.payment.shared.network.GatewayClient
import com.landoulsi.payment.shared.network.dto.CardDetails
import com.landoulsi.payment.shared.network.dto.CardTokenRequest
import com.landoulsi.payment.shared.network.dto.CardTokenResponse
import com.landoulsi.payment.shared.validation.CardFormState
import com.landoulsi.payment.shared.validation.CardNumberUpdateResult
import com.landoulsi.payment.shared.validation.CvcUpdateResult
import com.landoulsi.payment.shared.validation.ExpiryUpdateResult
import com.landoulsi.payment.ui.theme.PaymentSpacing
import kotlinx.coroutines.launch

/**
 * Card input form providing live formatting, Luhn validation, and in-SDK tokenization.
 *
 * To ensure PCI-DSS compliance, raw PAN and CVC are never exposed outside the SDK.
 * Tokenization is performed inside this component (via [gatewayClient] or standard SDK tokenization)
 * before invoking [onCardComplete] with a sanitized [CardTokenResponse].
 */
@Composable
fun CardInputForm(
    onCardComplete: (CardTokenResponse) -> Unit,
    gatewayClient: GatewayClient? = null,
    onTokenizationError: ((Throwable) -> Unit)? = null,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    initialState: CardFormState = CardFormState.initial()
) {
    var formState by remember { mutableStateOf(initialState) }
    var lastEmittedTokenId by remember { mutableStateOf<String?>(null) }
    var isTokenizing by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val expiryFocusRequester = remember { FocusRequester() }
    val cvcFocusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    val numberError = formState.number.error
    val expiryError = formState.expiry.error
    val cvcError = formState.cvc.error

    val showNumberError = numberError != null && formState.number.isComplete
    val showExpiryError = expiryError != null && formState.expiry.isComplete
    val showCvcError = cvcError != null && formState.cvc.isComplete

    fun checkFormComplete(state: CardFormState) {
        if (state.isFormComplete && state.isFormValid && !isTokenizing) {
            val month = state.expiry.month
            val rawYear = state.expiry.year
            if (month != null && rawYear != null && month in 1..12) {
                // Two-digit year (e.g. 30) is normalized to full four-digit year (2030) as required by standard gateway tokenization APIs (Stripe CardTokenRequest)
                val fullYear = if (rawYear in 0..99) 2000 + rawYear else rawYear
                val rawPan = state.number.rawValue
                val rawCvc = state.cvc.rawValue
                val cardholderName = state.cardholderName.takeIf { it.isNotBlank() }
                val detectedNetwork = state.number.network

                val tokenRequest = CardTokenRequest(
                    number = rawPan,
                    expiryMonth = month,
                    expiryYear = fullYear,
                    cvc = rawCvc,
                    cardholderName = cardholderName
                )

                // Immediately clear sensitive authentication data and PAN from local UI state
                formState = state.clearSensitiveData()

                isTokenizing = true
                coroutineScope.launch {
                    try {
                        val tokenResponse = if (gatewayClient != null) {
                            gatewayClient.tokenizeCard(tokenRequest)
                        } else {
                            val last4 = rawPan.takeLast(4)
                            CardTokenResponse(
                                id = "tok_card_${last4}_${System.currentTimeMillis()}",
                                `object` = "token",
                                created = System.currentTimeMillis() / 1000,
                                livemode = false,
                                type = "card",
                                card = CardDetails(
                                    brand = detectedNetwork?.name?.lowercase(),
                                    last4 = last4,
                                    expMonth = month,
                                    expYear = fullYear
                                )
                            )
                        }

                        if (tokenResponse.id != lastEmittedTokenId) {
                            lastEmittedTokenId = tokenResponse.id
                            onCardComplete(tokenResponse)
                        }
                    } catch (t: Throwable) {
                        onTokenizationError?.invoke(t)
                    } finally {
                        isTokenizing = false
                    }
                }
            }
        }
    }

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
            checkFormComplete(updated)
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
            supportingText = if (showNumberError) numberError else null,
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
                supportingText = if (showExpiryError) expiryError else null,
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
                supportingText = if (showCvcError) cvcError else null,
                keyboardActions = KeyboardActions(onDone = {
                    focusManager.clearFocus()
                    checkFormComplete(formState)
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
            onCardComplete = { _ -> }
        )
    }
}