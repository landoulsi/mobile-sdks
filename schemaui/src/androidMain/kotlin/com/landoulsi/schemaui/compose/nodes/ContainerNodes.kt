package com.landoulsi.schemaui.compose.nodes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import com.landoulsi.schemaui.SchemaUIEngine
import com.landoulsi.schemaui.compose.SchemaUINodeRenderer
import com.landoulsi.schemaui.compose.toComposeAlignment
import com.landoulsi.schemaui.compose.toComposeHorizontalAlignment
import com.landoulsi.schemaui.compose.toComposeModifier
import com.landoulsi.schemaui.compose.toComposeVerticalAlignment
import com.landoulsi.schemaui.ir.UIBox
import com.landoulsi.schemaui.ir.UIColumn
import com.landoulsi.schemaui.ir.UIHorizontalArrangement
import com.landoulsi.schemaui.ir.UIRow
import com.landoulsi.schemaui.ir.UIUnknown
import com.landoulsi.schemaui.ir.UIVerticalArrangement

@Composable
internal fun ColumnNode(
    node: UIColumn,
    engine: SchemaUIEngine,
    customRenderer: (@Composable (UIUnknown) -> Unit)? = null,
) {
    Column(
        modifier = node.modifiers.toComposeModifier(),
        verticalArrangement = node.verticalArrangement.toComposeVerticalArrangement(),
        horizontalAlignment = node.horizontalAlignment.toComposeHorizontalAlignment(),
    ) {
        node.children.forEach { child ->
            SchemaUINodeRenderer(node = child, engine = engine, customRenderer = customRenderer)
        }
    }
}

@Composable
internal fun RowNode(
    node: UIRow,
    engine: SchemaUIEngine,
    customRenderer: (@Composable (UIUnknown) -> Unit)? = null,
) {
    Row(
        modifier = node.modifiers.toComposeModifier(),
        horizontalArrangement = node.horizontalArrangement.toComposeHorizontalArrangement(),
        verticalAlignment = node.verticalAlignment.toComposeVerticalAlignment(),
    ) {
        node.children.forEach { child ->
            SchemaUINodeRenderer(node = child, engine = engine, customRenderer = customRenderer)
        }
    }
}

@Composable
internal fun BoxNode(
    node: UIBox,
    engine: SchemaUIEngine,
    customRenderer: (@Composable (UIUnknown) -> Unit)? = null,
) {
    Box(
        modifier = node.modifiers.toComposeModifier(),
        contentAlignment = node.contentAlignment.toComposeAlignment(),
    ) {
        node.children.forEach { child ->
            SchemaUINodeRenderer(node = child, engine = engine, customRenderer = customRenderer)
        }
    }
}

// ─── Arrangement Converters ───────────────────────────────────────────────────

private fun UIVerticalArrangement.toComposeVerticalArrangement(): Arrangement.Vertical =
    when (this) {
        UIVerticalArrangement.Top -> Arrangement.Top
        UIVerticalArrangement.Bottom -> Arrangement.Bottom
        UIVerticalArrangement.Center -> Arrangement.Center
        UIVerticalArrangement.SpaceBetween -> Arrangement.SpaceBetween
        UIVerticalArrangement.SpaceAround -> Arrangement.SpaceAround
        UIVerticalArrangement.SpaceEvenly -> Arrangement.SpaceEvenly
    }

private fun UIHorizontalArrangement.toComposeHorizontalArrangement(): Arrangement.Horizontal =
    when (this) {
        UIHorizontalArrangement.Start -> Arrangement.Start
        UIHorizontalArrangement.End -> Arrangement.End
        UIHorizontalArrangement.Center -> Arrangement.Center
        UIHorizontalArrangement.SpaceBetween -> Arrangement.SpaceBetween
        UIHorizontalArrangement.SpaceAround -> Arrangement.SpaceAround
        UIHorizontalArrangement.SpaceEvenly -> Arrangement.SpaceEvenly
    }
