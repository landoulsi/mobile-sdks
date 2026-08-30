package com.landoulsi.design.components

import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.landoulsi.design.Shapes
import com.landoulsi.design.SuccessGreen

enum class ButtonTone {
    Primary,
    Secondary,
    Error,
    Success,
    Neutral,
}

private val ButtonMinHeight = 48.dp
private val SpinnerSize = 20.dp
private val SpinnerStrokeWidth = 2.dp

@Composable
fun DesignButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tone: ButtonTone = ButtonTone.Primary,
    enabled: Boolean = true,
    loading: Boolean = false,
    content: @Composable () -> Unit = { Text(text = text, fontWeight = FontWeight.Medium) },
) {
    val colors = when (tone) {
        ButtonTone.Primary -> ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        )
        ButtonTone.Secondary -> ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary,
        )
        ButtonTone.Error -> ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError,
        )
        ButtonTone.Success -> ButtonDefaults.buttonColors(
            containerColor = SuccessGreen,
            contentColor = Color.White,
        )
        ButtonTone.Neutral -> ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    Button(
        onClick = { if (!loading) onClick() },
        modifier = modifier
            .defaultMinSize(minHeight = ButtonMinHeight)
            .semantics {
                if (loading) {
                    contentDescription = text
                }
            },
        shape = Shapes.extraLarge,
        colors = colors,
        enabled = enabled && !loading,
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(SpinnerSize),
                strokeWidth = SpinnerStrokeWidth,
                color = colors.contentColor,
            )
        } else {
            content()
        }
    }
}

@Composable
fun DesignOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    content: @Composable () -> Unit = { Text(text = text, fontWeight = FontWeight.Medium) },
) {
    OutlinedButton(
        onClick = { if (!loading) onClick() },
        modifier = modifier
            .defaultMinSize(minHeight = ButtonMinHeight)
            .semantics {
                if (loading) {
                    contentDescription = text
                }
            },
        shape = Shapes.extraLarge,
        enabled = enabled && !loading,
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(SpinnerSize),
                strokeWidth = SpinnerStrokeWidth,
                color = MaterialTheme.colorScheme.primary,
            )
        } else {
            content()
        }
    }
}
