package com.landoulsi.viewmodel

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.concurrent.Volatile

/**
 * Base abstract class for lifecycle management.
 * Exposes the current lifecycle state, a reactive [StateFlow] stream,
 * and registration hooks for [LifecycleObserver] instances.
 */
abstract class Lifecycle {

    /**
     * The current [LifecycleState] of this component.
     */
    abstract val currentState: LifecycleState

    /**
     * A [StateFlow] emitting updates whenever the [currentState] transitions.
     */
    abstract val stateFlow: StateFlow<LifecycleState>

    /**
     * The number of active observers subscribed to this lifecycle.
     */
    abstract val observerCount: Int

    /**
     * Adds the specified [LifecycleObserver] to receive state change callbacks.
     *
     * If the lifecycle is already at a state beyond [LifecycleState.INITIALIZED],
     * the observer will be brought up to the current state through linear transitions.
     */
    abstract fun addObserver(observer: LifecycleObserver)

    /**
     * Removes the specified [LifecycleObserver] from receiving further callbacks.
     */
    abstract fun removeObserver(observer: LifecycleObserver)

    /**
     * Returns `true` if this lifecycle has at least one active observer.
     */
    open fun hasObservers(): Boolean = observerCount > 0
}

/**
 * Thread-safe, observable implementation of [Lifecycle] for [LifecycleOwner] hosts.
 *
 * Manages active observers, updates [stateFlow], and dispatches state transitions
 * linearly to prevent illegal transition jumps and support re-entrant calls safely.
 *
 * @param owner The [LifecycleOwner] provider managing this registry.
 * @param initialState The initial [LifecycleState], defaulting to [LifecycleState.INITIALIZED].
 */
open class LifecycleRegistry(
    private val owner: LifecycleOwner,
    initialState: LifecycleState = LifecycleState.INITIALIZED
) : Lifecycle() {

    private class ObserverEntry(
        val observer: LifecycleObserver,
        var state: LifecycleState
    )

    private val _stateFlow = MutableStateFlow(initialState)
    override val stateFlow: StateFlow<LifecycleState> = _stateFlow.asStateFlow()

    @Volatile
    private var isDispatching: Boolean = false

    @Volatile
    private var targetState: LifecycleState = initialState

    @Volatile
    private var observerList: List<ObserverEntry> = emptyList()

    override val observerCount: Int
        get() = observerList.size

    override var currentState: LifecycleState
        get() = _stateFlow.value
        set(value) {
            moveToState(value)
        }

    /**
     * Convenience method to trigger a lifecycle event / state change.
     */
    fun handleLifecycleEvent(state: LifecycleState) {
        currentState = state
    }

    override fun addObserver(observer: LifecycleObserver) {
        val existing = observerList.firstOrNull { it.observer == observer }
        if (existing != null) return

        val initialObserverState = if (_stateFlow.value == LifecycleState.DESTROYED) {
            LifecycleState.DESTROYED
        } else {
            LifecycleState.INITIALIZED
        }

        val entry = ObserverEntry(observer, initialObserverState)
        observerList = observerList + entry

        if (_stateFlow.value == LifecycleState.DESTROYED) {
            dispatchEvent(entry.observer, LifecycleState.DESTROYED, isUpward = false)
            return
        }

        // Catch-up: bring the observer up to the current state linearly
        while (entry.state < _stateFlow.value && observerList.any { it.observer == observer }) {
            val next = nextStepUp(entry.state) ?: break
            entry.state = next
            dispatchEvent(entry.observer, next, isUpward = true)
        }
    }

    override fun removeObserver(observer: LifecycleObserver) {
        observerList = observerList.filterNot { it.observer == observer }
    }

    private fun moveToState(nextTarget: LifecycleState) {
        if (_stateFlow.value == LifecycleState.DESTROYED && nextTarget != LifecycleState.DESTROYED) {
            return
        }

        targetState = nextTarget
        if (isDispatching) {
            return
        }

        isDispatching = true
        try {
            while (_stateFlow.value != targetState) {
                val current = _stateFlow.value
                if (current == LifecycleState.DESTROYED && targetState != LifecycleState.DESTROYED) {
                    break
                }

                val isUpward = targetState > current
                val nextState = if (isUpward) nextStepUp(current) else nextStepDown(current)
                if (nextState == null) break

                _stateFlow.value = nextState
                dispatchStep(nextState, isUpward)
            }
        } finally {
            isDispatching = false
        }
    }

    private fun dispatchStep(nextState: LifecycleState, isUpward: Boolean) {
        val currentEntries = observerList
        for (entry in currentEntries) {
            if (!observerList.contains(entry)) continue

            if (isUpward) {
                if (entry.state < nextState) {
                    entry.state = nextState
                    dispatchEvent(entry.observer, nextState, isUpward = true)
                }
            } else {
                if (entry.state > nextState || (nextState == LifecycleState.DESTROYED && entry.state != LifecycleState.DESTROYED)) {
                    entry.state = nextState
                    dispatchEvent(entry.observer, nextState, isUpward = false)
                }
            }
        }
    }

    private fun dispatchEvent(observer: LifecycleObserver, state: LifecycleState, isUpward: Boolean) {
        observer.onStateChanged(owner, state)
        if (observer is DefaultLifecycleObserver) {
            when {
                isUpward && state == LifecycleState.CREATED -> observer.onCreate(owner)
                isUpward && state == LifecycleState.STARTED -> observer.onStart(owner)
                isUpward && state == LifecycleState.RESUMED -> observer.onResume(owner)
                !isUpward && state == LifecycleState.STARTED -> observer.onPause(owner)
                !isUpward && state == LifecycleState.CREATED -> observer.onStop(owner)
                state == LifecycleState.DESTROYED -> observer.onDestroy(owner)
            }
        }
    }

    private fun nextStepUp(current: LifecycleState): LifecycleState? = when (current) {
        LifecycleState.INITIALIZED -> LifecycleState.CREATED
        LifecycleState.CREATED -> LifecycleState.STARTED
        LifecycleState.STARTED -> LifecycleState.RESUMED
        LifecycleState.RESUMED -> null
        LifecycleState.DESTROYED -> null
    }

    private fun nextStepDown(current: LifecycleState): LifecycleState? = when (current) {
        LifecycleState.RESUMED -> LifecycleState.STARTED
        LifecycleState.STARTED -> LifecycleState.CREATED
        LifecycleState.CREATED -> LifecycleState.DESTROYED
        LifecycleState.INITIALIZED -> LifecycleState.DESTROYED
        LifecycleState.DESTROYED -> null
    }
}
