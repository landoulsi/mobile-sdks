package com.landoulsi.schemaui.ir

/**
 * Resolved, platform-agnostic intermediate representation of modifiers.
 * All values have been normalized and defaults applied — renderers should not
 * need to null-check individual fields.
 */
data class UIModifiers(
    val paddingStart: Float = 0f,
    val paddingEnd: Float = 0f,
    val paddingTop: Float = 0f,
    val paddingBottom: Float = 0f,
    val width: Float? = null,
    val height: Float? = null,
    val fillMaxWidth: Boolean = false,
    val fillMaxHeight: Boolean = false,
    val minWidth: Float? = null,
    val minHeight: Float? = null,
    /** Normalized ARGB hex string, e.g. "FF1A1A2E". Null = no background. */
    val backgroundColor: String? = null,
    val cornerRadius: Float = 0f,
    val alpha: Float = 1f,
    val clip: Boolean = false,
) {
    companion object {
        val None = UIModifiers()
    }
}

/**
 * Resolved text appearance for text nodes. Renderers consume this directly.
 */
data class UITextStyle(
    val fontSize: Float = 14f,
    val fontWeight: UIFontWeight = UIFontWeight.Normal,
    val fontStyle: UIFontStyle = UIFontStyle.Normal,
    /** Normalized ARGB hex string. Null = inherit from theme. */
    val color: String? = null,
    val textAlign: UITextAlign = UITextAlign.Start,
    val maxLines: Int = Int.MAX_VALUE,
    val lineHeight: Float? = null,
)

enum class UIFontWeight { Thin, Light, Normal, Medium, SemiBold, Bold, ExtraBold, Black }
enum class UIFontStyle { Normal, Italic }
enum class UITextAlign { Start, Center, End, Justify }
enum class UIContentScale { Fit, Crop, Inside, FillBounds, None }
enum class UIButtonStyle { Filled, Outlined, Text, Elevated, Tonal }
enum class UIInputType { Text, Email, Number, Phone, Password }
enum class UIHorizontalAlignment { Start, Center, End }
enum class UIVerticalAlignment { Top, Center, Bottom }
enum class UIAlignment { TopStart, TopCenter, TopEnd, CenterStart, Center, CenterEnd, BottomStart, BottomCenter, BottomEnd }
enum class UIHorizontalArrangement { Start, End, Center, SpaceBetween, SpaceAround, SpaceEvenly }
enum class UIVerticalArrangement { Top, Bottom, Center, SpaceBetween, SpaceAround, SpaceEvenly }

/**
 * Platform-agnostic IR tree. Sealed hierarchy mirrors the schema node set.
 * Renderers pattern-match on this hierarchy to emit platform UI.
 *
 * All optional schema fields have been resolved to concrete values;
 * all modifier math has been applied (e.g., padding shorthand expanded).
 */
sealed class UINode {
    abstract val modifiers: UIModifiers
}

// ─── Containers ──────────────────────────────────────────────────────────────

data class UIColumn(
    val children: List<UINode>,
    val verticalArrangement: UIVerticalArrangement = UIVerticalArrangement.Top,
    val horizontalAlignment: UIHorizontalAlignment = UIHorizontalAlignment.Start,
    override val modifiers: UIModifiers = UIModifiers.None,
) : UINode()

data class UIRow(
    val children: List<UINode>,
    val horizontalArrangement: UIHorizontalArrangement = UIHorizontalArrangement.Start,
    val verticalAlignment: UIVerticalAlignment = UIVerticalAlignment.Top,
    override val modifiers: UIModifiers = UIModifiers.None,
) : UINode()

data class UIBox(
    val children: List<UINode>,
    val contentAlignment: UIAlignment = UIAlignment.TopStart,
    override val modifiers: UIModifiers = UIModifiers.None,
) : UINode()

// ─── Leaf Nodes ──────────────────────────────────────────────────────────────

data class UIText(
    val text: String,
    val style: UITextStyle = UITextStyle(),
    override val modifiers: UIModifiers = UIModifiers.None,
) : UINode()

data class UIImage(
    /** Remote/local URL. */
    val url: String?,
    /** Named bundled resource identifier. */
    val resource: String?,
    val contentDescription: String?,
    val contentScale: UIContentScale = UIContentScale.Fit,
    override val modifiers: UIModifiers = UIModifiers.None,
) : UINode()

data class UIButton(
    val label: String,
    val action: String,
    val style: UIButtonStyle = UIButtonStyle.Filled,
    val icon: String? = null,
    override val modifiers: UIModifiers = UIModifiers.None,
) : UINode()

data class UITextField(
    val label: String,
    val placeholder: String,
    val stateKey: String,
    val action: String?,
    val inputType: UIInputType = UIInputType.Text,
    override val modifiers: UIModifiers = UIModifiers.None,
) : UINode()

data class UISpacer(
    val width: Float?,
    val height: Float?,
    override val modifiers: UIModifiers = UIModifiers.None,
) : UINode()

data class UIList(
    val items: List<UINode>,
    val dividers: Boolean = false,
    override val modifiers: UIModifiers = UIModifiers.None,
) : UINode()

/**
 * Placeholder emitted for unrecognized schema node types.
 * Host apps can inspect [originalType] and register custom renderers.
 */
data class UIUnknown(
    val originalType: String,
    override val modifiers: UIModifiers = UIModifiers.None,
) : UINode()
