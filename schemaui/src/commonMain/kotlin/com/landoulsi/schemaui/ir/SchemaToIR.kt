package com.landoulsi.schemaui.ir

import com.landoulsi.schemaui.schema.AlignmentModifier
import com.landoulsi.schemaui.schema.BoxSchemaNode
import com.landoulsi.schemaui.schema.ButtonSchemaNode
import com.landoulsi.schemaui.schema.ColumnSchemaNode
import com.landoulsi.schemaui.schema.ImageSchemaNode
import com.landoulsi.schemaui.schema.ListSchemaNode
import com.landoulsi.schemaui.schema.PaddingModifier
import com.landoulsi.schemaui.schema.RowSchemaNode
import com.landoulsi.schemaui.schema.SchemaModifiers
import com.landoulsi.schemaui.schema.SchemaNode
import com.landoulsi.schemaui.schema.SizeModifier
import com.landoulsi.schemaui.schema.SpacerSchemaNode
import com.landoulsi.schemaui.schema.TextFieldSchemaNode
import com.landoulsi.schemaui.schema.TextSchemaNode
import com.landoulsi.schemaui.schema.TextStyle
import com.landoulsi.schemaui.schema.UnknownSchemaNode

/** Thrown when the SchemaNode → UINode conversion detects a structural error. */
class SchemaToIRException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Converts a raw [SchemaNode] tree into the platform-agnostic [UINode] IR tree.
 *
 * Responsibilities:
 * - Apply modifier defaults (expand padding shorthands, clamp values)
 * - Map string discriminators to enums (fail gracefully with defaults for unknown strings)
 * - Recursively convert children
 */
fun SchemaNode.toIR(): UINode = when (this) {
    is ColumnSchemaNode -> UIColumn(
        children = children.map { it.toIR() },
        verticalArrangement = verticalArrangement.toVerticalArrangement(),
        horizontalAlignment = modifiers?.alignment.toHorizontalAlignment(),
        modifiers = modifiers.toUIModifiers(),
    )

    is RowSchemaNode -> UIRow(
        children = children.map { it.toIR() },
        horizontalArrangement = horizontalArrangement.toHorizontalArrangement(),
        verticalAlignment = modifiers?.alignment.toVerticalAlignment(),
        modifiers = modifiers.toUIModifiers(),
    )

    is BoxSchemaNode -> UIBox(
        children = children.map { it.toIR() },
        contentAlignment = modifiers?.alignment.toBoxAlignment(),
        modifiers = modifiers.toUIModifiers(),
    )

    is TextSchemaNode -> UIText(
        text = text,
        style = style.toUITextStyle(),
        modifiers = modifiers.toUIModifiers(),
    )

    is ImageSchemaNode -> UIImage(
        url = url,
        resource = resource,
        contentDescription = contentDescription,
        contentScale = contentScale.toContentScale(),
        modifiers = modifiers.toUIModifiers(),
    )

    is ButtonSchemaNode -> UIButton(
        label = label,
        action = action,
        style = style.toButtonStyle(),
        icon = icon,
        modifiers = modifiers.toUIModifiers(),
    )

    is TextFieldSchemaNode -> UITextField(
        label = label,
        placeholder = placeholder,
        stateKey = stateKey,
        action = action,
        inputType = inputType.toInputType(),
        modifiers = modifiers.toUIModifiers(),
    )

    is SpacerSchemaNode -> UISpacer(
        width = width,
        height = height,
        modifiers = modifiers.toUIModifiers(),
    )

    is ListSchemaNode -> UIList(
        items = items.map { it.toIR() },
        dividers = dividers,
        modifiers = modifiers.toUIModifiers(),
    )

    is UnknownSchemaNode -> UIUnknown(
        originalType = originalType,
        modifiers = modifiers.toUIModifiers(),
    )
}

// ─── Modifier Conversion ──────────────────────────────────────────────────────

internal fun SchemaModifiers?.toUIModifiers(): UIModifiers {
    if (this == null) return UIModifiers.None
    return UIModifiers(
        paddingStart = padding.resolve(
            specific = padding?.start,
            axis = padding?.horizontal,
            all = padding?.all,
        ),
        paddingEnd = padding.resolve(
            specific = padding?.end,
            axis = padding?.horizontal,
            all = padding?.all,
        ),
        paddingTop = padding.resolve(
            specific = padding?.top,
            axis = padding?.vertical,
            all = padding?.all,
        ),
        paddingBottom = padding.resolve(
            specific = padding?.bottom,
            axis = padding?.vertical,
            all = padding?.all,
        ),
        width = size?.width,
        height = size?.height,
        fillMaxWidth = size?.fillMaxWidth ?: false,
        fillMaxHeight = size?.fillMaxHeight ?: false,
        minWidth = size?.minWidth,
        minHeight = size?.minHeight,
        backgroundColor = background?.color?.normalizeHexColor(),
        cornerRadius = background?.cornerRadius ?: 0f,
        alpha = (alpha ?: 1f).coerceIn(0f, 1f),
        clip = clip,
    )
}

/**
 * Resolves a padding value with specificity: specific > axis > all > 0
 */
private fun PaddingModifier?.resolve(
    specific: Float?,
    axis: Float?,
    all: Float?,
): Float = specific ?: axis ?: all ?: 0f

