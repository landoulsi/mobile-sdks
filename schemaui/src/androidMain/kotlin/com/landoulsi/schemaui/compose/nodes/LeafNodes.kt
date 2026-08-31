package com.landoulsi.schemaui.compose.nodes

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.landoulsi.schemaui.SchemaUIEngine
import com.landoulsi.schemaui.compose.hexToComposeColor
import com.landoulsi.schemaui.compose.toComposeFontWeight
import com.landoulsi.schemaui.compose.toComposeModifier
import com.landoulsi.schemaui.compose.toComposeTextAlign
import com.landoulsi.schemaui.ir.UIButton
import com.landoulsi.schemaui.ir.UIButtonStyle
import com.landoulsi.schemaui.ir.UIFontStyle
import com.landoulsi.schemaui.ir.UIInputType
import com.landoulsi.schemaui.ir.UISpacer
import com.landoulsi.schemaui.ir.UIText
import com.landoulsi.schemaui.ir.UITextField

// ─── Text ─────────────────────────────────────────────────────────────────────

@Composable
internal fun TextNode(node: UIText) {
    val textColor = node.style.color?.hexToComposeColor() ?: Color.Unspecified
    val resolvedLineHeight = node.style.lineHeight?.let {
        if (it <= 3f) (node.style.fontSize * it).sp else it.sp
    } ?: TextUnit.Unspecified

    Text(
        text = node.text,
        modifier = node.modifiers.toComposeModifier(),
        style = TextStyle(
            fontSize = node.style.fontSize.sp,
            fontWeight = node.style.fontWeight.toComposeFontWeight(),
            fontStyle = if (node.style.fontStyle == UIFontStyle.Italic) FontStyle.Italic else FontStyle.Normal,
            color = textColor,
            textAlign = node.style.textAlign.toComposeTextAlign(),
            lineHeight = resolvedLineHeight,
        ),
        maxLines = node.style.maxLines,
    )
}

// ─── Button ───────────────────────────────────────────────────────────────────

@Composable
internal fun ButtonNode(node: UIButton, engine: SchemaUIEngine) {
    val onClick = { engine.triggerAction(node.action) }
    val modifier = node.modifiers.toComposeModifier()
    val label: @Composable () -> Unit = { Text(node.label) }

    when (node.style) {
        UIButtonStyle.Filled -> Button(onClick = onClick, modifier = modifier) { label() }
        UIButtonStyle.Outlined -> OutlinedButton(onClick = onClick, modifier = modifier) { label() }
        UIButtonStyle.Text -> TextButton(onClick = onClick, modifier = modifier) { label() }
        UIButtonStyle.Elevated -> ElevatedButton(onClick = onClick, modifier = modifier) { label() }
        UIButtonStyle.Tonal -> FilledTonalButton(onClick = onClick, modifier = modifier) { label() }
    }
}

// ─── TextField ────────────────────────────────────────────────────────────────

@Composable
internal fun TextFieldNode(node: UITextField, engine: SchemaUIEngine) {
    val textFlow = remember(node.stateKey, engine) { engine.stateStore.observe(node.stateKey) }
    val value by textFlow.collectAsState(initial = engine.stateStore.get(node.stateKey) ?: "")

    OutlinedTextField(
        value = value ?: "",
        onValueChange = { engine.stateStore.set(node.stateKey, it) },
        modifier = node.modifiers.toComposeModifier(),
        label = if (node.label.isNotEmpty()) { { Text(node.label) } } else null,
        placeholder = if (node.placeholder.isNotEmpty()) { { Text(node.placeholder) } } else null,
        singleLine = node.inputType != UIInputType.Text,
        keyboardOptions = KeyboardOptions(
            keyboardType = node.inputType.toKeyboardType(),
            imeAction = if (node.action != null) ImeAction.Done else ImeAction.Default,
        ),
        keyboardActions = KeyboardActions(
            onDone = { node.action?.let { engine.triggerAction(it) } },
        ),
        visualTransformation = if (node.inputType == UIInputType.Password) {
            PasswordVisualTransformation()
        } else {
            VisualTransformation.None
        },
    )
}

private fun UIInputType.toKeyboardType(): KeyboardType = when (this) {
    UIInputType.Text -> KeyboardType.Text
    UIInputType.Email -> KeyboardType.Email
    UIInputType.Number -> KeyboardType.Number
    UIInputType.Phone -> KeyboardType.Phone
    UIInputType.Password -> KeyboardType.Password
}

// ─── Spacer ───────────────────────────────────────────────────────────────────

/**
 * Renders a [UISpacer] node.
 * Fixed width and/or height dimensions are applied as explicit dp sizes.
 * Flexible spacing should use `"modifiers.size.fillMaxWidth/fillMaxHeight": true` in the schema.
 */
@Composable
internal fun SpacerNode(node: UISpacer) {
    val modifier = node.modifiers.toComposeModifier().then(
        Modifier
            .then(if (node.width != null) Modifier.width(node.width.dp) else Modifier)
            .then(if (node.height != null) Modifier.height(node.height.dp) else Modifier)
    )
    Spacer(modifier = modifier)
}
