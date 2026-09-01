package com.landoulsi.viewmodel

/**
 * Functional interface for observing lifecycle state transitions of a [LifecycleOwner].
 */
fun interface LifecycleObserver {
    /**
     * Notifies the observer of a lifecycle state change.
     *
     * @param owner The [LifecycleOwner] whose state transitioned.
     * @param state The new [LifecycleState].
     */
    fun onStateChanged(owner: LifecycleOwner, state: LifecycleState)
}

/**
 * Callback interface for listening to individual component lifecycle events.
 *
 * Provides default no-op implementations so implementers only need to override
 * the specific lifecycle callbacks they are interested in.
 */
interface DefaultLifecycleObserver : LifecycleObserver {

    /**
     * Called when the [LifecycleOwner] enters the [LifecycleState.CREATED] state.
     */
    fun onCreate(owner: LifecycleOwner) {}

    /**
     * Called when the [LifecycleOwner] enters the [LifecycleState.STARTED] state (moving up).
     */
    fun onStart(owner: LifecycleOwner) {}

    /**
     * Called when the [LifecycleOwner] enters the [LifecycleState.RESUMED] state.
     */
    fun onResume(owner: LifecycleOwner) {}

    /**
     * Called when the [LifecycleOwner] transitions down from [LifecycleState.RESUMED] to [LifecycleState.STARTED].
     */
    fun onPause(owner: LifecycleOwner) {}

    /**
     * Called when the [LifecycleOwner] transitions down from [LifecycleState.STARTED] to [LifecycleState.CREATED].
     */
    fun onStop(owner: LifecycleOwner) {}

    /**
     * Called when the [LifecycleOwner] enters the [LifecycleState.DESTROYED] state.
     */
    fun onDestroy(owner: LifecycleOwner) {}

    override fun onStateChanged(owner: LifecycleOwner, state: LifecycleState) {}
}
