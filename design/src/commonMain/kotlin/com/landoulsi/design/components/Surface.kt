package com.landoulsi.design.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

enum class SurfaceTone {
    Default,
    Inverted,
}

@Composable
fun DesignSurface(
    modifier: Modifier = Modifier,
    tone: SurfaceTone = SurfaceTone.Default,
    content: @Composable () -> Unit,
) {
    val color = when (tone) {
        SurfaceTone.Default -> MaterialTheme.colorScheme.background
        SurfaceTone.Inverted -> MaterialTheme.colorScheme.inverseSurface
    }
    val contentColor = when (tone) {
        SurfaceTone.Default -> MaterialTheme.colorScheme.onBackground
        SurfaceTone.Inverted -> MaterialTheme.colorScheme.inverseOnSurface
    }
    Surface(
        modifier = modifier,
        color = color,
        contentColor = contentColor,
        content = content,
    )
}
