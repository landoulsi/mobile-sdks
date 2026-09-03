package com.landoulsi.viewmodel

import androidx.annotation.MainThread
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.Lifecycle as AndroidxLifecycle
import androidx.lifecycle.LifecycleOwner as AndroidxLifecycleOwner
import kotlinx.coroutines.flow.StateFlow

/**
 * Converts an [AndroidxLifecycle.State] enum to the KMP [LifecycleState] enum.
 */
fun AndroidxLifecycle.State.toKmpState(): LifecycleState = when (this) {
    AndroidxLifecycle.State.DESTROYED -> LifecycleState.DESTROYED
    AndroidxLifecycle.State.INITIALIZED -> LifecycleState.INITIALIZED
    AndroidxLifecycle.State.CREATED -> LifecycleState.CREATED
    AndroidxLifecycle.State.STARTED -> LifecycleState.STARTED
    AndroidxLifecycle.State.RESUMED -> LifecycleState.RESUMED
}

/**
 * Converts a KMP [LifecycleState] enum to the [AndroidxLifecycle.State] enum.
 */
fun LifecycleState.toAndroidxState(): AndroidxLifecycle.State = when (this) {
    LifecycleState.DESTROYED -> AndroidxLifecycle.State.DESTROYED
    LifecycleState.INITIALIZED -> AndroidxLifecycle.State.INITIALIZED
    LifecycleState.CREATED -> AndroidxLifecycle.State.CREATED
    LifecycleState.STARTED -> AndroidxLifecycle.State.STARTED
    LifecycleState.RESUMED -> AndroidxLifecycle.State.RESUMED
}

/**
 * Converts an [AndroidxLifecycle.Event] enum to the corresponding target KMP [LifecycleState].
 *
 * @throws IllegalArgumentException if called with [AndroidxLifecycle.Event.ON_ANY], as `ON_ANY`
 * is a wildcard dispatch filter and does not represent a discrete target state.
 */
fun AndroidxLifecycle.Event.toTargetLifecycleState(): LifecycleState = when (this) {
    AndroidxLifecycle.Event.ON_CREATE -> LifecycleState.CREATED
    AndroidxLifecycle.Event.ON_START -> LifecycleState.STARTED
    AndroidxLifecycle.Event.ON_RESUME -> LifecycleState.RESUMED
    AndroidxLifecycle.Event.ON_PAUSE -> LifecycleState.STARTED
    AndroidxLifecycle.Event.ON_STOP -> LifecycleState.CREATED
    AndroidxLifecycle.Event.ON_DESTROY -> LifecycleState.DESTROYED
    AndroidxLifecycle.Event.ON_ANY -> throw IllegalArgumentException(
        "ON_ANY cannot be mapped to a target LifecycleState as it is a dispatch filter event",
    )
}

/**
 * Converts an [AndroidxLifecycle.Event] enum to the corresponding target KMP [LifecycleState].
 * Alias for [toTargetLifecycleState].
 */
fun AndroidxLifecycle.Event.toKmpState(): LifecycleState = toTargetLifecycleState()

/**
 * Bridges an AndroidX [AndroidxLifecycle] to the KMP [Lifecycle] abstraction.
 *
 * Automatically converts AndroidX lifecycle state and event transitions into KMP
 * [LifecycleState] notifications by delegating to a composed [LifecycleRegistry],
 * updating [stateFlow] and dispatching to registered [LifecycleObserver] and
 * [DefaultLifecycleObserver] instances linearly.
 *
 * Dispatching and observer mutations must occur on the main thread in accordance
 * with AndroidX Lifecycle contract requirements.
 *
 * @param androidLifecycle The underlying AndroidX [AndroidxLifecycle] instance.
 * @param owner The KMP [LifecycleOwner] managing this lifecycle, or a default wrapper if omitted.
 */
