package com.landoulsi.design

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.Dp

fun Modifier.designPaddingAll(size: Dp = Spacing.md): Modifier =
    padding(size)

fun Modifier.designPaddingHorizontal(size: Dp = Spacing.md): Modifier =
    padding(horizontal = size)

fun Modifier.designPaddingVertical(size: Dp = Spacing.md): Modifier =
    padding(vertical = size)

fun Modifier.designCardShape(): Modifier = clip(Shapes.large)

fun Modifier.designSurfaceShape(): Modifier = clip(Shapes.medium)

fun Modifier.designChipShape(): Modifier = clip(Shapes.small)

fun Modifier.designPillShape(): Modifier = clip(Shapes.extraLarge)

fun Modifier.designCardElevation(): Modifier = shadow(
    elevation = Elevation.sm,
    shape = Shapes.large,
    clip = true,
)
