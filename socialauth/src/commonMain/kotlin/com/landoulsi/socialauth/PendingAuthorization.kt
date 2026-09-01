package com.landoulsi.socialauth

import kotlinx.coroutines.CompletableDeferred
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi

/**
 * The suspend/resume state machine for the **Android** [RedirectAuthorizer]: one
 * outstanding browser round-trip at a time, resumed by a redirect delivered from the
 * host's redirect Activity, a cancel, or a launch failure. iOS uses
 * `ASWebAuthenticationSession`'s own completion callback and does not need this.
 *
 * The expected CSRF `state` travels with the round-trip and the state check is fused
 * into [deliver] so a concurrent [begin] can't cause one round's redirect to resolve
 * another round. Pure Kotlin so it is unit-tested in common code.
 */
@OptIn(ExperimentalAtomicApi::class)
internal class PendingAuthorization {

    internal class AuthorizationRound(
        val state: String,
        val deferred: CompletableDeferred<AuthorizationResult> = CompletableDeferred(),
    )

    private val current = AtomicReference<AuthorizationRound?>(null)

    /**
     * Starts a new round-trip with the given expected [state], abandoning any
     * previous one as [AuthorizationResult.Cancelled].
     */
    fun begin(state: String): AuthorizationRound {
        val newRound = AuthorizationRound(state)
        current.exchange(newRound)?.takeIf { it.deferred.isActive }
            ?.deferred?.complete(AuthorizationResult.Cancelled)
        return newRound
    }

    /** The `state` the outstanding round-trip expects back, or null if none is pending. */
    fun expectedState(): String? = current.load()?.state

    /**
     * Atomically claims the outstanding round-trip **iff** its expected state equals
     * [receivedState] (the `state` echoed back on the redirect), then resolves it with [result].
     * @return true if a matching round was claimed and resolved.
     */
    fun deliver(receivedState: String, result: AuthorizationResult): Boolean {
        while (true) {
            val round = current.load() ?: return false
            if (round.state != receivedState) return false
            if (current.compareAndSet(round, null)) {
                return round.deferred.complete(result)
            }
        }
    }

    /** Resolves the outstanding round-trip, whatever its state, with [AuthorizationResult.Cancelled]. */
    fun cancelCurrent(): Boolean {
        val round = current.exchange(null) ?: return false
        return round.deferred.complete(AuthorizationResult.Cancelled)
    }

    /** Clears [round] as the outstanding one only if it still is (atomic compare-and-set). */
    fun clear(round: AuthorizationRound) {
        current.compareAndSet(round, null)
    }
}
