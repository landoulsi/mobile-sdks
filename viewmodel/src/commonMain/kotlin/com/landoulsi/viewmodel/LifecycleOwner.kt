package com.landoulsi.viewmodel

/**
 * A class that possesses a [Lifecycle].
 *
 * Types implementing this interface can be observed by [LifecycleObserver]
 * and [DefaultLifecycleObserver] instances to track component active and inactive states.
 */
interface LifecycleOwner {
    /**
     * The [Lifecycle] owned by this component.
     */
    val lifecycle: Lifecycle
}
