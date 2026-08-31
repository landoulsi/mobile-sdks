package com.landoulsi.schemaui.schema

import kotlinx.serialization.Serializable

/**
 * Padding modifier that can be applied to any schema node.
 * Values in density-independent pixels (dp).
 * Specific sides take precedence over shorthand properties.
 */
@Serializable
data class PaddingModifier(
    val all: Float? = null,
    val horizontal: Float? = null,
    val vertical: Float? = null,
    val start: Float? = null,
    val end: Float? = null,
    val top: Float? = null,
    val bottom: Float? = null,
)

/**
 * Size modifier that controls the dimensions of a node.
 * All values in dp; null means "wrap content".
 */
@Serializable
data class SizeModifier(
    val width: Float? = null,
    val height: Float? = null,
    /** If true, the node fills all available horizontal space. */
    val fillMaxWidth: Boolean = false,
    /** If true, the node fills all available vertical space. */
    val fillMaxHeight: Boolean = false,
    val minWidth: Float? = null,
    val minHeight: Float? = null,
)

/**
 * Background appearance modifier.
 * [color] is a hex string e.g. "#FF5733" or "#80FFFFFF" (AARRGGBB supported).
 */
@Serializable
data class BackgroundModifier(
    val color: String? = null,
    val cornerRadius: Float = 0f,
)

/**
 * Alignment modifier for container children.
 * Matches the cross-axis semantics on each platform:
 *  - Column: [horizontal] aligns children cross-axis; [vertical] aligns along main-axis (not typical but provided for Box)
 *  - Row: [vertical] aligns children cross-axis
 *  - Box: both axes apply
 */
@Serializable
data class AlignmentModifier(
    /** "start" | "center" | "end" */
    val horizontal: String? = null,
    /** "top" | "center" | "bottom" */
    val vertical: String? = null,
)

/**
 * Typography style for text nodes.
 */
@Serializable
data class TextStyle(
    /** Font size in sp. */
    val fontSize: Float = 14f,
    /** "thin" | "light" | "normal" | "medium" | "semiBold" | "bold" | "extraBold" | "black" */
    val fontWeight: String = "normal",
    /** Hex color string. */
    val color: String? = null,
    /** "start" | "center" | "end" | "justify" */
    val textAlign: String? = null,
    /** Maximum number of lines. null = unlimited. */
    val maxLines: Int? = null,
    /** Line height multiplier. */
    val lineHeight: Float? = null,
    /** "italic" | "normal" */
    val fontStyle: String = "normal",
)

/**
 * Top-level schema modifiers collected on every node.
 * Each property is optional; null / default means "not applied".
 */
@Serializable
data class SchemaModifiers(
    val padding: PaddingModifier? = null,
    val size: SizeModifier? = null,
    val background: BackgroundModifier? = null,
    val alignment: AlignmentModifier? = null,
    /** Opacity from 0.0 (transparent) to 1.0 (fully opaque). */
    val alpha: Float? = null,
    /** Clip the node to its background shape. */
    val clip: Boolean = false,
)
