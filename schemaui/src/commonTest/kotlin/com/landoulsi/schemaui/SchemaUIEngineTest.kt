package com.landoulsi.schemaui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SchemaUIEngineTest {

    @Test
    fun `registerAction and triggerAction execute callback`() {
        val engine = SchemaUIEngine()
        var triggered = false
        engine.registerAction("click") {
            triggered = true
        }

        assertTrue(engine.hasAction("click"))
        engine.triggerAction("click")
        assertTrue(triggered)
    }

    @Test
    fun `registerActionWithState receives latest state snapshot`() {
        val engine = SchemaUIEngine()
        engine.stateStore.set("username", "testUser")
        var capturedUsername: String? = null

        engine.registerActionWithState("submit") { state ->
            capturedUsername = state["username"]
        }

        assertTrue(engine.hasAction("submit"))
        engine.triggerAction("submit")
        assertEquals("testUser", capturedUsername)
    }

    @Test
    fun `unregisterAction removes both simple and stateful actions`() {
        val engine = SchemaUIEngine()
        var simpleTriggered = false
        var stateTriggered = false

        engine.registerAction("actionA") { simpleTriggered = true }
        engine.registerActionWithState("actionA") { stateTriggered = true }

        assertTrue(engine.hasAction("actionA"))

        engine.unregisterAction("actionA")
        assertFalse(engine.hasAction("actionA"))

        engine.triggerAction("actionA")
        assertFalse(simpleTriggered)
        assertFalse(stateTriggered)
    }

    @Test
    fun `triggerAction on unregistered name degrades gracefully`() {
        val engine = SchemaUIEngine()
        assertFalse(engine.hasAction("nonExistent"))
        // Should not throw
        engine.triggerAction("nonExistent")
    }

    @Test
    fun `parseFromString valid and invalid schemas`() {
        val engine = SchemaUIEngine()
        val successResult = engine.parseFromString("""{"type":"text","text":"hello"}""")
        assertTrue(successResult.isSuccess)

        val failResult = engine.parseFromString("invalid json")
        assertTrue(failResult.isFailure)
    }
}
