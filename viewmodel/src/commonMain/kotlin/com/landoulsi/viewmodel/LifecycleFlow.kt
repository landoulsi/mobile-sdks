package com.landoulsi.viewmodel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.resume

/**
 * Runs the given [block] in a new coroutine whenever this [Lifecycle] is at least at [minActiveState]
 * and cancels the coroutine when the lifecycle drops below [minActiveState].
 *
 * If the lifecycle reaches [LifecycleState.DESTROYED], the observation is stopped and this method resumes.
 *
 * @param minActiveState The minimum [LifecycleState] required for [block] to execute. Must be at least [LifecycleState.CREATED].
 * @param block The suspendable lambda to execute while the lifecycle is at least at [minActiveState].
 * @throws IllegalArgumentException if [minActiveState] is [LifecycleState.INITIALIZED] or [LifecycleState.DESTROYED].
 */
suspend fun Lifecycle.repeatOnLifecycle(
    minActiveState: LifecycleState,
    block: suspend CoroutineScope.() -> Unit
) {
    require(minActiveState >= LifecycleState.CREATED) {
        "repeatOnLifecycle cannot be observed with minActiveState $minActiveState; state must be at least CREATED"
    }

    if (currentState == LifecycleState.DESTROYED) {
        return
    }

    coroutineScope {
        val mutex = Mutex()
        var currentJob: Job? = null
        var observer: LifecycleObserver? = null

        try {
            suspendCancellableCoroutine<Unit> { continuation ->
                val obs = LifecycleObserver { _, state ->
                    launch {
                        mutex.withLock {
                            if (state == LifecycleState.DESTROYED) {
                                currentJob?.cancel()
                                currentJob = null
                                if (continuation.isActive) {
                                    continuation.resume(Unit)
                                }
                            } else if (state.isAtLeast(minActiveState)) {
                                if (currentJob == null || currentJob?.isCompleted == true) {
                                    currentJob = launch(block = block)
                                }
                            } else {
                                currentJob?.cancel()
                                currentJob = null
                            }
                        }
                    }
                }
                observer = obs
                addObserver(obs)
            }
        } finally {
            observer?.let { removeObserver(it) }
            currentJob?.cancel()
        }
    }
}

/**
 * Runs the given [block] in a new coroutine whenever this [LifecycleOwner]'s lifecycle is at least at [minActiveState]
 * and cancels the coroutine when the lifecycle drops below [minActiveState].
 *
 * @param minActiveState The minimum [LifecycleState] required for [block] to execute. Must be at least [LifecycleState.CREATED].
 * @param block The suspendable lambda to execute while the lifecycle is at least at [minActiveState].
 */
suspend fun LifecycleOwner.repeatOnLifecycle(
    minActiveState: LifecycleState,
    block: suspend CoroutineScope.() -> Unit
) {
    lifecycle.repeatOnLifecycle(minActiveState, block)
}

/**
 * Flow operator that emits values from this [Flow] only when the [lifecycle] is at least at [minActiveState].
 *
 * When the [lifecycle] drops below [minActiveState], upstream flow collection is paused/cancelled.
 * When the [lifecycle] reaches [minActiveState] again, a new subscription to the upstream flow is established.
 * When the [lifecycle] reaches [LifecycleState.DESTROYED], the returned Flow is completed.
 *
 * @param lifecycle The [Lifecycle] to observe.
 * @param minActiveState The minimum [LifecycleState] required to collect emissions. Defaults to [LifecycleState.STARTED].
 */
fun <T> Flow<T>.flowWithLifecycle(
    lifecycle: Lifecycle,
    minActiveState: LifecycleState = LifecycleState.STARTED
): Flow<T> = callbackFlow {
    lifecycle.repeatOnLifecycle(minActiveState) {
        this@flowWithLifecycle.collect {
            send(it)
        }
    }
    close()
    awaitClose { /* Teardown handled by repeatOnLifecycle finally block */ }
}.buffer(Channel.BUFFERED)

/**
 * Flow operator that emits values from this [Flow] only when the [lifecycleOwner]'s lifecycle is at least at [minActiveState].
 *
 * @param lifecycleOwner The [LifecycleOwner] to observe.
 * @param minActiveState The minimum [LifecycleState] required to collect emissions. Defaults to [LifecycleState.STARTED].
 */
fun <T> Flow<T>.flowWithLifecycle(
    lifecycleOwner: LifecycleOwner,
    minActiveState: LifecycleState = LifecycleState.STARTED
): Flow<T> = flowWithLifecycle(lifecycleOwner.lifecycle, minActiveState)

/**
 * Collects emissions from this [Flow] only when the [lifecycle] is at least at [minActiveState].
 *
 * @param lifecycle The [Lifecycle] to observe.
 * @param minActiveState The minimum [LifecycleState] required to collect emissions. Defaults to [LifecycleState.STARTED].
 * @param action The suspend function to execute for each emitted value.
 */
suspend fun <T> Flow<T>.collectWithLifecycle(
    lifecycle: Lifecycle,
    minActiveState: LifecycleState = LifecycleState.STARTED,
    action: suspend (T) -> Unit
) {
    lifecycle.repeatOnLifecycle(minActiveState) {
        collect { value ->
            action(value)
        }
    }
}

/**
 * Collects emissions from this [Flow] only when the [lifecycleOwner]'s lifecycle is at least at [minActiveState].
 *
 * @param lifecycleOwner The [LifecycleOwner] to observe.
 * @param minActiveState The minimum [LifecycleState] required to collect emissions. Defaults to [LifecycleState.STARTED].
 * @param action The suspend function to execute for each emitted value.
 */
suspend fun <T> Flow<T>.collectWithLifecycle(
    lifecycleOwner: LifecycleOwner,
    minActiveState: LifecycleState = LifecycleState.STARTED,
    action: suspend (T) -> Unit
) {
    collectWithLifecycle(lifecycleOwner.lifecycle, minActiveState, action)
}
