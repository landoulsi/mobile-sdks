package com.landoulsi.schemaui

import com.landoulsi.schemaui.ir.SchemaToIRException
import com.landoulsi.schemaui.ir.UINode
import com.landoulsi.schemaui.ir.toIR
import com.landoulsi.schemaui.schema.SchemaNode
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * Parses a JSON string into a [UINode] IR tree.
 *
 * The parser is lenient by default:
 * - Unknown keys are ignored (forward-compatible with schema extensions).
 * - Unknown [SchemaNode] [type] values produce a [UIUnknown] node rather than throwing.
 *
 * The JSON format is polymorphic on the `"type"` field. See [SchemaNode] for supported types.
 *
 * @throws SchemaParseException if the input is not valid JSON, or if serialization fails
 *         for a reason other than an unknown type discriminator.
 */
internal class SchemaUIParser {

    companion object {
        private val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            classDiscriminator = "type"
        }
    }

    /**
     * Parses [jsonString] and returns the root [UINode] of the IR tree.
     *
     * @throws SchemaParseException wrapping any underlying deserialization or IR error.
     */
    fun parse(jsonString: String): UINode {
        val schemaNode = try {
            json.decodeFromString(SchemaNode.serializer(), jsonString)
        } catch (e: SerializationException) {
            throw SchemaParseException("JSON deserialization failed: ${e.message}", e)
        } catch (e: IllegalArgumentException) {
            throw SchemaParseException("Invalid JSON input: ${e.message}", e)
        }
        return try {
            schemaNode.toIR()
        } catch (e: SchemaToIRException) {
            throw SchemaParseException("IR conversion failed: ${e.message}", e)
        }
    }
}

/**
 * Thrown when a schema string cannot be parsed into a valid [UINode] tree.
 */
class SchemaParseException(message: String, cause: Throwable? = null) :
    Exception(message, cause)
