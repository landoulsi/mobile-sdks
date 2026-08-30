package com.landoulsi.design.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import com.landoulsi.design.Elevation
import com.landoulsi.design.Shapes

enum class CardTone {
    Neutral,
    Primary,
    Secondary,
    Error,
}

@Composable
fun DesignCard(
    modifier: Modifier = Modifier,
    tone: CardTone = CardTone.Neutral,
    shape: Shape = Shapes.large,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = when (tone) {
        CardTone.Neutral -> CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        )
        CardTone.Primary -> CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        )
        CardTone.Secondary -> CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        )
        CardTone.Error -> CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
        )
    }
    Card(
        modifier = modifier,
        shape = shape,
        colors = colors,
        content = content,
    )
}

@Composable
fun DesignElevatedCard(
    modifier: Modifier = Modifier,
    tone: CardTone = CardTone.Neutral,
    shape: Shape = Shapes.large,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = when (tone) {
        CardTone.Neutral -> CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        )
        CardTone.Primary -> CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        )
        CardTone.Secondary -> CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        )
        CardTone.Error -> CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
        )
    }
    Card(
        modifier = modifier,
        shape = shape,
        colors = colors,
        elevation = CardDefaults.cardElevation(defaultElevation = Elevation.xs),
        content = content,
    )
}

@Composable
fun DesignOutlinedCard(
    modifier: Modifier = Modifier,
    tone: CardTone = CardTone.Neutral,
    shape: Shape = Shapes.large,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = when (tone) {
        CardTone.Neutral -> CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        )
        CardTone.Primary -> CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        )
        CardTone.Secondary -> CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        )
        CardTone.Error -> CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
        )
    }
    OutlinedCard(
        modifier = modifier,
        shape = shape,
        colors = colors,
        border = BorderStroke(
            width = androidx.compose.ui.unit.Dp.Hairline,
            color = MaterialTheme.colorScheme.outlineVariant,
        ),
        content = content,
    )
}
