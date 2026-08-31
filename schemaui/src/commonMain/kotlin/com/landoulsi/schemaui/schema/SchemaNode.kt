package com.landoulsi.schemaui.schema

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Raw JSON-parsed schema node. Each node must have a [type] field that drives
 * polymorphic deserialization. Nodes map 1:1 to supported UI elements.
 *
 * Unrecognized type strings gracefully deserialize to [UnknownSchemaNode] with the
 * raw type preserved in [UnknownSchemaNode.originalType].
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable(with = SchemaNodeSerializer::class)
sealed class SchemaNode {
    abstract val modifiers: SchemaModifiers?
}

/**
 * Polymorphic serializer for [SchemaNode] that dispatches on the `"type"` property
 * and defaults to [UnknownSchemaNode] for forward-compatibility with custom/unknown types.
 */
object SchemaNodeSerializer : JsonContentPolymorphicSerializer<SchemaNode>(SchemaNode::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<SchemaNode> {
        val jsonObject = element as? JsonObject
            ?: throw SerializationException("Expected a JSON object for SchemaNode, found: $element")
        val typeElement = jsonObject["type"]
            ?: throw SerializationException("Missing required 'type' discriminator field in schema node: $jsonObject")
        val type = typeElement.jsonPrimitive.contentOrNull
            ?: throw SerializationException("Expected 'type' to be a string primitive in schema node: $jsonObject")

        return when (type) {
            "column" -> ColumnSchemaNode.serializer()
            "row" -> RowSchemaNode.serializer()
            "box" -> BoxSchemaNode.serializer()
            "text" -> TextSchemaNode.serializer()
            "image" -> ImageSchemaNode.serializer()
            "button" -> ButtonSchemaNode.serializer()
            "textField" -> TextFieldSchemaNode.serializer()
            "spacer" -> SpacerSchemaNode.serializer()
            "list" -> ListSchemaNode.serializer()
            "unknown" -> UnknownSchemaNode.serializer()
            else -> FallbackUnknownNodeSerializer(type)
        }
    }
}

private class FallbackUnknownNodeSerializer(private val typeName: String) : KSerializer<UnknownSchemaNode> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("FallbackUnknownNode")

    override fun deserialize(decoder: Decoder): UnknownSchemaNode {
        val jsonDecoder = decoder as? JsonDecoder ?: return UnknownSchemaNode(originalType = typeName)
        val jsonObject = jsonDecoder.decodeJsonElement().jsonObject
        val modifiers = (jsonObject["modifiers"] as? JsonObject)?.let {
            jsonDecoder.json.decodeFromJsonElement(SchemaModifiers.serializer(), it)
        }
        return UnknownSchemaNode(originalType = typeName, modifiers = modifiers)
    }

    override fun serialize(encoder: Encoder, value: UnknownSchemaNode) {
        UnknownSchemaNode.serializer().serialize(encoder, value)
    }
}

// ─── Containers ──────────────────────────────────────────────────────────────

/**
 * Vertical container. Maps to Compose [Column] / SwiftUI [VStack].
 */
@Serializable
@SerialName("column")
data class ColumnSchemaNode(
    val children: List<SchemaNode> = emptyList(),
    /**
     * Arrangement along the main axis.
     * "top" | "bottom" | "center" | "spaceBetween" | "spaceAround" | "spaceEvenly"
     */
    val verticalArrangement: String? = null,
    override val modifiers: SchemaModifiers? = null,
) : SchemaNode()

/**
 * Horizontal container. Maps to Compose [Row] / SwiftUI [HStack].
 */
@Serializable
@SerialName("row")
data class RowSchemaNode(
    val children: List<SchemaNode> = emptyList(),
    /**
     * Arrangement along the main axis.
     * "start" | "end" | "center" | "spaceBetween" | "spaceAround" | "spaceEvenly"
     */
    val horizontalArrangement: String? = null,
    override val modifiers: SchemaModifiers? = null,
) : SchemaNode()

/**
 * Overlay container (Z-stack). Maps to Compose [Box] / SwiftUI [ZStack].
 */
@Serializable
@SerialName("box")
data class BoxSchemaNode(
    val children: List<SchemaNode> = emptyList(),
    override val modifiers: SchemaModifiers? = null,
) : SchemaNode()

// ─── Leaf Nodes ──────────────────────────────────────────────────────────────

/**
 * Text display node.
 */
@Serializable
@SerialName("text")
data class TextSchemaNode(
    val text: String = "",
    val style: TextStyle = TextStyle(),
    override val modifiers: SchemaModifiers? = null,
) : SchemaNode()

/**
 * Image node. Supports remote URLs, bundled resources, or data URIs.
 */
@Serializable
@SerialName("image")
data class ImageSchemaNode(
    /** Remote URL or local file path. */
    val url: String? = null,
    /** Resource identifier for bundled drawables / assets. */
    val resource: String? = null,
    val contentDescription: String? = null,
    /** "fit" | "crop" | "inside" | "fillBounds" | "none" */
    val contentScale: String = "fit",
    override val modifiers: SchemaModifiers? = null,
) : SchemaNode()

/**
 * Interactive button node.
 */
@Serializable
@SerialName("button")
data class ButtonSchemaNode(
    val label: String = "",
    /** Action identifier string dispatched to [com.landoulsi.schemaui.SchemaUIEngine]. */
    val action: String = "",
    /** "filled" | "outlined" | "text" | "elevated" | "tonal" */
    val style: String = "filled",
    val icon: String? = null,
    override val modifiers: SchemaModifiers? = null,
) : SchemaNode()

/**
 * Text input field. Reads and writes its value to [com.landoulsi.schemaui.state.StateStore]
 * under [stateKey].
 */
@Serializable
@SerialName("textField")
data class TextFieldSchemaNode(
    val label: String = "",
    val placeholder: String = "",
    /** Key in [com.landoulsi.schemaui.state.StateStore] bound to this input. */
    val stateKey: String = "",
    /** Action fired on IME Done / Return key. Null = no action. */
    val action: String? = null,
    /** "text" | "email" | "number" | "phone" | "password" */
    val inputType: String = "text",
    override val modifiers: SchemaModifiers? = null,
) : SchemaNode()

/**
 * Fixed or flexible blank space between nodes.
 */
@Serializable
@SerialName("spacer")
data class SpacerSchemaNode(
    /** Fixed width in dp. Null = not constrained horizontally. */
    val width: Float? = null,
    /** Fixed height in dp. Null = not constrained vertically. */
    val height: Float? = null,
    override val modifiers: SchemaModifiers? = null,
) : SchemaNode()

/**
 * Scrollable vertical list of child nodes.
 */
@Serializable
@SerialName("list")
data class ListSchemaNode(
    val items: List<SchemaNode> = emptyList(),
    /** Whether to add dividers between items. */
    val dividers: Boolean = false,
    override val modifiers: SchemaModifiers? = null,
) : SchemaNode()

/**
 * Catch-all node for unrecognized [type] values.
 * The IR transformer produces a [UIUnknown] from this, preserving the raw type string
 * so host apps can decide how to handle custom components.
 */
@Serializable
@SerialName("unknown")
data class UnknownSchemaNode(
    val originalType: String = "unknown",
    override val modifiers: SchemaModifiers? = null,
) : SchemaNode()
