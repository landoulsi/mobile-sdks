package com.landoulsi.schemaui

import com.landoulsi.schemaui.ir.UINode
import com.landoulsi.schemaui.state.StateStore
import kotlin.experimental.ExperimentalObjCName

/**
 * Swift-callable facade over [SchemaUIEngine].
 *
 * KMP sealed classes compile to ObjC base classes, so Swift code can
 * `switch` on runtime types using `as? UIColumn`, `as? UIText`, etc.
 * This facade provides a clean @objc API surface without exposing Kotlin
 * internals unnecessarily.
 *
 * ## Swift usage
 * ```swift
 * let kit = SchemaUIKit()
 * kit.registerAction(name: "submit") {
 *     let email = kit.stateStore.get(key: "email") ?? ""
 *     print("Submitted:", email)
 * }
 * if let node = kit.parseSchema(json: myJsonString) {
 *     let view = SchemaUIView(node: node, kit: kit)
 * }
 * ```
 */
@OptIn(ExperimentalObjCName::class)
@ObjCName("SchemaUIKit", swiftName = "SchemaUIKit")
class SchemaUIKit {

    private val engine = SchemaUIEngine()

    /** The reactive state store. Readable/writable from Swift. */
    val stateStore: StateStore get() = engine.stateStore

    /** The error message from the most recent failed [parseSchema] call. */
    var lastError: String? = null
        private set

    /**
     * Parses [json] and returns the root [UINode], or null if parsing fails.
     * On failure, [lastError] is set to the error message.
     */
    fun parseSchema(json: String): UINode? {
        return engine.parseFromString(json).fold(
            onSuccess = { node ->
                lastError = null
                node
            },
            onFailure = { throwable ->
                lastError = throwable.message ?: "Unknown parse error"
                null
            },
        )
    }

    /**
     * Registers a simple action handler callable from Swift.
     * @param name The action identifier string from the schema.
     * @param handler A Swift closure `() -> Void`.
     */
    fun registerAction(name: String, handler: () -> Unit) {
        engine.registerAction(name, handler)
    }

    /**
     * Registers an action handler that receives the current state snapshot.
     * The [handler] is a Swift closure `([String: String]) -> Void`.
     */
    fun registerActionWithState(name: String, handler: (Map<String, String>) -> Unit) {
        engine.registerActionWithState(name, handler)
    }

    /** Triggers the action registered under [name]. Called by the SwiftUI renderer. */
    fun triggerAction(name: String) {
        engine.triggerAction(name)
    }

    /** Removes all registered actions and clears the state store. */
    fun reset() {
        engine.clearActions()
        engine.stateStore.clear()
        lastError = null
    }
}
