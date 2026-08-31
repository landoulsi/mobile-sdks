package com.landoulsi.schemaui

import com.landoulsi.schemaui.ir.UIButton
import com.landoulsi.schemaui.ir.UIText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SchemaUIKitTest {

    @Test
    fun parseValidSchemaReturnsNode() {
        val kit = SchemaUIKit()
        val json = """{"type":"text","text":"Hello iOS"}"""
        val node = kit.parseSchema(json)
        assertNotNull(node)
        assertTrue(node is UIText)
        assertEquals("Hello iOS", node.text)
        assertNull(kit.lastError)
    }

    @Test
    fun parseInvalidSchemaReturnsNullAndSetsLastError() {
        val kit = SchemaUIKit()
        val node = kit.parseSchema("invalid json")
        assertNull(node)
        assertNotNull(kit.lastError)
    }

    @Test
    fun actionsAreTriggeredThroughKit() {
        val kit = SchemaUIKit()
        var triggered = false
        kit.registerAction("onTap") {
            triggered = true
        }
        kit.triggerAction("onTap")
        assertTrue(triggered)
    }

    @Test
    fun stateStoreIsAccessibleAndCanBeReset() {
        val kit = SchemaUIKit()
        kit.stateStore.set("key", "value")
        assertEquals("value", kit.stateStore.get("key"))
        kit.reset()
        assertNull(kit.stateStore.get("key"))
        assertNull(kit.lastError)
    }
}
