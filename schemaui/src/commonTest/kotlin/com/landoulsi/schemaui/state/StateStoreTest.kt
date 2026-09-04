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
    fun set_and_get_a_value() {
        val store = StateStore()
        store.set("name", "Alice")
        assertEquals("Alice", store.get("name"))
    }

    @Test
    fun get_returns_null_for_missing_key() {
        val store = StateStore()
        assertNull(store.get("missing"))
    }

    @Test
    fun set_overwrites_previous_value() {
        val store = StateStore()
        store.set("count", "1")
        store.set("count", "2")
        assertEquals("2", store.get("count"))
    }

    @Test
    fun multiple_keys_are_isolated() {
        val store = StateStore()
        store.set("a", "alpha")
        store.set("b", "beta")
        assertEquals("alpha", store.get("a"))
        assertEquals("beta", store.get("b"))
    }

    @Test
    fun remove_deletes_a_key() {
        val store = StateStore()
        store.set("x", "value")
        store.remove("x")
        assertNull(store.get("x"))
    }

    @Test
    fun clear_removes_all_keys() {
        val store = StateStore()
        store.set("k1", "v1")
        store.set("k2", "v2")
        store.clear()
        assertNull(store.get("k1"))
        assertNull(store.get("k2"))
    }

    @Test
    fun snapshot_returns_full_state_map() {
        val store = StateStore()
        store.set("email", "test@example.com")
        store.set("name", "Alice")
        val snap = store.snapshot()
        assertEquals("test@example.com", snap["email"])
        assertEquals("Alice", snap["name"])
        assertEquals(2, snap.size)
    }

    @Test
    fun observe_emits_current_value_on_collection() = runTest {
        val store = StateStore()
        store.set("key", "initial")
        val observed = store.observe("key").first()
        assertEquals("initial", observed)
    }

    @Test
    fun observe_emits_null_for_missing_key() = runTest {
        val store = StateStore()
        val observed = store.observe("nonexistent").first()
        assertNull(observed)
    }

    @Test
    fun observe_emits_only_distinct_values_for_observed_key() = runTest {
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
    fun state_flow_emits_on_set() = runTest {
        val store = StateStore()
        store.set("x", "hello")
        val current = store.state.first()
        assertEquals("hello", current["x"])
    }

    @Test
    fun state_flow_starts_empty() = runTest {
        val store = StateStore()
        assertTrue(store.state.first().isEmpty())
    }

    @Test
    fun observe_emits_null_when_key_is_removed() = runTest {
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
