package com.landoulsi.schemaui.compose.nodes

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import com.landoulsi.schemaui.SchemaUIEngine
import com.landoulsi.schemaui.compose.SchemaUINodeRenderer
import com.landoulsi.schemaui.compose.toComposeModifier
import com.landoulsi.schemaui.ir.UIList
import com.landoulsi.schemaui.ir.UIUnknown

/**
 * Renders a [UIList] as a Compose [LazyColumn].
 * Optional dividers are drawn between items using [HorizontalDivider].
 */
@Composable
internal fun ListNode(
    node: UIList,
    engine: SchemaUIEngine,
    customRenderer: (@Composable (UIUnknown) -> Unit)? = null,
) {
    LazyColumn(
        modifier = node.modifiers.toComposeModifier(),
    ) {
        itemsIndexed(
            items = node.items,
        ) { index, item ->
            SchemaUINodeRenderer(node = item, engine = engine, customRenderer = customRenderer)
            if (node.dividers && index < node.items.lastIndex) {
                HorizontalDivider()
            }
        }
    }
}
