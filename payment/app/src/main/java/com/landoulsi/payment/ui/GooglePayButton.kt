package com.landoulsi.payment.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.landoulsi.payment.R

/**
 * Visual themes for the Google Pay button conforming to Google Pay Brand Guidelines.
 */
enum class GooglePayButtonTheme {
    /** Dark button background with light Google Pay branding (standard for light app surfaces). */
    DARK,
    /** Light button background with dark Google Pay branding (standard for dark app surfaces). */
    LIGHT,
    /** Automatically follows the system dark/light theme mode. */
    DYNAMIC
}

/**
 * Button label types supported by Google Pay Brand Guidelines, referencing localized string resources.
 */
enum class GooglePayButtonType(@get:StringRes val labelResId: Int?) {
    /** Buy with Google Pay */
    BUY(R.string.gpay_buy_with),
    /** Pay with Google Pay */
    PAY(R.string.gpay_pay_with),
    /** Checkout with Google Pay */
    CHECKOUT(R.string.gpay_checkout_with),
    /** Order with Google Pay */
    ORDER(R.string.gpay_order_with),
    /** Subscribe with Google Pay */
    SUBSCRIBE(R.string.gpay_subscribe_with),
    /** Plain Google Pay button without text prefix */
    PLAIN(null)
}

/**
 * A Jetpack Compose button component for Google Pay adhering to Google Pay Brand Guidelines.
 *
 * Requirements enforced:
 * - Minimum touch target height of 48dp (default 48dp, customizable).
 * - Official Google Pay logo asset with appropriate light/dark contrast.
 * - Dark, Light, or Dynamic theme support.
 * - Accessibility semantic role and content description.
 * - Loading indicator state while payment is processing.
 * - Localized button labels conforming to Google Pay brand guidelines.
 *
 * @param onClick Callback invoked when the user taps the button.
 * @param modifier Optional [Modifier] for button layout.
 * @param type The type / label for the Google Pay button (default: [GooglePayButtonType.PAY]).
 * @param buttonTheme The theme styling ([GooglePayButtonTheme.DARK], [GooglePayButtonTheme.LIGHT], or [GooglePayButtonTheme.DYNAMIC]).
 * @param enabled Whether the button is interactable.
 * @param isLoading When true, displays a progress spinner inside the button and disables clicks.
 * @param cornerRadius Corner radius of the button background (default: 24dp pill or rounded rectangle).
 * @param minHeight Minimum height conforming to 48dp Google Pay guidelines.
 */
@Composable
fun GooglePayButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    type: GooglePayButtonType = GooglePayButtonType.PAY,
    buttonTheme: GooglePayButtonTheme = GooglePayButtonTheme.DARK,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    cornerRadius: Dp = 24.dp,
    minHeight: Dp = 48.dp
) {
    val isDark = when (buttonTheme) {
        GooglePayButtonTheme.DARK -> true
        GooglePayButtonTheme.LIGHT -> false
        GooglePayButtonTheme.DYNAMIC -> isSystemInDarkTheme()
    }

    val backgroundColor = if (isDark) Color.Black else Color.White
    val contentColor = if (isDark) Color.White else Color.Black
    val borderColor = if (isDark) Color.Transparent else Color(0xFF747775)

    val logoResId = if (isDark) {
        R.drawable.gpay_logo_generic_dark
    } else {
        R.drawable.gpay_logo_generic_light
    }

    val labelText = type.labelResId?.let { stringResource(id = it) }
    val cdText = if (labelText != null) {
        stringResource(id = R.string.gpay_content_description_with_prefix, labelText)
    } else {
        stringResource(id = R.string.gpay_content_description_plain)
    }

    val effectiveEnabled = enabled && !isLoading

    Button(
        onClick = onClick,
        enabled = effectiveEnabled,
        shape = RoundedCornerShape(cornerRadius),
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = contentColor,
            disabledContainerColor = backgroundColor.copy(alpha = 0.6f),
            disabledContentColor = contentColor.copy(alpha = 0.6f)
        ),
        border = if (!isDark) BorderStroke(1.dp, borderColor) else null,
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
        modifier = modifier
            .defaultMinSize(minHeight = minHeight.coerceAtLeast(48.dp))
            .semantics {
                role = Role.Button
                contentDescription = cdText
            }
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = contentColor,
                strokeWidth = 2.5.dp,
                modifier = Modifier.size(24.dp)
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (labelText != null) {
                    Text(
                        text = labelText,
                        color = contentColor,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.SansSerif,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                }
                Image(
                    painter = painterResource(id = logoResId),
                    contentDescription = null,
                    modifier = Modifier.height(24.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF0F0F0)
@Composable
fun GooglePayButtonDarkPreview() {
    Surface(modifier = Modifier.padding(16.dp)) {
        GooglePayButton(
            onClick = {},
            type = GooglePayButtonType.PAY,
            buttonTheme = GooglePayButtonTheme.DARK,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
fun GooglePayButtonLightPreview() {
    Surface(modifier = Modifier.padding(16.dp)) {
        GooglePayButton(
            onClick = {},
            type = GooglePayButtonType.BUY,
            buttonTheme = GooglePayButtonTheme.LIGHT,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Preview(showBackground = true)
@Composable
fun GooglePayButtonLoadingPreview() {
    Surface(modifier = Modifier.padding(16.dp)) {
        GooglePayButton(
            onClick = {},
            isLoading = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
