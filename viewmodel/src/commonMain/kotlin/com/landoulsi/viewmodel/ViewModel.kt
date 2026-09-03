package com.landoulsi.viewmodel

import androidx.annotation.CallSuper
import androidx.lifecycle.ViewModel as AndroidXViewModel
import androidx.lifecycle.viewModelScope as androidxViewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi

/**
 * Platform-agnostic base ViewModel abstraction for Kotlin Multiplatform.
 *
 * Extends [androidx.lifecycle.ViewModel] to provide lifecycle-aware coroutine scoping
 * via [viewModelScope] and automatic cancellation on destruction.
 *
 * On Android, the lifecycle is managed automatically by AndroidX ViewModelStore / Jetpack Lifecycle.
 *
 * For non-Android platforms (e.g. iOS), [clear] can be manually invoked (e.g. within a Swift wrapper's
 * `deinit` block) to cancel all active coroutines in [viewModelScope] and invoke [onCleared], preventing
 * resource and memory leaks.
 */
@OptIn(ExperimentalAtomicApi::class)
open class ViewModel : AndroidXViewModel {

    private val isCleared = AtomicBoolean(false)

    /**
     * Optional [SavedStateHandle] associated with this [ViewModel] for preserving UI state.
     */
    val savedStateHandle: SavedStateHandle?

    /**
     * A [CoroutineScope] tied to this [ViewModel]'s lifecycle.
     * Coroutines launched in this scope are cancelled when the [ViewModel] is cleared.
     */
    val viewModelScope: CoroutineScope
        get() = androidxViewModelScope

    constructor() : super() {
        this.savedStateHandle = null
    }

    constructor(viewModelScope: CoroutineScope) : super(viewModelScope) {
        this.savedStateHandle = null
    }

    constructor(savedStateHandle: SavedStateHandle) : super() {
        this.savedStateHandle = savedStateHandle
    }

    constructor(savedStateHandle: SavedStateHandle?, viewModelScope: CoroutineScope) : super(viewModelScope) {
        this.savedStateHandle = savedStateHandle
    }

    constructor(viewModelScope: CoroutineScope, savedStateHandle: SavedStateHandle?) : super(viewModelScope) {
        this.savedStateHandle = savedStateHandle
    }

    /**
     * Called when this [ViewModel] is destroyed/cleared.
     * Subclasses can override this method to release resources.
     */
    @CallSuper
    override fun onCleared() {
        if (isCleared.compareAndSet(false, true)) {
            viewModelScope.cancel()
        }
        super.onCleared()
    }

    /**
     * Manually triggers teardown of the [ViewModel], cancelling all active coroutines
     * in [viewModelScope] and invoking [onCleared].
     *
     * This method is thread-safe and idempotent; repeated invocations will be no-ops.
     * Typically invoked from Swift wrapper `deinit` on iOS.
     */
    fun clear() {
        if (isCleared.compareAndSet(false, true)) {
            viewModelScope.cancel()
            onCleared()
        }
    }
}

/**
 * Binds this [ViewModel] to a KMP [LifecycleOwner].
 *
 * Automatically calls [ViewModel.clear] when the host lifecycle enters
 * [LifecycleState.DESTROYED] or if already destroyed.
 *
 * @param owner The KMP [LifecycleOwner] to bind to.
 */
fun ViewModel.bindToLifecycle(owner: LifecycleOwner) {
    bindToLifecycle(owner.lifecycle)
}

/**
 * Binds this [ViewModel] to a KMP [Lifecycle].
 *
 * Automatically calls [ViewModel.clear] when the lifecycle enters
 * [LifecycleState.DESTROYED] or if already destroyed.
 *
 * @param lifecycle The KMP [Lifecycle] to bind to.
 */
fun ViewModel.bindToLifecycle(lifecycle: Lifecycle) {
    if (lifecycle.currentState == LifecycleState.DESTROYED) {
        clear()
        return
    }

    val observer = object : DefaultLifecycleObserver {
        override fun onDestroy(owner: LifecycleOwner) {
            clear()
            owner.lifecycle.removeObserver(this)
        }
    }
    lifecycle.addObserver(observer)
}

/**
 * Returns the [SavedStateHandle] associated with this [ViewModel], or throws [IllegalStateException] if none was attached.
 */
fun ViewModel.requireSavedStateHandle(): SavedStateHandle {
    return checkNotNull(savedStateHandle) {
        "ViewModel $this does not have a SavedStateHandle attached."
    }
}

