package com.landoulsi.schemaui.compose

import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import com.landoulsi.schemaui.SchemaUIEngine
import com.landoulsi.schemaui.compose.nodes.BoxNode
import com.landoulsi.schemaui.compose.nodes.ButtonNode
import com.landoulsi.schemaui.compose.nodes.ColumnNode
import com.landoulsi.schemaui.compose.nodes.ImageNode
import com.landoulsi.schemaui.compose.nodes.ListNode
import com.landoulsi.schemaui.compose.nodes.RowNode
import com.landoulsi.schemaui.compose.nodes.SpacerNode
import com.landoulsi.schemaui.compose.nodes.TextFieldNode
import com.landoulsi.schemaui.compose.nodes.TextNode
import com.landoulsi.schemaui.ir.UIBox
import com.landoulsi.schemaui.ir.UIButton
import com.landoulsi.schemaui.ir.UIColumn
import com.landoulsi.schemaui.ir.UIImage
import com.landoulsi.schemaui.ir.UIList
import com.landoulsi.schemaui.ir.UINode
import com.landoulsi.schemaui.ir.UIRow
import com.landoulsi.schemaui.ir.UISpacer
import com.landoulsi.schemaui.ir.UIText
import com.landoulsi.schemaui.ir.UITextField
import com.landoulsi.schemaui.ir.UIUnknown

/**
 * Top-level public Composable for rendering a SchemaUI [UINode] tree.
 *
 * ## Usage
 * ```kotlin
 * val engine = SchemaUIEngine()
 * engine.registerAction("submit") { /* ... */ }
 *
 * val result = engine.parseFromString(jsonString)
 * result.onSuccess { rootNode ->
 *     SchemaUI(node = rootNode, engine = engine)
 * }
 * ```
 *
 * @param node The root [UINode] from [SchemaUIEngine.parseFromString].
 * @param engine The [SchemaUIEngine] instance that owns the [StateStore] and action registry.
 * @param customRenderer Optional custom Composable slot for rendering [UIUnknown] node types.
 */
@Composable
fun SchemaUI(
    node: UINode,
    engine: SchemaUIEngine,
    modifier: Modifier = Modifier,
    customRenderer: (@Composable (UIUnknown) -> Unit)? = null,
) {
    Box(modifier = modifier) {
        SchemaUINodeRenderer(node = node, engine = engine, customRenderer = customRenderer)
    }
}

/**
 * Internal recursive dispatcher. Routes each [UINode] subtype to its dedicated Composable.
 * This function is the single entry point for all recursive child rendering.
 */
@Composable
internal fun SchemaUINodeRenderer(
    node: UINode,
    engine: SchemaUIEngine,
    customRenderer: (@Composable (UIUnknown) -> Unit)? = null,
) {
    when (node) {
        // Containers
        is UIColumn -> ColumnNode(node, engine, customRenderer)
        is UIRow -> RowNode(node, engine, customRenderer)
        is UIBox -> BoxNode(node, engine, customRenderer)
        // Leaves
        is UIText -> TextNode(node)
        is UIImage -> ImageNode(node)
        is UIButton -> ButtonNode(node, engine)
        is UITextField -> TextFieldNode(node, engine)
        is UISpacer -> SpacerNode(node)
        is UIList -> ListNode(node, engine, customRenderer)
        // Unknown — delegate to customRenderer if provided, else fallback to debug placeholder
        is UIUnknown -> {
            if (customRenderer != null) {
                customRenderer(node)
            } else {
                UnknownNodePlaceholder(node)
            }
        }
    }
}

/**
 * Debug placeholder rendered for unrecognized schema node types.
 * Visible in debug/preview builds to help schema authors catch typos.
 */
@Composable
private fun UnknownNodePlaceholder(node: UIUnknown) {
    Text(
        text = "⚠ Unknown node: \"${node.originalType}\"",
        color = Color(0xFFFF5722),
        textAlign = TextAlign.Center,
    )
}
