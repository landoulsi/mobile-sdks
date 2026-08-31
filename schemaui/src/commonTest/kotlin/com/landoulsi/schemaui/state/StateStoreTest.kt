package com.landoulsi.schemaui.state

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class StateStoreTest {

    @Test
    fun `set and get a value`() {
        val store = StateStore()
        store.set("name", "Alice")
        assertEquals("Alice", store.get("name"))
    }

    @Test
    fun `get returns null for missing key`() {
        val store = StateStore()
        assertNull(store.get("missing"))
    }

    @Test
    fun `set overwrites previous value`() {
        val store = StateStore()
        store.set("count", "1")
        store.set("count", "2")
        assertEquals("2", store.get("count"))
    }

    @Test
    fun `multiple keys are isolated`() {
        val store = StateStore()
        store.set("a", "alpha")
        store.set("b", "beta")
        assertEquals("alpha", store.get("a"))
        assertEquals("beta", store.get("b"))
    }

    @Test
    fun `remove deletes a key`() {
        val store = StateStore()
        store.set("x", "value")
        store.remove("x")
        assertNull(store.get("x"))
    }

    @Test
    fun `clear removes all keys`() {
        val store = StateStore()
        store.set("k1", "v1")
        store.set("k2", "v2")
        store.clear()
        assertNull(store.get("k1"))
        assertNull(store.get("k2"))
    }

    @Test
    fun `snapshot returns full state map`() {
        val store = StateStore()
        store.set("email", "test@example.com")
        store.set("name", "Alice")
        val snap = store.snapshot()
        assertEquals("test@example.com", snap["email"])
        assertEquals("Alice", snap["name"])
        assertEquals(2, snap.size)
    }

    @Test
    fun `observe emits current value on collection`() = runTest {
        val store = StateStore()
        store.set("key", "initial")
        val observed = store.observe("key").first()
        assertEquals("initial", observed)
    }

    @Test
    fun `observe emits null for missing key`() = runTest {
        val store = StateStore()
        val observed = store.observe("nonexistent").first()
        assertNull(observed)
    }

    @Test
    fun `observe emits only distinct values for observed key`() = runTest {
        val store = StateStore()
        val collectedValues = mutableListOf<String?>()

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            store.observe("targetKey").toList(collectedValues)
        }

        store.set("targetKey", "v1")
        store.set("unrelatedKey", "shouldNotTriggerNewValue")
        store.set("targetKey", "v2")

        assertEquals(listOf(null, "v1", "v2"), collectedValues)
    }

    @Test
    fun `state flow emits on set`() = runTest {
        val store = StateStore()
        store.set("x", "hello")
        val current = store.state.first()
        assertEquals("hello", current["x"])
    }

    @Test
    fun `state flow starts empty`() = runTest {
        val store = StateStore()
        assertTrue(store.state.first().isEmpty())
    }

    @Test
    fun `observe emits null when key is removed`() = runTest {
        val store = StateStore()
        val emissions = mutableListOf<String?>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            store.observe("key").toList(emissions)
        }
        store.set("key", "value")
        store.remove("key")
        assertEquals(listOf(null, "value", null), emissions)
    }
}
