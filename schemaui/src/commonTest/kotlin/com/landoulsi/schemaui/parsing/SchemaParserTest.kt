package com.landoulsi.schemaui.parsing

import com.landoulsi.schemaui.SchemaParseException
import com.landoulsi.schemaui.SchemaUIEngine
import com.landoulsi.schemaui.ir.UIBox
import com.landoulsi.schemaui.ir.UIButton
import com.landoulsi.schemaui.ir.UIButtonStyle
import com.landoulsi.schemaui.ir.UIColumn
import com.landoulsi.schemaui.ir.UIFontWeight
import com.landoulsi.schemaui.ir.UIImage
import com.landoulsi.schemaui.ir.UIList
import com.landoulsi.schemaui.ir.UIModifiers
import com.landoulsi.schemaui.ir.UIRow
import com.landoulsi.schemaui.ir.UISpacer
import com.landoulsi.schemaui.ir.UIText
import com.landoulsi.schemaui.ir.UITextField
import com.landoulsi.schemaui.ir.UIUnknown
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SchemaParserTest {

    private val engine = SchemaUIEngine()

    // ─── Positive Cases ───────────────────────────────────────────────────────

    @Test
    fun parse_minimal_column_node() {
        val json = """{"type":"column","children":[]}"""
        val result = engine.parseFromString(json)
        assertTrue(result.isSuccess)
        assertIs<UIColumn>(result.getOrThrow())
    }

    @Test
    fun parse_text_node_with_style() {
        val json = """
            {
              "type": "text",
              "text": "Hello SchemaUI",
              "style": {
                "fontSize": 24.0,
                "fontWeight": "bold",
                "color": "#FF0000"
              }
            }
        """.trimIndent()
        val result = engine.parseFromString(json)
        val node = result.getOrThrow() as UIText
        assertEquals("Hello SchemaUI", node.text)
        assertEquals(24f, node.style.fontSize)
        assertEquals(UIFontWeight.Bold, node.style.fontWeight)
        assertNotNull(node.style.color)
    }

    @Test
    fun parse_button_node_with_action() {
        val json = """{"type":"button","label":"Submit","action":"submit","style":"outlined"}"""
        val node = engine.parseFromString(json).getOrThrow() as UIButton
        assertEquals("Submit", node.label)
        assertEquals("submit", node.action)
        assertEquals(UIButtonStyle.Outlined, node.style)
    }

    @Test
    fun parse_textField_node_with_stateKey() {
        val json = """{"type":"textField","label":"Email","placeholder":"you@example.com","stateKey":"email"}"""
        val node = engine.parseFromString(json).getOrThrow() as UITextField
        assertEquals("email", node.stateKey)
        assertEquals("Email", node.label)
        assertNull(node.action)
    }

    @Test
    fun parse_image_node_with_url() {
        val json = """{"type":"image","url":"https://example.com/img.png","contentDescription":"Hero image"}"""
        val node = engine.parseFromString(json).getOrThrow() as UIImage
        assertEquals("https://example.com/img.png", node.url)
        assertEquals("Hero image", node.contentDescription)
        assertNull(node.resource)
    }

    @Test
    fun parse_spacer_node_with_height() {
        val json = """{"type":"spacer","height":16.0}"""
        val node = engine.parseFromString(json).getOrThrow() as UISpacer
        assertEquals(16f, node.height)
        assertNull(node.width)
    }

    @Test
    fun parse_row_node() {
        val json = """{"type":"row","children":[{"type":"text","text":"A"},{"type":"text","text":"B"}]}"""
        val node = engine.parseFromString(json).getOrThrow() as UIRow
        assertEquals(2, node.children.size)
        assertIs<UIText>(node.children[0])
    }

    @Test
    fun parse_box_node() {
        val json = """{"type":"box","children":[]}"""
        assertIs<UIBox>(engine.parseFromString(json).getOrThrow())
    }

    @Test
    fun parse_list_node_with_items() {
        val json = """
            {
              "type": "list",
              "dividers": true,
              "items": [
                {"type":"text","text":"Item 1"},
                {"type":"text","text":"Item 2"}
              ]
            }
        """.trimIndent()
        val node = engine.parseFromString(json).getOrThrow() as UIList
        assertEquals(2, node.items.size)
        assertTrue(node.dividers)
    }

    @Test
    fun parse_nested_column_with_children() {
        val json = """
            {
              "type": "column",
              "children": [
                {"type":"text","text":"Title"},
                {"type":"spacer","height":8.0},
                {"type":"button","label":"OK","action":"ok"}
              ]
            }
        """.trimIndent()
        val columnNode = engine.parseFromString(json).getOrThrow() as UIColumn
        assertEquals(3, columnNode.children.size)
        assertIs<UIText>(columnNode.children[0])
        assertIs<UISpacer>(columnNode.children[1])
        assertIs<UIButton>(columnNode.children[2])
    }

    @Test
    fun parse_node_with_padding_modifier() {
        val json = """
            {
              "type": "text",
              "text": "Padded",
              "modifiers": {
                "padding": {"all": 16.0}
              }
            }
        """.trimIndent()
        val node = engine.parseFromString(json).getOrThrow() as UIText
        assertEquals(16f, node.modifiers.paddingStart)
        assertEquals(16f, node.modifiers.paddingEnd)
        assertEquals(16f, node.modifiers.paddingTop)
        assertEquals(16f, node.modifiers.paddingBottom)
    }

    @Test
    fun specific_padding_overrides_shorthand() {
        val json = """
            {
              "type": "text",
              "text": "Test",
              "modifiers": {
                "padding": {"all": 8.0, "top": 24.0}
              }
            }
        """.trimIndent()
        val node = engine.parseFromString(json).getOrThrow() as UIText
        assertEquals(24f, node.modifiers.paddingTop)
        assertEquals(8f, node.modifiers.paddingBottom)
        assertEquals(8f, node.modifiers.paddingStart)
    }

    @Test
    fun parse_fillMaxWidth_modifier() {
        val json = """
            {
              "type": "column",
              "children": [],
              "modifiers": {"size": {"fillMaxWidth": true}}
            }
        """.trimIndent()
        val node = engine.parseFromString(json).getOrThrow() as UIColumn
        assertTrue(node.modifiers.fillMaxWidth)
    }

    @Test
    fun parse_background_modifier_with_hex_color() {
        val json = """
            {
              "type": "text",
              "text": "Colored",
              "modifiers": {"background": {"color": "#FF5733", "cornerRadius": 8.0}}
            }
        """.trimIndent()
        val node = engine.parseFromString(json).getOrThrow() as UIText
        assertEquals("FFFF5733", node.modifiers.backgroundColor)
        assertEquals(8f, node.modifiers.cornerRadius)
    }

    // ─── Unknown Type ─────────────────────────────────────────────────────────

    @Test
    fun parse_explicit_unknown_type_produces_UIUnknown_gracefully() {
        val json = """{"type":"unknown","originalType":"custom_widget"}"""
        val node = engine.parseFromString(json).getOrThrow()
        assertIs<UIUnknown>(node)
        assertEquals("custom_widget", (node as UIUnknown).originalType)
    }

    @Test
    fun parse_arbitrary_unrecognized_type_produces_UIUnknown_with_originalType() {
        val json = """{"type":"video_player","url":"https://example.com/video.mp4"}"""
        val result = engine.parseFromString(json)
        assertTrue(result.isSuccess)
        val node = result.getOrThrow()
        assertIs<UIUnknown>(node)
        assertEquals("video_player", (node as UIUnknown).originalType)
    }

    @Test
    fun parse_column_with_unrecognized_child_type_produces_UIUnknown_child() {
        val json = """
            {
              "type": "column",
              "children": [
                {"type": "text", "text": "Title"},
                {"type": "chart_view", "data": [1, 2, 3]}
              ]
            }
        """.trimIndent()
        val column = engine.parseFromString(json).getOrThrow() as UIColumn
        assertEquals(2, column.children.size)
        assertIs<UIText>(column.children[0])
        assertIs<UIUnknown>(column.children[1])
        assertEquals("chart_view", (column.children[1] as UIUnknown).originalType)
    }

    @Test
    fun FallbackUnknownNodeSerializer_handles_null_modifiers_gracefully() {
        val json = """{"type":"custom_widget","modifiers":null}"""
        val result = engine.parseFromString(json)
        assertTrue(result.isSuccess)
        val node = result.getOrThrow()
        assertIs<UIUnknown>(node)
        assertEquals("custom_widget", (node as UIUnknown).originalType)
        assertEquals(UIModifiers.None, node.modifiers)
    }

    // ─── Negative Cases ───────────────────────────────────────────────────────

    @Test
    fun parse_empty_string_returns_failure() {
        val result = engine.parseFromString("")
        assertTrue(result.isFailure)
        assertIs<SchemaParseException>(result.exceptionOrNull())
    }

    @Test
    fun parse_invalid_json_returns_failure() {
        val result = engine.parseFromString("this is not json")
        assertTrue(result.isFailure)
        assertIs<SchemaParseException>(result.exceptionOrNull())
    }

    @Test
    fun parse_json_without_type_field_returns_failure() {
        val result = engine.parseFromString("""{"label":"No type here"}""")
        assertTrue(result.isFailure)
    }

    @Test
    fun extra_unknown_fields_are_ignored() {
        val json = """{"type":"text","text":"Hi","unknownField":"ignored","anotherOne":42}"""
        val result = engine.parseFromString(json)
        assertTrue(result.isSuccess)
        val node = result.getOrThrow() as UIText
        assertEquals("Hi", node.text)
    }
}
