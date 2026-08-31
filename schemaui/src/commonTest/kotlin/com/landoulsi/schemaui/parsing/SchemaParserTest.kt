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
    fun `parse minimal column node`() {
        val json = """{"type":"column","children":[]}"""
        val result = engine.parseFromString(json)
        assertTrue(result.isSuccess)
        assertIs<UIColumn>(result.getOrThrow())
    }

    @Test
    fun `parse text node with style`() {
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
    fun `parse button node with action`() {
        val json = """{"type":"button","label":"Submit","action":"submit","style":"outlined"}"""
        val node = engine.parseFromString(json).getOrThrow() as UIButton
        assertEquals("Submit", node.label)
        assertEquals("submit", node.action)
        assertEquals(UIButtonStyle.Outlined, node.style)
    }

    @Test
    fun `parse textField node with stateKey`() {
        val json = """{"type":"textField","label":"Email","placeholder":"you@example.com","stateKey":"email"}"""
        val node = engine.parseFromString(json).getOrThrow() as UITextField
        assertEquals("email", node.stateKey)
        assertEquals("Email", node.label)
        assertNull(node.action)
    }

    @Test
    fun `parse image node with url`() {
        val json = """{"type":"image","url":"https://example.com/img.png","contentDescription":"Hero image"}"""
        val node = engine.parseFromString(json).getOrThrow() as UIImage
        assertEquals("https://example.com/img.png", node.url)
        assertEquals("Hero image", node.contentDescription)
        assertNull(node.resource)
    }

    @Test
    fun `parse spacer node with height`() {
        val json = """{"type":"spacer","height":16.0}"""
        val node = engine.parseFromString(json).getOrThrow() as UISpacer
        assertEquals(16f, node.height)
        assertNull(node.width)
    }

    @Test
    fun `parse row node`() {
        val json = """{"type":"row","children":[{"type":"text","text":"A"},{"type":"text","text":"B"}]}"""
        val node = engine.parseFromString(json).getOrThrow() as UIRow
        assertEquals(2, node.children.size)
        assertIs<UIText>(node.children[0])
    }

    @Test
    fun `parse box node`() {
        val json = """{"type":"box","children":[]}"""
        assertIs<UIBox>(engine.parseFromString(json).getOrThrow())
    }

    @Test
    fun `parse list node with items`() {
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
    fun `parse nested column with children`() {
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
    fun `parse node with padding modifier`() {
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
    fun `specific padding overrides shorthand`() {
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
    fun `parse fillMaxWidth modifier`() {
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
    fun `parse background modifier with hex color`() {
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
    fun `parse explicit unknown type produces UIUnknown gracefully`() {
        val json = """{"type":"unknown","originalType":"custom_widget"}"""
        val node = engine.parseFromString(json).getOrThrow()
        assertIs<UIUnknown>(node)
        assertEquals("custom_widget", (node as UIUnknown).originalType)
    }

    @Test
    fun `parse arbitrary unrecognized type produces UIUnknown with originalType`() {
        val json = """{"type":"video_player","url":"https://example.com/video.mp4"}"""
        val result = engine.parseFromString(json)
        assertTrue(result.isSuccess)
        val node = result.getOrThrow()
        assertIs<UIUnknown>(node)
        assertEquals("video_player", (node as UIUnknown).originalType)
    }

    @Test
    fun `parse column with unrecognized child type produces UIUnknown child`() {
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
    fun `FallbackUnknownNodeSerializer handles null modifiers gracefully`() {
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
    fun `parse empty string returns failure`() {
        val result = engine.parseFromString("")
        assertTrue(result.isFailure)
        assertIs<SchemaParseException>(result.exceptionOrNull())
    }

    @Test
    fun `parse invalid json returns failure`() {
        val result = engine.parseFromString("this is not json")
        assertTrue(result.isFailure)
        assertIs<SchemaParseException>(result.exceptionOrNull())
    }

    @Test
    fun `parse json without type field returns failure`() {
        val result = engine.parseFromString("""{"label":"No type here"}""")
        assertTrue(result.isFailure)
    }

    @Test
    fun `extra unknown fields are ignored`() {
        val json = """{"type":"text","text":"Hi","unknownField":"ignored","anotherOne":42}"""
        val result = engine.parseFromString(json)
        assertTrue(result.isSuccess)
        val node = result.getOrThrow() as UIText
        assertEquals("Hi", node.text)
    }
}
