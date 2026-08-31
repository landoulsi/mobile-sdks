package com.landoulsi.schemaui.compose.nodes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage
import com.landoulsi.schemaui.compose.toComposeContentScale
import com.landoulsi.schemaui.compose.toComposeModifier
import com.landoulsi.schemaui.ir.UIImage

/**
 * Renders a [UIImage] node using Coil 3's [AsyncImage].
 * Supports remote URLs and named bundled resources.
 * Falls back to a transparent placeholder while loading.
 */
@Composable
internal fun ImageNode(node: UIImage) {
    val context = LocalContext.current
    val model: Any? = when {
        node.url != null -> node.url
        node.resource != null -> {
            val resourceName = node.resource
            val resourceId = remember(resourceName) {
                context.resources.getIdentifier(resourceName, "drawable", context.packageName)
            }
            if (resourceId != 0) resourceId else resourceName
        }
        else -> null
    }

    AsyncImage(
        model = model,
        contentDescription = node.contentDescription,
        modifier = node.modifiers.toComposeModifier(),
        contentScale = node.contentScale.toComposeContentScale(),
    )
}
