package com.landoulsi.schemaui.state

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * Reactive key-value store that backs all stateful UI elements (text fields, toggles, etc.).
 *
 * The state is a simple [Map]<[String], [String]> where:
 * - Keys are the [stateKey] values defined in schema [TextFieldSchemaNode]s and similar nodes.
 * - Values are always strings — renderers parse them as needed (e.g., for numeric inputs).
 *
 * The [state] [StateFlow] is intended to be collected by the Compose renderer to trigger
 * recomposition when any value changes. The [observe] function returns a [Flow] scoped to
 * a single key for more targeted subscriptions.
 *
 * Thread-safety: [MutableStateFlow] is thread-safe; [set] can be called from any thread.
 */
class StateStore {

    private val _state = MutableStateFlow<Map<String, String>>(emptyMap())

    /** Observable snapshot of the full state. Emits on every change. */
    val state: StateFlow<Map<String, String>> = _state.asStateFlow()

    /**
     * Sets [value] for [key]. Triggers a new emission on [state] and any [observe] collectors.
     */
    fun set(key: String, value: String) {
        _state.update { current -> current + (key to value) }
    }

    /**
     * Returns the current value for [key], or null if the key has not been set.
     * Non-reactive — for single reads without observation.
     */
    fun get(key: String): String? = _state.value[key]

    /**
     * Returns a [Flow] that emits the current and future values for [key].
     * Emits only when the value for [key] changes (distinct until changed).
     * Emits null when the key is absent.
     */
    fun observe(key: String): Flow<String?> = _state.map { it[key] }.distinctUntilChanged()

    /**
     * Removes a key from the store.
     */
    fun remove(key: String) {
        _state.update { current -> current - key }
    }

    /**
     * Resets all state to an empty map in a thread-safe manner.
     */
    fun clear() {
        _state.update { emptyMap() }
    }

    /**
     * Returns a snapshot of the entire current state as a plain [Map].
     * Useful for passing to action handlers that need the full form state.
     */
    fun snapshot(): Map<String, String> = _state.value
}
