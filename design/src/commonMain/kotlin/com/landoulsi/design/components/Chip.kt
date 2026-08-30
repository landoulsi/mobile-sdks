package com.landoulsi.design.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.landoulsi.design.Shapes
import com.landoulsi.design.Spacing
import com.landoulsi.design.SuccessGreen

enum class BadgeTone {
    Neutral,
    Primary,
    Secondary,
    Tertiary,
    Success,
    Error,
}

@Composable
private fun resolveBadgeColors(tone: BadgeTone): ChipColors {
    return when (tone) {
        BadgeTone.Neutral -> ChipColors(
            container = MaterialTheme.colorScheme.surfaceVariant,
            content = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        BadgeTone.Primary -> ChipColors(
            container = MaterialTheme.colorScheme.primaryContainer,
            content = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        BadgeTone.Secondary -> ChipColors(
            container = MaterialTheme.colorScheme.secondaryContainer,
            content = MaterialTheme.colorScheme.onSecondaryContainer,
        )
        BadgeTone.Tertiary -> ChipColors(
            container = MaterialTheme.colorScheme.tertiaryContainer,
            content = MaterialTheme.colorScheme.onTertiaryContainer,
        )
        BadgeTone.Success -> ChipColors(
            container = SuccessGreen.copy(alpha = 0.15f),
            content = SuccessGreen,
        )
        BadgeTone.Error -> ChipColors(
            container = MaterialTheme.colorScheme.errorContainer,
            content = MaterialTheme.colorScheme.onErrorContainer,
        )
    }
}

@Composable
fun DesignChip(
    text: String,
    modifier: Modifier = Modifier,
    tone: BadgeTone = BadgeTone.Neutral,
) {
    val colors = resolveBadgeColors(tone)
    Surface(
        modifier = modifier,
        shape = Shapes.small,
        color = colors.container,
    ) {
        Text(
            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs),
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = colors.content,
        )
    }
}

@Composable
fun StatusBadge(
    text: String,
    modifier: Modifier = Modifier,
    tone: BadgeTone = BadgeTone.Neutral,
) {
    val colors = resolveBadgeColors(tone)
    Surface(
        modifier = modifier,
        shape = Shapes.extraLarge,
        color = colors.container,
    ) {
        Text(
            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs),
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = colors.content,
        )
    }
}

private class ChipColors(
    val container: Color,
    val content: Color,
)
