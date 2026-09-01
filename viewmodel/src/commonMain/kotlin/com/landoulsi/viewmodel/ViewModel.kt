package com.landoulsi.viewmodel

import androidx.lifecycle.ViewModel as AndroidXViewModel
import androidx.lifecycle.viewModelScope as androidxViewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlin.concurrent.Volatile

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
open class ViewModel : AndroidXViewModel {

    @Volatile
    private var isCleared: Boolean = false

    /**
     * A [CoroutineScope] tied to this [ViewModel]'s lifecycle.
     * Coroutines launched in this scope are cancelled when the [ViewModel] is cleared.
     */
    open val viewModelScope: CoroutineScope
        get() = (this as AndroidXViewModel).androidxViewModelScope

    constructor() : super()

    constructor(viewModelScope: CoroutineScope) : super(viewModelScope)

    /**
     * Called when this [ViewModel] is destroyed/cleared.
     * Subclasses can override this method to release resources.
     */
    override fun onCleared() {
        isCleared = true
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
        if (isCleared) return
        isCleared = true
        viewModelScope.cancel()
        onCleared()
    }
}