/**
 * Normalizes hex color strings to "AARRGGBB" format (without '#').
 * Supports: "#RGB", "#RRGGBB", "#AARRGGBB"
 */
internal fun String.normalizeHexColor(): String {
    val clean = trimStart('#').uppercase()
    return when (clean.length) {
        3 -> "FF${clean[0]}${clean[0]}${clean[1]}${clean[1]}${clean[2]}${clean[2]}"
        6 -> "FF$clean"
        8 -> clean
        else -> "FF000000" // fallback to opaque black
    }
}

// ─── Enum Mapping ────────────────────────────────────────────────────────────

private fun String?.toVerticalArrangement(): UIVerticalArrangement = when (this?.lowercase()) {
    "top" -> UIVerticalArrangement.Top
    "bottom" -> UIVerticalArrangement.Bottom
    "center" -> UIVerticalArrangement.Center
    "spacebetween" -> UIVerticalArrangement.SpaceBetween
    "spacearound" -> UIVerticalArrangement.SpaceAround
    "spaceevenly" -> UIVerticalArrangement.SpaceEvenly
    else -> UIVerticalArrangement.Top
}

private fun String?.toHorizontalArrangement(): UIHorizontalArrangement = when (this?.lowercase()) {
    "start" -> UIHorizontalArrangement.Start
    "end" -> UIHorizontalArrangement.End
    "center" -> UIHorizontalArrangement.Center
    "spacebetween" -> UIHorizontalArrangement.SpaceBetween
    "spacearound" -> UIHorizontalArrangement.SpaceAround
    "spaceevenly" -> UIHorizontalArrangement.SpaceEvenly
    else -> UIHorizontalArrangement.Start
}

private fun AlignmentModifier?.toHorizontalAlignment(): UIHorizontalAlignment =
    when (this?.horizontal?.lowercase()) {
        "center" -> UIHorizontalAlignment.Center
        "end" -> UIHorizontalAlignment.End
        else -> UIHorizontalAlignment.Start
    }

private fun AlignmentModifier?.toVerticalAlignment(): UIVerticalAlignment =
    when (this?.vertical?.lowercase()) {
        "center" -> UIVerticalAlignment.Center
        "bottom" -> UIVerticalAlignment.Bottom
        else -> UIVerticalAlignment.Top
    }

private fun AlignmentModifier?.toBoxAlignment(): UIAlignment {
    val horizontal = this?.horizontal?.lowercase()
    val vertical = this?.vertical?.lowercase()
    return when (vertical) {
        "center" -> when (horizontal) {
            "center" -> UIAlignment.Center
            "end" -> UIAlignment.CenterEnd
            else -> UIAlignment.CenterStart
        }
        "bottom" -> when (horizontal) {
            "center" -> UIAlignment.BottomCenter
            "end" -> UIAlignment.BottomEnd
            else -> UIAlignment.BottomStart
        }
        else -> when (horizontal) {
            "center" -> UIAlignment.TopCenter
            "end" -> UIAlignment.TopEnd
            else -> UIAlignment.TopStart
        }
    }
}

private fun String?.toContentScale(): UIContentScale = when (this?.lowercase()) {
    "fit" -> UIContentScale.Fit
    "crop" -> UIContentScale.Crop
    "inside" -> UIContentScale.Inside
    "fillbounds" -> UIContentScale.FillBounds
    "none" -> UIContentScale.None
    else -> UIContentScale.Fit
}

private fun String?.toButtonStyle(): UIButtonStyle = when (this?.lowercase()) {
    "filled" -> UIButtonStyle.Filled
    "outlined" -> UIButtonStyle.Outlined
    "text" -> UIButtonStyle.Text
    "elevated" -> UIButtonStyle.Elevated
    "tonal" -> UIButtonStyle.Tonal
    else -> UIButtonStyle.Filled
}

private fun String?.toInputType(): UIInputType = when (this?.lowercase()) {
    "email" -> UIInputType.Email
    "number" -> UIInputType.Number
    "phone" -> UIInputType.Phone
    "password" -> UIInputType.Password
    else -> UIInputType.Text
}

private fun TextStyle?.toUITextStyle(): UITextStyle {
    if (this == null) return UITextStyle()
    return UITextStyle(
        fontSize = fontSize,
        fontWeight = fontWeight.toFontWeight(),
        fontStyle = if (fontStyle.lowercase() == "italic") UIFontStyle.Italic else UIFontStyle.Normal,
        color = color?.normalizeHexColor(),
        textAlign = textAlign.toTextAlign(),
        maxLines = maxLines ?: Int.MAX_VALUE,
        lineHeight = lineHeight,
    )
}

private fun String.toFontWeight(): UIFontWeight = when (this.lowercase()) {
    "thin" -> UIFontWeight.Thin
    "light" -> UIFontWeight.Light
    "normal" -> UIFontWeight.Normal
    "medium" -> UIFontWeight.Medium
    "semibold" -> UIFontWeight.SemiBold
    "bold" -> UIFontWeight.Bold
    "extrabold" -> UIFontWeight.ExtraBold
    "black" -> UIFontWeight.Black
    else -> UIFontWeight.Normal
}

private fun String?.toTextAlign(): UITextAlign = when (this?.lowercase()) {
    "center" -> UITextAlign.Center
    "end" -> UITextAlign.End
    "justify" -> UITextAlign.Justify
    else -> UITextAlign.Start
}
