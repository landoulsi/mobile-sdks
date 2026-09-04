package com.landoulsi.schemaui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SchemaUIEngineTest {

    @Test
    fun registerAction_and_triggerAction_execute_callback() {
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
    fun registerActionWithState_receives_latest_state_snapshot() {
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
    fun unregisterAction_removes_both_simple_and_stateful_actions() {
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
    fun triggerAction_on_unregistered_name_degrades_gracefully() {
        val engine = SchemaUIEngine()
        assertFalse(engine.hasAction("nonExistent"))
        // Should not throw
        engine.triggerAction("nonExistent")
    }

    @Test
    fun parseFromString_valid_and_invalid_schemas() {
        val engine = SchemaUIEngine()
        val successResult = engine.parseFromString("""{"type":"text","text":"hello"}""")
        assertTrue(successResult.isSuccess)

        val failResult = engine.parseFromString("invalid json")
        assertTrue(failResult.isFailure)
    }
}
