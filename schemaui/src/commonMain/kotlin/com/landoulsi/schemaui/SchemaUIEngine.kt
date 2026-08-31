package com.landoulsi.schemaui

import com.landoulsi.schemaui.ir.UINode
import com.landoulsi.schemaui.state.StateStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/**
 * The central coordinator for SchemaUI. Host applications interact exclusively
 * with this class to:
 * 1. Parse JSON schemas into [UINode] IR trees.
 * 2. Register named action handlers (button clicks, form submissions).
 * 3. Access the reactive [StateStore] for reading/writing form state.
 *
 * ## Typical Android usage
 * ```kotlin
 * val engine = SchemaUIEngine()
 * engine.registerAction("submit") {
 *     val email = engine.stateStore.get("email")
 *     // handle submit
 * }
 * val rootNode = engine.parseFromString(myJson).getOrThrow()
 * // Pass engine + rootNode to the SchemaUI() composable
 * ```
 *
 * ## Typical iOS usage (from Swift)
 * ```swift
 * let engine = SchemaUIEngine()
 * engine.registerAction(name: "submit") {
 *     let email = engine.stateStore.get(key: "email")
 * }
 * let result = engine.parseFromString(json: myJson)
 * // Pass engine + result to SchemaUIView(node:engine:)
 * ```
 *
 * @param stateStore Optionally inject a pre-populated or shared [StateStore].
 *                   If not provided, a fresh empty store is created.
 */
class SchemaUIEngine(
    val stateStore: StateStore = StateStore(),
) {
    private val parser = SchemaUIParser()
    private val actions = MutableStateFlow<Map<String, (Map<String, String>) -> Unit>>(emptyMap())

    // ─── Parsing ─────────────────────────────────────────────────────────────

    /**
     * Parses a JSON [jsonString] into a [UINode] IR tree.
     *
     * @return [Result.success] with the root [UINode], or
     *         [Result.failure] with a [SchemaParseException] if parsing fails.
     */
    fun parseFromString(jsonString: String): Result<UINode> = runCatching {
        parser.parse(jsonString)
    }

    // ─── Action Registration ─────────────────────────────────────────────────

    /**
     * Registers a simple no-argument action handler in a thread-safe manner.
     *
     * Example: a "navigate_back" button that needs no form state.
     *
     * @param name The action identifier string defined in the schema's `"action"` field.
     * @param handler Called on the calling thread when the action is triggered.
     */
    fun registerAction(name: String, handler: () -> Unit) {
        registerActionWithState(name) { handler() }
    }

    /**
     * Registers an action handler that receives the current [StateStore] snapshot in a thread-safe manner.
     *
     * Use this for form submissions where the handler needs to read input values.
     *
     * @param name The action identifier string.
     * @param handler Called with a snapshot of the current state map.
     */
    fun registerActionWithState(name: String, handler: (Map<String, String>) -> Unit) {
        actions.update { current -> current + (name to handler) }
    }

    /**
     * Removes a previously registered action in a thread-safe manner.
     */
    fun unregisterAction(name: String) {
        actions.update { current -> current - name }
    }

    /**
     * Clears all registered actions.
     */
    fun clearActions() {
        actions.update { emptyMap() }
    }

    // ─── Action Triggering ───────────────────────────────────────────────────

    /**
     * Fires the handler registered for [name] with a snapshot of the current state.
     * Silently does nothing if [name] has no registered handler (graceful degradation).
     *
     * Renderers call this when a button is tapped or a text field's IME action fires.
     */
    fun triggerAction(name: String) {
        actions.value[name]?.invoke(stateStore.snapshot())
    }

    /**
     * Returns true if a handler is registered for [name].
     */
    fun hasAction(name: String): Boolean = actions.value.containsKey(name)
}
