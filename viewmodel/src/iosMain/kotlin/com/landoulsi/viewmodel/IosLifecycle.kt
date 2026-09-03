package com.landoulsi.viewmodel

import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.UIKit.UIApplicationDidBecomeActiveNotification
import platform.UIKit.UIApplicationDidEnterBackgroundNotification
import platform.UIKit.UIApplicationWillEnterForegroundNotification
import platform.UIKit.UIApplicationWillResignActiveNotification
import platform.UIKit.UIApplicationWillTerminateNotification
import platform.darwin.NSObjectProtocol
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.ref.createCleaner

/**
 * A [LifecycleOwner] implementation tailored for UIKit's `UIViewController`.
 *
 * Exposes Swift-friendly lifecycle callback hooks corresponding to standard `UIViewController`
 * lifecycle callbacks. State transitions are processed linearly through an internal [LifecycleRegistry],
 * ensuring subscribers receive orderly callbacks without state jumps.
 *
 * In UIKit, callers must invoke [destroy] or [dispose] from `UIViewController.deinit` (or when tearing
 * down the view controller) to transition the lifecycle to [LifecycleState.DESTROYED] and release
 * bound [ViewModel] coroutine scopes and observers.
 */
class UIViewControllerLifecycleOwner : LifecycleOwner {

    private val registry = LifecycleRegistry(this)

    override val lifecycle: Lifecycle
        get() = registry

    /**
     * Call when the view controller has loaded its view hierarchy (e.g. within `viewDidLoad()`).
     * Transitions the lifecycle state to [LifecycleState.CREATED].
     */
    fun viewDidLoad() {
        registry.currentState = LifecycleState.CREATED
    }

    /**
     * Call when the view is about to be added to the window hierarchy (e.g. within `viewWillAppear(_:)`).
     * Transitions the lifecycle state to [LifecycleState.STARTED].
     */
    fun viewWillAppear() {
        registry.currentState = LifecycleState.STARTED
    }

    /**
     * Call when the view has been added to the window hierarchy (e.g. within `viewDidAppear(_:)`).
     * Transitions the lifecycle state to [LifecycleState.RESUMED].
     */
    fun viewDidAppear() {
        registry.currentState = LifecycleState.RESUMED
    }

    /**
     * Call when the view is about to be removed from the window hierarchy (e.g. within `viewWillDisappear(_:)`).
     * Transitions the lifecycle state to [LifecycleState.STARTED].
     */
    fun viewWillDisappear() {
        registry.currentState = LifecycleState.STARTED
    }

    /**
     * Call when the view has been removed from the window hierarchy (e.g. within `viewDidDisappear(_:)`).
     * Transitions the lifecycle state to [LifecycleState.CREATED].
     */
    fun viewDidDisappear() {
        registry.currentState = LifecycleState.CREATED
    }

    /**
     * Explicitly transitions the lifecycle state to [LifecycleState.DESTROYED], unregistering
     * observers and cancelling bound [ViewModel] scopes.
     *
     * Should be invoked from `UIViewController.deinit` to complete the lifecycle teardown contract.
     */
    fun destroy() {
        if (registry.currentState != LifecycleState.DESTROYED) {
            registry.currentState = LifecycleState.DESTROYED
        }
    }

    /**
     * Alias for [destroy] to provide standard disposal nomenclature.
     */
    fun dispose() {
        destroy()
    }
}

/**
 * Represents SwiftUI's `ScenePhase` states for lifecycle synchronization.
 */
enum class ScenePhase {
    /**
     * The scene is in the foreground and interactive (maps to [LifecycleState.RESUMED]).
     */
    ACTIVE,

    /**
     * The scene is in the foreground but not receiving events (maps to [LifecycleState.STARTED]).
     */
    INACTIVE,

    /**
     * The scene is running in the background and not visible (maps to [LifecycleState.CREATED]).
     */
    BACKGROUND,
}

/**
 * A [LifecycleOwner] implementation tailored for declarative SwiftUI views.
 *
 * Exposes Swift-friendly lifecycle hooks such as [onAppear], [onDisappear], and [onScenePhaseChange].
 * Callers should invoke [destroy] or [dispose] during view or coordinator teardown to transition
 * to [LifecycleState.DESTROYED].
 */
class SwiftUiLifecycleOwner : LifecycleOwner {

    private val registry = LifecycleRegistry(this)

    override val lifecycle: Lifecycle
        get() = registry

    /**
     * Call when the SwiftUI view appears (e.g. `.onAppear { ... }`).
     * Transitions the lifecycle state to [LifecycleState.RESUMED].
     */
    fun onAppear() {
        registry.currentState = LifecycleState.RESUMED
    }

    /**
     * Call when the SwiftUI view disappears (e.g. `.onDisappear { ... }`).
     * Transitions the lifecycle state to [LifecycleState.CREATED].
     */
    fun onDisappear() {
        registry.currentState = LifecycleState.CREATED
    }

    /**
     * Call when the SwiftUI scene phase changes (e.g. `.onChange(of: scenePhase) { phase in ... }`).
     *
     * @param phase The current SwiftUI [ScenePhase].
     */
    fun onScenePhaseChange(phase: ScenePhase) {
        registry.currentState = when (phase) {
            ScenePhase.ACTIVE -> LifecycleState.RESUMED
            ScenePhase.INACTIVE -> LifecycleState.STARTED
            ScenePhase.BACKGROUND -> LifecycleState.CREATED
        }
    }

    /**
     * Explicitly transitions the lifecycle state to [LifecycleState.DESTROYED].
     */
    fun destroy() {
        if (registry.currentState != LifecycleState.DESTROYED) {
            registry.currentState = LifecycleState.DESTROYED
        }
    }