class AndroidLifecycleBridge(
    private val androidLifecycle: AndroidxLifecycle,
    owner: LifecycleOwner? = null,
) : Lifecycle() {

    private val actualOwner: LifecycleOwner = owner ?: object : LifecycleOwner {
        override val lifecycle: Lifecycle get() = this@AndroidLifecycleBridge
    }

    private val registry: LifecycleRegistry = LifecycleRegistry(
        owner = actualOwner,
        initialState = if (androidLifecycle.currentState == AndroidxLifecycle.State.DESTROYED) {
            LifecycleState.DESTROYED
        } else {
            LifecycleState.INITIALIZED
        },
    )

    override val currentState: LifecycleState
        get() = registry.currentState

    override val stateFlow: StateFlow<LifecycleState>
        get() = registry.stateFlow

    override val observerCount: Int
        get() = registry.observerCount

    private val eventObserver: LifecycleEventObserver = object : LifecycleEventObserver {
        override fun onStateChanged(source: AndroidxLifecycleOwner, event: AndroidxLifecycle.Event) {
            val targetState = event.toTargetLifecycleState()
            registry.handleLifecycleEvent(targetState)
            if (event == AndroidxLifecycle.Event.ON_DESTROY) {
                source.lifecycle.removeObserver(this)
            }
        }
    }

    init {
        if (androidLifecycle.currentState != AndroidxLifecycle.State.DESTROYED) {
            androidLifecycle.addObserver(eventObserver)
        }
    }

    @MainThread
    override fun addObserver(observer: LifecycleObserver) {
        registry.addObserver(observer)
    }

    @MainThread
    override fun removeObserver(observer: LifecycleObserver) {
        registry.removeObserver(observer)
    }

    override fun hasObservers(): Boolean {
        return registry.hasObservers()
    }
}

/**
 * Wraps this AndroidX [AndroidxLifecycleOwner] into a KMP [LifecycleOwner].
 *
 * Note: Each invocation creates a new [LifecycleOwner] and [AndroidLifecycleBridge] wrapper
 * that registers an observer with this host lifecycle until it is destroyed. For continuous
 * state observation across a component lifecycle, retain the returned instance.
 */
fun AndroidxLifecycleOwner.asKmpLifecycleOwner(): LifecycleOwner {
    return object : LifecycleOwner {
        override val lifecycle: Lifecycle = AndroidLifecycleBridge(
            androidLifecycle = this@asKmpLifecycleOwner.lifecycle,
            owner = this,
        )
    }
}

/**
 * Wraps this AndroidX [AndroidxLifecycle] into a KMP [Lifecycle].
 *
 * Note: Each invocation creates a new [AndroidLifecycleBridge] wrapper that registers an observer
 * with this lifecycle until destroyed. Retain the returned instance to avoid redundant observers.
 *
 * @param owner Optional KMP [LifecycleOwner] managing this lifecycle.
 */
fun AndroidxLifecycle.asKmpLifecycle(owner: LifecycleOwner? = null): Lifecycle {
    return AndroidLifecycleBridge(androidLifecycle = this, owner = owner)
}

/**
 * Binds this [ViewModel] to an Android [AndroidxLifecycleOwner].
 *
 * Automatically calls [ViewModel.clear] when the host lifecycle reaches
 * [AndroidxLifecycle.Event.ON_DESTROY] or if the lifecycle is already destroyed,
 * cancelling all coroutines running in [ViewModel.viewModelScope] and invoking [ViewModel.onCleared].
 *
 * @param owner The Android [AndroidxLifecycleOwner] to bind to.
 */
fun ViewModel.bindToLifecycle(owner: AndroidxLifecycleOwner) {
    bindToLifecycle(owner.lifecycle)
}

/**
 * Binds this [ViewModel] to an Android [AndroidxLifecycle].
 *
 * Automatically calls [ViewModel.clear] when the lifecycle reaches
 * [AndroidxLifecycle.Event.ON_DESTROY] or if the lifecycle is already destroyed,
 * cancelling all coroutines running in [ViewModel.viewModelScope] and invoking [ViewModel.onCleared].
 *
 * @param lifecycle The Android [AndroidxLifecycle] to bind to.
 */
fun ViewModel.bindToLifecycle(lifecycle: AndroidxLifecycle) {
    if (lifecycle.currentState == AndroidxLifecycle.State.DESTROYED) {
        clear()
        return
    }

    val observer = object : LifecycleEventObserver {
        override fun onStateChanged(source: AndroidxLifecycleOwner, event: AndroidxLifecycle.Event) {
            if (event == AndroidxLifecycle.Event.ON_DESTROY) {
                clear()
                source.lifecycle.removeObserver(this)
            }
        }
    }
    lifecycle.addObserver(observer)
}
