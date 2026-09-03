package com.landoulsi.viewmodel

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SavedStateTest {

    private class TestSavedStateRegistryOwner(
        initialState: LifecycleState = LifecycleState.INITIALIZED
    ) : SavedStateRegistryOwner {
        val registry = LifecycleRegistry(this, initialState)
        override val lifecycle: Lifecycle get() = registry
        override val savedStateRegistry: SavedStateRegistry = SavedStateRegistry()
    }

    private class TestViewModelWithSavedState(
        handle: SavedStateHandle
    ) : ViewModel(handle)

    @Test
    fun testSavedStateHandleBasicReadWrite() {
        val handle = SavedStateHandle(mapOf("initialKey" to "initialValue"))

        assertEquals("initialValue", handle.get<String>("initialKey"))
        assertEquals("initialValue", handle["initialKey"])
        assertTrue(handle.contains("initialKey"))

        handle["user_id"] = 12345
        assertEquals(12345, handle.get<Int>("user_id"))

        handle["score"] = 98.5
        assertEquals(98.5, handle.get<Double>("score"))

        assertEquals(setOf("initialKey", "user_id", "score"), handle.keys())

        val removed = handle.remove<Int>("user_id")
        assertEquals(12345, removed)
        assertFalse(handle.contains("user_id"))
        assertNull(handle.get<Int>("user_id"))

        val snapshot = handle.toMap()
        assertEquals(mapOf("initialKey" to "initialValue", "score" to 98.5), snapshot)
    }

    @Test
    fun testSavedStateHandleStateFlowEmitsUpdates() = runTest {
        val handle = SavedStateHandle(mapOf("counter" to 10))
        val stateFlow = handle.getStateFlow("counter", 0)

        val emissions = mutableListOf<Int>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            stateFlow.toList(emissions)
        }

        assertEquals(listOf(10), emissions)

        handle["counter"] = 20
        handle["counter"] = 30

        assertEquals(listOf(10, 20, 30), emissions)

        job.cancel()
    }

    @Test
    fun testSaveableMutableStateFlowTwoWayBinding() = runTest {
        val handle = SavedStateHandle()
        val mutableFlow = handle.saveableMutableStateFlow("query", "initial")

        assertEquals("initial", handle["query"])
        assertEquals("initial", mutableFlow.value)

        // Mutating StateFlow updates handle
        mutableFlow.value = "search_query"
        assertEquals("search_query", handle["query"])

        // Mutating handle updates StateFlow
        handle["query"] = "updated_query"
        assertEquals("updated_query", mutableFlow.value)
    }

    @Test
    fun testSavedStateRegistryRegisterAndSaveState() {
        val registry = SavedStateRegistry()

        registry.registerSavedStateProvider("user_prefs") {
            mapOf("dark_mode" to true, "theme" to "system")
        }

        registry.registerSavedStateProvider("auth_state") {
            mapOf("token" to "xyz123")
        }

        val saved = registry.performSave()
        assertEquals(
            mapOf(
                "user_prefs" to mapOf("dark_mode" to true, "theme" to "system"),
                "auth_state" to mapOf("token" to "xyz123")
            ),
            saved
        )
    }

    @Test
    fun testSavedStateRegistryRestoreAndConsumeState() {
        val registry = SavedStateRegistry()
        assertFalse(registry.isRestored)

        val restoredBundle = mapOf(
            "screen_state" to mapOf("scroll_position" to 450),
            "cart" to listOf("item1", "item2")
        )

        registry.performRestore(restoredBundle)
        assertTrue(registry.isRestored)

        val consumedScreen: Map<String, Any?>? = registry.consumeRestoredStateForKey("screen_state")
        assertEquals(mapOf("scroll_position" to 450), consumedScreen)

        // Consuming again returns null
        val consumedAgain: Map<String, Any?>? = registry.consumeRestoredStateForKey("screen_state")
        assertNull(consumedAgain)

        // Unconsumed state remains in performSave()
        val reSaved = registry.performSave()
        assertEquals(mapOf("cart" to listOf("item1", "item2")), reSaved)
    }

    @Test
    fun testSavedStateRegistryDuplicateKeyThrows() {
        val registry = SavedStateRegistry()
        registry.registerSavedStateProvider("provider1") { emptyMap() }

        assertFailsWith<IllegalArgumentException> {
            registry.registerSavedStateProvider("provider1") { emptyMap() }
        }
    }

    @Test
    fun testSavedStateRegistryUnregisterProvider() {
        val registry = SavedStateRegistry()
        registry.registerSavedStateProvider("temp") { mapOf("temp_val" to 1) }

        assertEquals(mapOf("temp" to mapOf("temp_val" to 1)), registry.performSave())

        registry.unregisterSavedStateProvider("temp")
        assertEquals(emptyMap(), registry.performSave())
    }

    @Test
    fun testSavedStateRegistryOwnerCreateSavedStateHandle() {
        val owner = TestSavedStateRegistryOwner(LifecycleState.CREATED)

        val restoredBundle = mapOf(
            "my_feature" to mapOf("cached_key" to "cached_value")
        )
        owner.savedStateRegistry.performRestore(restoredBundle)

        val handle = owner.createSavedStateHandle("my_feature")
        assertEquals("cached_value", handle["cached_key"])

        handle["new_key"] = "new_value"

        val reSaved = owner.savedStateRegistry.performSave()
        assertEquals(
            mapOf("my_feature" to mapOf("cached_key" to "cached_value", "new_key" to "new_value")),
            reSaved
        )
    }

    @Test
    fun testViewModelWithSavedStateHandle() {
        val handle = SavedStateHandle(mapOf("vm_key" to "vm_value"))
        val viewModel = TestViewModelWithSavedState(handle)

        assertEquals(handle, viewModel.savedStateHandle)
        assertEquals(handle, viewModel.requireSavedStateHandle())
        assertEquals("vm_value", viewModel.requireSavedStateHandle()["vm_key"])

        val plainViewModel = object : ViewModel() {}
        assertNull(plainViewModel.savedStateHandle)
        assertFailsWith<IllegalStateException> {
            plainViewModel.requireSavedStateHandle()
        }
    }
}