    /**
     * Alias for [destroy] to provide standard disposal nomenclature.
     */
    fun dispose() {
        destroy()
    }
}

/**
 * Bridges UIKit application-wide lifecycle notifications from [NSNotificationCenter]
 * to the KMP [Lifecycle] abstraction.
 *
 * Observes:
 * - [UIApplicationDidBecomeActiveNotification] -> [LifecycleState.RESUMED]
 * - [UIApplicationWillResignActiveNotification] -> [LifecycleState.STARTED]
 * - [UIApplicationDidEnterBackgroundNotification] -> [LifecycleState.CREATED]
 * - [UIApplicationWillEnterForegroundNotification] -> [LifecycleState.STARTED]
 * - [UIApplicationWillTerminateNotification] -> [LifecycleState.DESTROYED]
 *
 * Automatically unregisters all notification observers upon reaching [LifecycleState.DESTROYED]
 * (via notification or internal transition) or when [dispose] is explicitly called, preventing
 * retain cycles and memory leaks.
 */
@OptIn(ExperimentalAtomicApi::class)
class AppLifecycleBridge(
    private val notificationCenter: NSNotificationCenter = NSNotificationCenter.defaultCenter,
) : LifecycleOwner {

    private val registry = LifecycleRegistry(this)

    override val lifecycle: Lifecycle
        get() = registry

    private val observerTokens = mutableListOf<NSObjectProtocol>()

    private val isDisposed = AtomicBoolean(false)

    init {
        registry.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                dispose()
            }
        })

        registerNotification(UIApplicationDidBecomeActiveNotification) {
            registry.currentState = LifecycleState.RESUMED
        }
        registerNotification(UIApplicationWillResignActiveNotification) {
            registry.currentState = LifecycleState.STARTED
        }
        registerNotification(UIApplicationDidEnterBackgroundNotification) {
            registry.currentState = LifecycleState.CREATED
        }
        registerNotification(UIApplicationWillEnterForegroundNotification) {
            registry.currentState = LifecycleState.STARTED
        }
        registerNotification(UIApplicationWillTerminateNotification) {
            registry.currentState = LifecycleState.DESTROYED
        }
    }

    /**
     * Unregisters all [NSNotificationCenter] observers and transitions to [LifecycleState.DESTROYED]
     * if not already destroyed. Thread-safe and idempotent.
     */
    fun dispose() {
        if (!isDisposed.compareAndSet(false, true)) return
        for (token in observerTokens) {
            notificationCenter.removeObserver(token)
        }
        observerTokens.clear()
        if (registry.currentState != LifecycleState.DESTROYED) {
            registry.currentState = LifecycleState.DESTROYED
        }
    }

    private fun registerNotification(name: String?, onNotification: () -> Unit) {
        if (name == null) return
        val token = notificationCenter.addObserverForName(
            name = name,
            `object` = null,
            queue = NSOperationQueue.mainQueue,
        ) { _ ->
            if (!isDisposed.load() && registry.currentState != LifecycleState.DESTROYED) {
                onNotification()
            }
        }
        observerTokens.add(token)
    }
}

/**
 * Swift-friendly deallocation watcher and scope cleaner.
 *
 * Retained as a property in a Swift `UIViewController` or SwiftUI view holder.
 * When released upon deinit, it automatically triggers [ViewModel.clear] via Kotlin/Native's
 * cleaner hook, cancelling the coroutine scope and releasing resources. Can also be manually
 * disposed via [dispose] or [clear].
 */
@OptIn(ExperimentalAtomicApi::class, ExperimentalNativeApi::class)
class ViewModelDeallocWatcher(
    private val viewModel: ViewModel,
) {
    private val isDisposed = AtomicBoolean(false)

    @Suppress("unused")
    private val cleaner = createCleaner(viewModel) { vm ->
        vm.clear()
    }

    /**
     * Triggers teardown of the associated [ViewModel], cancelling active coroutines and
     * executing [ViewModel.onCleared]. Thread-safe and idempotent.
     */
    fun dispose() {
        if (!isDisposed.compareAndSet(false, true)) return
        viewModel.clear()
    }

    /**
     * Alias for [dispose].
     */
    fun clear() {
        dispose()
    }
}

/**
 * Binds this [ViewModel] to a [UIViewControllerLifecycleOwner].
 *
 * Automatically calls [ViewModel.clear] when the view controller lifecycle is destroyed.
 * Swift callers should invoke [UIViewControllerLifecycleOwner.destroy] or
 * [UIViewControllerLifecycleOwner.dispose] in `deinit`.
 *
 * @param owner The [UIViewControllerLifecycleOwner] to bind to.
 */
fun ViewModel.bindToViewController(owner: UIViewControllerLifecycleOwner) {
    bindToLifecycle(owner)
}

/**
 * Binds this [ViewModel] to an [AppLifecycleBridge], clearing the view model when the application terminates.
 *
 * @param bridge The [AppLifecycleBridge] to observe, defaulting to a new instance.
 * @return The [AppLifecycleBridge] instance.
 */
fun ViewModel.bindToAppLifecycle(bridge: AppLifecycleBridge = AppLifecycleBridge()): AppLifecycleBridge {
    bindToLifecycle(bridge)
    return bridge
}

/**
 * Creates and attaches a [ViewModelDeallocWatcher] for this [ViewModel].
 *
 * The watcher can be retained by Swift components to automatically trigger [ViewModel.clear]
 * upon ARC deallocation / deinit.
 *
 * @return A [ViewModelDeallocWatcher] linked to this [ViewModel].
 */
fun ViewModel.attachDeallocWatcher(): ViewModelDeallocWatcher {
    return ViewModelDeallocWatcher(this)
}

