package com.landoulsi.viewmodel

/**
 * Represents the current lifecycle state of a component.
 *
 * The states follow a strict progression hierarchy from [INITIALIZED] through [RESUMED],
 * with [DESTROYED] representing the terminal lifecycle state.
 */
enum class LifecycleState {
    /**
     * Destroyed state for a component whose lifecycle has ended.
     * This is a terminal state; once reached, no further forward transitions occur.
     */
    DESTROYED,

    /**
     * Initialized state for a component whose lifecycle is initialized but not yet created.
     */
    INITIALIZED,

    /**
     * Created state for a component that has been created.
     */
    CREATED,

    /**
     * Started state for a component that is visible but not necessarily focused/interactive.
     */
    STARTED,

    /**
     * Resumed state for a component that is fully active, visible, and focused.
     */
    RESUMED;

    /**
     * Compares if this state is at least the specified [targetState].
     *
     * For example:
     * - `RESUMED.isAtLeast(STARTED)` returns `true`
     * - `STARTED.isAtLeast(CREATED)` returns `true`
     * - `CREATED.isAtLeast(STARTED)` returns `false`
     * - `DESTROYED.isAtLeast(INITIALIZED)` returns `false`
     *
     * @param targetState The state to compare against.
     * @return `true` if this state's ordinal is greater than or equal to [targetState]'s ordinal.
     */
    fun isAtLeast(targetState: LifecycleState): Boolean {
        return this >= targetState
    }
}
