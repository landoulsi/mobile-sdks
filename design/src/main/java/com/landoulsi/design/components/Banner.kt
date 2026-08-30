package com.landoulsi.design.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.landoulsi.design.Shapes
import com.landoulsi.design.Spacing
import com.landoulsi.design.SuccessGreen

enum class BannerTone {
    Info,
    Error,
    Success,
}

@Composable
fun DesignBanner(
    text: String,
    modifier: Modifier = Modifier,
    tone: BannerTone = BannerTone.Info,
    dismissLabel: String = "Dismiss",
    onDismiss: (() -> Unit)? = null,
) {
    val colors = when (tone) {
        BannerTone.Info -> BannerColors(
            container = MaterialTheme.colorScheme.tertiaryContainer,
            content = MaterialTheme.colorScheme.onTertiaryContainer,
        )
        BannerTone.Error -> BannerColors(
            container = MaterialTheme.colorScheme.errorContainer,
            content = MaterialTheme.colorScheme.onErrorContainer,
        )
        BannerTone.Success -> BannerColors(
            container = MaterialTheme.colorScheme.primaryContainer,
            content = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }

    Surface(
        modifier = modifier,
        shape = Shapes.small,
        color = colors.container,
        contentColor = colors.content,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f),
            )
            if (onDismiss != null) {
                TextButton(onClick = onDismiss) {
                    Text(
                        text = dismissLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.content,
                    )
                }
            }
        }
    }
}

private class BannerColors(
    val container: Color,
    val content: Color,
)
