package com.landoulsi.viewmodel

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class LifecycleTest {

    private class TestLifecycleOwner : LifecycleOwner {
        override val lifecycle: LifecycleRegistry = LifecycleRegistry(this)
    }

    @Test
    fun testLifecycleStateIsAtLeast() {
        assertTrue(LifecycleState.RESUMED.isAtLeast(LifecycleState.RESUMED))
        assertTrue(LifecycleState.RESUMED.isAtLeast(LifecycleState.STARTED))
        assertTrue(LifecycleState.RESUMED.isAtLeast(LifecycleState.CREATED))
        assertTrue(LifecycleState.RESUMED.isAtLeast(LifecycleState.INITIALIZED))
        assertTrue(LifecycleState.RESUMED.isAtLeast(LifecycleState.DESTROYED))

        assertFalse(LifecycleState.STARTED.isAtLeast(LifecycleState.RESUMED))
        assertTrue(LifecycleState.STARTED.isAtLeast(LifecycleState.STARTED))
        assertTrue(LifecycleState.STARTED.isAtLeast(LifecycleState.CREATED))
        assertTrue(LifecycleState.STARTED.isAtLeast(LifecycleState.INITIALIZED))

        assertFalse(LifecycleState.CREATED.isAtLeast(LifecycleState.STARTED))
        assertTrue(LifecycleState.CREATED.isAtLeast(LifecycleState.CREATED))
        assertTrue(LifecycleState.CREATED.isAtLeast(LifecycleState.INITIALIZED))

        assertFalse(LifecycleState.INITIALIZED.isAtLeast(LifecycleState.CREATED))
        assertTrue(LifecycleState.INITIALIZED.isAtLeast(LifecycleState.INITIALIZED))
        assertTrue(LifecycleState.INITIALIZED.isAtLeast(LifecycleState.DESTROYED))

        assertFalse(LifecycleState.DESTROYED.isAtLeast(LifecycleState.INITIALIZED))
        assertFalse(LifecycleState.DESTROYED.isAtLeast(LifecycleState.CREATED))
        assertTrue(LifecycleState.DESTROYED.isAtLeast(LifecycleState.DESTROYED))
    }

    @Test
    fun testObserverReceivesLinearTransitionsUpward() {
        val owner = TestLifecycleOwner()
        val transitions = mutableListOf<LifecycleState>()

        owner.lifecycle.addObserver { _, state ->
            transitions.add(state)
        }

        owner.lifecycle.currentState = LifecycleState.RESUMED

        assertEquals(
            listOf(LifecycleState.CREATED, LifecycleState.STARTED, LifecycleState.RESUMED),
            transitions,
            "Advancing to RESUMED should linearly transition through CREATED and STARTED"
        )
        assertEquals(LifecycleState.RESUMED, owner.lifecycle.currentState)
    }

    @Test
    fun testDefaultLifecycleObserverCallbacksUpwardAndDownward() {
        val owner = TestLifecycleOwner()
        val events = mutableListOf<String>()

        val observer = object : DefaultLifecycleObserver {
            override fun onCreate(owner: LifecycleOwner) {
                events.add("onCreate")
            }

            override fun onStart(owner: LifecycleOwner) {
                events.add("onStart")
            }

            override fun onResume(owner: LifecycleOwner) {
                events.add("onResume")
            }

            override fun onPause(owner: LifecycleOwner) {
                events.add("onPause")
            }

            override fun onStop(owner: LifecycleOwner) {
                events.add("onStop")
            }

            override fun onDestroy(owner: LifecycleOwner) {
                events.add("onDestroy")
            }
        }

        owner.lifecycle.addObserver(observer)

        owner.lifecycle.currentState = LifecycleState.RESUMED
        assertEquals(listOf("onCreate", "onStart", "onResume"), events)

        events.clear()
        owner.lifecycle.currentState = LifecycleState.DESTROYED
        assertEquals(
            listOf("onPause", "onStop", "onDestroy"),
            events,
            "Moving to DESTROYED from RESUMED should step through onPause, onStop, and onDestroy"
        )
    }

    @Test
    fun testObserverCatchUpWhenAddedToActiveLifecycle() {
        val owner = TestLifecycleOwner()
        owner.lifecycle.currentState = LifecycleState.STARTED

        val events = mutableListOf<String>()
        val observer = object : DefaultLifecycleObserver {
            override fun onCreate(owner: LifecycleOwner) {
                events.add("onCreate")
            }

            override fun onStart(owner: LifecycleOwner) {
                events.add("onStart")
            }

            override fun onResume(owner: LifecycleOwner) {
                events.add("onResume")
            }
        }

        owner.lifecycle.addObserver(observer)

        assertEquals(
            listOf("onCreate", "onStart"),
            events,
            "Newly registered observer should catch up to current state (STARTED)"
        )
    }

    @Test
    fun testRemoveObserverStopsEventDelivery() {
        val owner = TestLifecycleOwner()
        val transitions = mutableListOf<LifecycleState>()

        val observer = LifecycleObserver { _, state ->
            transitions.add(state)
        }

        owner.lifecycle.addObserver(observer)
        assertTrue(owner.lifecycle.hasObservers())
        assertEquals(1, owner.lifecycle.observerCount)

        owner.lifecycle.currentState = LifecycleState.CREATED
        assertEquals(listOf(LifecycleState.CREATED), transitions)

        owner.lifecycle.removeObserver(observer)
        assertFalse(owner.lifecycle.hasObservers())
        assertEquals(0, owner.lifecycle.observerCount)

        owner.lifecycle.currentState = LifecycleState.RESUMED
        assertEquals(
            listOf(LifecycleState.CREATED),
            transitions,
            "Removed observer should not receive subsequent transitions"
        )
    }

    @Test
    fun testIdempotentStateTransitions() {
        val owner = TestLifecycleOwner()
        var callCount = 0

        owner.lifecycle.addObserver { _, _ ->
            callCount++
        }

        owner.lifecycle.currentState = LifecycleState.CREATED
        assertEquals(1, callCount)

        owner.lifecycle.currentState = LifecycleState.CREATED
        assertEquals(1, callCount, "Setting same state should be idempotent")
    }

    @Test
    fun testDestroyedIsTerminalState() {
        val owner = TestLifecycleOwner()
        var callCount = 0

        owner.lifecycle.addObserver { _, _ ->
            callCount++
        }

        owner.lifecycle.currentState = LifecycleState.DESTROYED
        val countAfterDestroy = callCount

        owner.lifecycle.currentState = LifecycleState.RESUMED
        assertEquals(LifecycleState.DESTROYED, owner.lifecycle.currentState)
        assertEquals(countAfterDestroy, callCount, "Transitions out of DESTROYED must be ignored")
    }

    @Test
    fun testReentrantStateChangeHandledGracefully() {
        val owner = TestLifecycleOwner()
        val statesEncountered = mutableListOf<LifecycleState>()

        val reentrantObserver = object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                (owner.lifecycle as LifecycleRegistry).currentState = LifecycleState.DESTROYED
            }

            override fun onStateChanged(owner: LifecycleOwner, state: LifecycleState) {
                statesEncountered.add(state)
            }
        }

        owner.lifecycle.addObserver(reentrantObserver)
        owner.lifecycle.currentState = LifecycleState.RESUMED

        assertEquals(LifecycleState.DESTROYED, owner.lifecycle.currentState)
        assertEquals(
            listOf(
                LifecycleState.CREATED,
                LifecycleState.STARTED,
                LifecycleState.RESUMED,
                LifecycleState.STARTED,
                LifecycleState.CREATED,
                LifecycleState.DESTROYED
            ),
            statesEncountered,
            "Re-entrant transition to DESTROYED during onResume should unwind linearly"
        )
    }

    @Test
    fun testStateFlowEmitsLifecycleTransitions() = runTest {
        val owner = TestLifecycleOwner()
        val emitted = mutableListOf<LifecycleState>()

        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            owner.lifecycle.stateFlow.toList(emitted)
        }

        owner.lifecycle.currentState = LifecycleState.CREATED
        owner.lifecycle.currentState = LifecycleState.STARTED
        owner.lifecycle.currentState = LifecycleState.RESUMED
        owner.lifecycle.currentState = LifecycleState.DESTROYED

        job.cancel()

        assertEquals(
            listOf(
                LifecycleState.INITIALIZED,
                LifecycleState.CREATED,
                LifecycleState.STARTED,
                LifecycleState.RESUMED,
                LifecycleState.STARTED,
                LifecycleState.CREATED,
                LifecycleState.DESTROYED
            ),
            emitted,
            "StateFlow should reflect all linear lifecycle state transitions"
        )
    }
}
