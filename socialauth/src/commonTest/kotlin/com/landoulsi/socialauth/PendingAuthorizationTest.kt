package com.landoulsi.socialauth

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PendingAuthorizationTest {

    @Test
    fun deliverResolvesTheOutstandingAwaitWhenStateMatches() = runTest {
        val pending = PendingAuthorization()
        val round = pending.begin("st")

        assertEquals("st", pending.expectedState())
        assertTrue(pending.deliver("st", AuthorizationResult.Success("code", "st")))

        val result = assertIs<AuthorizationResult.Success>(round.deferred.await())
        assertEquals("code", result.code)
        assertNull(pending.expectedState(), "no round-trip is pending once delivered")
    }

    @Test
    fun deliverWithAWrongStateIsIgnored() = runTest {
        val pending = PendingAuthorization()
        val round = pending.begin("st")

        assertFalse(pending.deliver("other", AuthorizationResult.Success("c", "other")))
        assertTrue(round.deferred.isActive, "the real round-trip is untouched")
        assertEquals("st", pending.expectedState())
    }

    @Test
    fun deliverWithNothingPendingReturnsFalse() {
        assertFalse(PendingAuthorization().deliver("x", AuthorizationResult.Cancelled))
    }

    @Test
    fun secondDeliverIsANoOp() = runTest {
        val pending = PendingAuthorization()
        pending.begin("st")
        assertTrue(pending.deliver("st", AuthorizationResult.Cancelled))
        assertFalse(pending.deliver("st", AuthorizationResult.Success("x", null)))
    }

    @Test
    fun cancelCurrentResolvesRegardlessOfState() = runTest {
        val pending = PendingAuthorization()
        val round = pending.begin("st")
        assertTrue(pending.cancelCurrent())
        assertIs<AuthorizationResult.Cancelled>(round.deferred.await())
        assertFalse(pending.cancelCurrent(), "nothing left to cancel")
    }

    @Test
    fun beginAbandonsThePriorRoundTripAsCancelled() = runTest {
        val pending = PendingAuthorization()
        val first = pending.begin("a")
        pending.begin("b")
        assertIs<AuthorizationResult.Cancelled>(first.deferred.await())
        assertEquals("b", pending.expectedState())
    }

    @Test
    fun clearForAStaleRoundDoesNotDropTheCurrentRoundTrip() = runTest {
        val pending = PendingAuthorization()
        val stale = pending.begin("a")
        val current = pending.begin("b") // supersedes `stale`

        pending.clear(stale) // must be a no-op — `stale` is no longer current

        assertEquals("b", pending.expectedState())
        assertTrue(pending.deliver("b", AuthorizationResult.Cancelled), "current round-trip still deliverable")
        assertIs<AuthorizationResult.Cancelled>(stale.deferred.await())
        assertIs<AuthorizationResult.Cancelled>(current.deferred.await())
    }
}
