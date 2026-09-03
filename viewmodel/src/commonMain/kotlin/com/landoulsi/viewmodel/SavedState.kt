package com.landoulsi.viewmodel

import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi

/**
 * Thread-safe key-value store for preserving state across configuration changes,
 * backgrounding, process recreation, and view model lifecycles.
 *
 * Supports storing primitive values, strings, collections, and custom serializable structures,
 * with first-class reactive bindings via [StateFlow] and [MutableStateFlow].
 */
@OptIn(ExperimentalAtomicApi::class)
class SavedStateHandle(
    initialState: Map<String, Any?> = emptyMap()
) {
    private val values = AtomicReference<Map<String, Any?>>(initialState.toMap())
    private val flows = AtomicReference<Map<String, MutableStateFlow<Any?>>>(emptyMap())

    /**
     * Retrieves the saved value associated with [key], or `null` if not found.
     */
    operator fun <T> get(key: String): T? = getInternal(key)

    /**
     * Retrieves the saved value associated with [key], or `null` if not found.
     */
    fun <T> getValue(key: String): T? = getInternal(key)

    @Suppress("UNCHECKED_CAST")
    private fun <T> getInternal(key: String): T? {
        return values.load()[key] as T?
    }

    /**
     * Sets or updates the saved value associated with [key].
     *
     * If a [StateFlow] or [MutableStateFlow] was created for [key] via [getStateFlow] or
     * [saveableMutableStateFlow], its value is updated immediately.
     */
    operator fun <T> set(key: String, value: T?) {
        setInternal(key, value)
    }

    private fun setInternal(key: String, value: Any?) {
        var flowToUpdate: MutableStateFlow<Any?>? = null
        while (true) {
            val currentMap = values.load()
            val newMap = currentMap.toMutableMap()
            if (value == null) {
                newMap.remove(key)
            } else {
                newMap[key] = value
            }
            if (values.compareAndSet(currentMap, newMap)) {
                flowToUpdate = flows.load()[key]
                break
            }
        }
        flowToUpdate?.value = value
    }

    /**
     * Returns `true` if this handle contains a mapping for [key].
     */
    fun contains(key: String): Boolean {
        return values.load().containsKey(key)
    }

    /**
     * Removes the mapping for [key] from this handle, returning the previous value or `null`.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> remove(key: String): T? {
        var removedValue: Any? = null
        var flowToUpdate: MutableStateFlow<Any?>? = null
        while (true) {
            val currentMap = values.load()
            if (!currentMap.containsKey(key)) {
                return null
            }
            removedValue = currentMap[key]
            val newMap = currentMap.toMutableMap()
            newMap.remove(key)
            if (values.compareAndSet(currentMap, newMap)) {
                flowToUpdate = flows.load()[key]
                break
            }
        }
        flowToUpdate?.value = null
        return removedValue as T?
    }

    /**
     * Returns a [Set] of all keys contained in this handle.
     */
    fun keys(): Set<String> {
        return values.load().keys
    }

    /**
     * Returns an immutable snapshot copy of all key-value entries in this handle.
     */
    fun toMap(): Map<String, Any?> {
        return values.load().toMap()
    }

    /**
     * Returns a read-only [StateFlow] that emits the current and future values associated with [key].
     *
     * If [key] already exists in this handle, the current value is emitted.
     * Otherwise, [initialValue] is used and stored in the handle.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> getStateFlow(key: String, initialValue: T): StateFlow<T> {
        return getOrCreateFlow(key, initialValue).asStateFlow() as StateFlow<T>
    }

    /**
     * Returns a [MutableStateFlow] whose value updates are synchronized with this [SavedStateHandle].
     *
     * Setting [MutableStateFlow.value] writes back to this handle, and mutating this handle via [set]
     * updates the returned flow.
     */
    fun <T> saveableMutableStateFlow(key: String, initialValue: T): MutableStateFlow<T> {
        val backingFlow = getOrCreateFlow(key, initialValue)
        return SaveableStateFlowWrapper<T>(this, key, backingFlow)
    }

    /**
     * Alias for [saveableMutableStateFlow].
     */
    fun <T> getMutableStateFlow(key: String, initialValue: T): MutableStateFlow<T> {
        return saveableMutableStateFlow(key, initialValue)
    }

    /**
     * Converts this [SavedStateHandle] into a [SavedStateProvider] suitable for registration
     * with a [SavedStateRegistry].
     */
    fun toSavedStateProvider(): SavedStateProvider {
        return SavedStateProvider { toMap() }
    }

    private fun getOrCreateFlow(key: String, initialValue: Any?): MutableStateFlow<Any?> {
        while (true) {
            val currentFlows = flows.load()
            val existing = currentFlows[key]
            if (existing != null) {
                return existing
            }
            val existingVal = values.load()[key]
            val initial = existingVal ?: initialValue
            if (existingVal == null && initialValue != null) {
                setInternal(key, initialValue)
            }
            val newFlow = MutableStateFlow(initial)
            val newFlows = currentFlows.toMutableMap()
            newFlows[key] = newFlow
            if (flows.compareAndSet(currentFlows, newFlows)) {
                return newFlow
            }
        }
    }

    @Suppress("OPT_IN_USAGE")
    @OptIn(kotlinx.coroutines.InternalCoroutinesApi::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private class SaveableStateFlowWrapper<T>(
        private val handle: SavedStateHandle,
        private val key: String,
        private val backingFlow: MutableStateFlow<Any?>
    ) : MutableStateFlow<T> {

        @Suppress("UNCHECKED_CAST")
        override var value: T
            get() = backingFlow.value as T
            set(newValue) {
                handle.set(key, newValue)
            }

        override val subscriptionCount: StateFlow<Int>
            get() = backingFlow.subscriptionCount

        override val replayCache: List<T>
            @Suppress("UNCHECKED_CAST")
            get() = backingFlow.replayCache as List<T>

        @Suppress("UNCHECKED_CAST")
        override suspend fun collect(collector: FlowCollector<T>): Nothing {
            backingFlow.collect { value ->
                collector.emit(value as T)
            }
        }

        override fun compareAndSet(expect: T, update: T): Boolean {
            if (value == expect) {
                value = update
                return true
            }
            return false
        }

        override suspend fun emit(value: T) {
            this.value = value
        }

        override fun tryEmit(value: T): Boolean {
            this.value = value
            return true
        }

        override fun resetReplayCache() {
            backingFlow.resetReplayCache()
        }
    }
}

/**
 * Interface for components that contribute state bundles to be saved and restored.
 */
fun interface SavedStateProvider {
    /**
     * Called when component state should be saved.
     *
     * @return A map containing serializable/preservable state entries.
     */
    fun saveState(): Map<String, Any?>
}

/**
 * Platform-agnostic registry for managing [SavedStateProvider] instances and coordinating
 * state restoration and state snapshot generation.
 */
@OptIn(ExperimentalAtomicApi::class)
class SavedStateRegistry {

    private val providers = AtomicReference<Map<String, SavedStateProvider>>(emptyMap())
    private val restoredState = AtomicReference<Map<String, Any?>?>(null)
    private val isRestoredFlag = AtomicBoolean(false)

    /**
     * Whether state has been restored into this registry via [performRestore] / [restoreState].
     */
    val isRestored: Boolean
        get() = isRestoredFlag.load()

    /**
     * Registers a [SavedStateProvider] with the specified [key].
     *
     * @param key Unique key identifying this state provider.
     * @param provider The [SavedStateProvider] instance to register.
     * @throws IllegalArgumentException if a provider with [key] is already registered.
     */
    fun registerSavedStateProvider(key: String, provider: SavedStateProvider) {
        while (true) {
            val current = providers.load()
            require(!current.containsKey(key)) {
                "SavedStateProvider with the key '$key' is already registered"
            }
            val updated = current + (key to provider)
            if (providers.compareAndSet(current, updated)) {
                break
            }
        }
    }

    /**
     * Unregisters the [SavedStateProvider] associated with [key].
     *
     * @param key The key of the provider to unregister.
     */
    fun unregisterSavedStateProvider(key: String) {
        while (true) {
            val current = providers.load()
            val updated = current - key
            if (providers.compareAndSet(current, updated)) {
                break
            }
        }
    }

    /**
     * Retrieves the registered [SavedStateProvider] for [key], or `null` if none is registered.
     */
    fun getSavedStateProvider(key: String): SavedStateProvider? {
        return providers.load()[key]
    }

    /**
     * Consumes and returns previously restored state associated with [key], removing it from the unconsumed state pool.
     *
     * @param key The key whose restored state is being queried.
     * @return The restored value, or `null` if no state was restored for [key].
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> consumeRestoredStateForKey(key: String): T? {
        while (true) {
            val current = restoredState.load() ?: return null
            if (!current.containsKey(key)) return null
            val value = current[key]
            val updated = current - key
            if (restoredState.compareAndSet(current, updated)) {
                return value as T?
            }
        }
    }

    /**
     * Restores state into the registry from a previous saved state map.
     *
     * @param savedState The saved state map to restore from.
     */
    fun performRestore(savedState: Map<String, Any?>?) {
        if (savedState != null) {
            restoredState.store(savedState.toMap())
        }
        isRestoredFlag.store(true)
    }

    /**
     * Alias for [performRestore].
     */
    fun restoreState(savedState: Map<String, Any?>?) {
        performRestore(savedState)
    }

    /**
     * Collects saved state from all registered [SavedStateProvider] instances and combines it with
     * any unconsumed restored state into an immutable map snapshot.
     *
     * @return An immutable map containing all saved state entries.
     */
    fun performSave(): Map<String, Any?> {
        val result = mutableMapOf<String, Any?>()
        restoredState.load()?.let {
            result.putAll(it)
        }
        val currentProviders = providers.load()
        for ((key, provider) in currentProviders) {
            result[key] = provider.saveState()
        }
        return result.toMap()
    }

    /**
     * Alias for [performSave].
     */
    fun saveState(): Map<String, Any?> = performSave()
}

/**
 * Interface representing a component that owns a [SavedStateRegistry].
 */
interface SavedStateRegistryOwner : LifecycleOwner {
    /**
     * The [SavedStateRegistry] owned by this component.
     */
    val savedStateRegistry: SavedStateRegistry
}

/**
 * Controller for coordinating state restoration and persistence on a [SavedStateRegistryOwner].
 */
class SavedStateRegistryController private constructor(
    val owner: SavedStateRegistryOwner
) {
    /**
     * The [SavedStateRegistry] associated with the owner.
     */
    val savedStateRegistry: SavedStateRegistry
        get() = owner.savedStateRegistry

    /**
     * Restores state into the owner's registry.
     */
    fun performRestore(savedState: Map<String, Any?>?) {
        savedStateRegistry.performRestore(savedState)
    }

    /**
     * Generates a saved state map snapshot from the owner's registry.
     */
    fun performSave(): Map<String, Any?> {
        return savedStateRegistry.performSave()
    }

    companion object {
        /**
         * Creates a new [SavedStateRegistryController] bound to the given [owner].
         */
        fun create(owner: SavedStateRegistryOwner): SavedStateRegistryController {
            return SavedStateRegistryController(owner)
        }
    }
}

/**
 * Creates and registers a [SavedStateHandle] with this [SavedStateRegistryOwner], automatically
 * restoring any previously saved state for [key] and registering a provider to save future state.
 *
 * @param key Unique key for this [SavedStateHandle].
 * @param defaultState Default initial state map if no restored state is found.
 * @return A configured [SavedStateHandle] bound to this owner's registry.
 */
fun SavedStateRegistryOwner.createSavedStateHandle(
    key: String,
    defaultState: Map<String, Any?> = emptyMap()
): SavedStateHandle {
    val restored: Map<String, Any?>? = savedStateRegistry.consumeRestoredStateForKey(key)
    val handle = SavedStateHandle(restored ?: defaultState)
    savedStateRegistry.registerSavedStateProvider(key, handle.toSavedStateProvider())
    return handle
}
