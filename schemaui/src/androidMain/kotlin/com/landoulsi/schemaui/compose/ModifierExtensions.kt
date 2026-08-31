package com.landoulsi.schemaui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.landoulsi.schemaui.ir.UIAlignment
import com.landoulsi.schemaui.ir.UIContentScale
import com.landoulsi.schemaui.ir.UIFontWeight
import com.landoulsi.schemaui.ir.UIHorizontalAlignment
import com.landoulsi.schemaui.ir.UIModifiers
import com.landoulsi.schemaui.ir.UITextAlign
import com.landoulsi.schemaui.ir.UIVerticalAlignment

// ─── UIModifiers → Compose Modifier ──────────────────────────────────────────

/**
 * Converts platform-agnostic [UIModifiers] into a Jetpack Compose [Modifier] chain.
 * Apply this before any layout modifiers specific to each node.
 */
internal fun UIModifiers.toComposeModifier(): Modifier {
    var modifier: Modifier = Modifier

    // Size (fill before fixed, matching Compose precedence)
    if (fillMaxWidth) modifier = modifier.fillMaxWidth()
    if (fillMaxHeight) modifier = modifier.fillMaxHeight()
    width?.let { modifier = modifier.width(it.dp) }
    height?.let { modifier = modifier.height(it.dp) }
    minWidth?.let { modifier = modifier.widthIn(min = it.dp) }
    minHeight?.let { modifier = modifier.heightIn(min = it.dp) }

    // Background and clipping (applied before padding so background is under padding area)
    val shape = if (cornerRadius > 0f) RoundedCornerShape(cornerRadius.dp) else RectangleShape
    val resolvedBackgroundColor = backgroundColor?.hexToComposeColor()
    if (resolvedBackgroundColor != null) {
        modifier = modifier.background(resolvedBackgroundColor, shape)
    }
    if (clip) {
        modifier = modifier.clip(shape)
    }

    // Padding
    val hasPadding = paddingStart > 0f || paddingEnd > 0f || paddingTop > 0f || paddingBottom > 0f
    if (hasPadding) {
        modifier = modifier.padding(
            start = paddingStart.dp,
            end = paddingEnd.dp,
            top = paddingTop.dp,
            bottom = paddingBottom.dp,
        )
    }

    // Alpha
    if (alpha < 1f) modifier = modifier.alpha(alpha)

    return modifier
}

// ─── Alignment Converters ─────────────────────────────────────────────────────

internal fun UIHorizontalAlignment.toComposeHorizontalAlignment(): Alignment.Horizontal =
    when (this) {
        UIHorizontalAlignment.Start -> Alignment.Start
        UIHorizontalAlignment.Center -> Alignment.CenterHorizontally
        UIHorizontalAlignment.End -> Alignment.End
    }

internal fun UIVerticalAlignment.toComposeVerticalAlignment(): Alignment.Vertical =
    when (this) {
        UIVerticalAlignment.Top -> Alignment.Top
        UIVerticalAlignment.Center -> Alignment.CenterVertically
        UIVerticalAlignment.Bottom -> Alignment.Bottom
    }

internal fun UIAlignment.toComposeAlignment(): Alignment =
    when (this) {
        UIAlignment.TopStart -> Alignment.TopStart
        UIAlignment.TopCenter -> Alignment.TopCenter
        UIAlignment.TopEnd -> Alignment.TopEnd
        UIAlignment.CenterStart -> Alignment.CenterStart
        UIAlignment.Center -> Alignment.Center
        UIAlignment.CenterEnd -> Alignment.CenterEnd
        UIAlignment.BottomStart -> Alignment.BottomStart
        UIAlignment.BottomCenter -> Alignment.BottomCenter
        UIAlignment.BottomEnd -> Alignment.BottomEnd
    }

// ─── ContentScale Converter ───────────────────────────────────────────────────

internal fun UIContentScale.toComposeContentScale(): ContentScale = when (this) {
    UIContentScale.Fit -> ContentScale.Fit
    UIContentScale.Crop -> ContentScale.Crop
    UIContentScale.Inside -> ContentScale.Inside
    UIContentScale.FillBounds -> ContentScale.FillBounds
    UIContentScale.None -> ContentScale.None
}

// ─── FontWeight Converter ─────────────────────────────────────────────────────

internal fun UIFontWeight.toComposeFontWeight(): FontWeight =
    when (this) {
        UIFontWeight.Thin -> FontWeight.Thin
        UIFontWeight.Light -> FontWeight.Light
        UIFontWeight.Normal -> FontWeight.Normal
        UIFontWeight.Medium -> FontWeight.Medium
        UIFontWeight.SemiBold -> FontWeight.SemiBold
        UIFontWeight.Bold -> FontWeight.Bold
        UIFontWeight.ExtraBold -> FontWeight.ExtraBold
        UIFontWeight.Black -> FontWeight.Black
    }

internal fun UITextAlign.toComposeTextAlign(): TextAlign =
    when (this) {
        UITextAlign.Start -> TextAlign.Start
        UITextAlign.Center -> TextAlign.Center
        UITextAlign.End -> TextAlign.End
        UITextAlign.Justify -> TextAlign.Justify
    }

/**
 * Parses a hex color string (e.g. "FF1A2B3C", "#1A2B3C", "1A2B3C") into a Compose [Color].
 * Supports 6-digit (RRGGBB, assumed 100% alpha) and 8-digit (AARRGGBB) hex formats.
 * Returns null if the string is malformed.
 */
internal fun String.hexToComposeColor(): Color? = try {
    val clean = this.removePrefix("#")
    val normalized = when (clean.length) {
        6 -> "FF$clean"
        8 -> clean
        else -> return null
    }
    val value = normalized.toLong(16)
    Color(
        alpha = ((value shr 24) and 0xFF).toFloat() / 255f,
        red = ((value shr 16) and 0xFF).toFloat() / 255f,
        green = ((value shr 8) and 0xFF).toFloat() / 255f,
        blue = (value and 0xFF).toFloat() / 255f,
    )
} catch (_: NumberFormatException) {
    null
}
