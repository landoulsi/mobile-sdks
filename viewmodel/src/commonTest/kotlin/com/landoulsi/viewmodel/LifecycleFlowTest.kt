package com.landoulsi.viewmodel

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class LifecycleFlowTest {

    private class TestLifecycleOwner(
        initialState: LifecycleState = LifecycleState.INITIALIZED
    ) : LifecycleOwner {
        val registry = LifecycleRegistry(this, initialState)
        override val lifecycle: Lifecycle get() = registry
    }

    @Test
    fun testRepeatOnLifecycleRequiresValidMinActiveState() = runTest {
        val owner = TestLifecycleOwner(LifecycleState.CREATED)

        assertFailsWith<IllegalArgumentException> {
            owner.repeatOnLifecycle(LifecycleState.INITIALIZED) { }
        }

        assertFailsWith<IllegalArgumentException> {
            owner.repeatOnLifecycle(LifecycleState.DESTROYED) { }
        }
    }

    @Test
    fun testRepeatOnLifecycleReturnsImmediatelyWhenDestroyed() = runTest {
        val owner = TestLifecycleOwner(LifecycleState.DESTROYED)
        var executed = false

        owner.repeatOnLifecycle(LifecycleState.STARTED) {
            executed = true
        }

        assertEquals(false, executed)
    }

    @Test
    fun testRepeatOnLifecycleExecutesAndCancelsAcrossTransitions() = runTest {
        val owner = TestLifecycleOwner(LifecycleState.INITIALIZED)
        var executions = 0
        var cancellations = 0

        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            owner.repeatOnLifecycle(LifecycleState.STARTED) {
                executions++
                try {
                    awaitCancellation()
                } finally {
                    cancellations++
                }
            }
        }

        assertEquals(0, executions)

        // Move to STARTED -> should execute block
        owner.registry.currentState = LifecycleState.STARTED
        assertEquals(1, executions)
        assertEquals(0, cancellations)

        // Move to RESUMED -> should remain active
        owner.registry.currentState = LifecycleState.RESUMED
        assertEquals(1, executions)
        assertEquals(0, cancellations)

        // Move to CREATED -> drops below STARTED -> should cancel
        owner.registry.currentState = LifecycleState.CREATED
        assertEquals(1, executions)
        assertEquals(1, cancellations)

        // Move to STARTED again -> should re-execute
        owner.registry.currentState = LifecycleState.STARTED
        assertEquals(2, executions)
        assertEquals(1, cancellations)

        // Move to DESTROYED -> should cancel and complete repeatOnLifecycle
        owner.registry.currentState = LifecycleState.DESTROYED
        assertEquals(2, executions)
        assertEquals(2, cancellations)

        job.cancel()
    }

    @Test
    fun testFlowWithLifecycleEmitsOnlyWhileActive() = runTest {
        val owner = TestLifecycleOwner(LifecycleState.INITIALIZED)
        val source = MutableSharedFlow<Int>()
        val collected = mutableListOf<Int>()

        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            source.flowWithLifecycle(owner, LifecycleState.STARTED)
                .toList(collected)
        }

        // Before STARTED
        source.emit(1)
        assertEquals(emptyList(), collected)

        // Enter STARTED
        owner.registry.currentState = LifecycleState.STARTED
        source.emit(2)
        source.emit(3)
        assertEquals(listOf(2, 3), collected)

        // Drop to CREATED
        owner.registry.currentState = LifecycleState.CREATED
        source.emit(4)
        assertEquals(listOf(2, 3), collected)

        // Resume to STARTED
        owner.registry.currentState = LifecycleState.STARTED
        source.emit(5)
        assertEquals(listOf(2, 3, 5), collected)

        // Transition to DESTROYED closes the flow
        owner.registry.currentState = LifecycleState.DESTROYED
        assertTrue(job.isCompleted)

        job.cancel()
    }

    @Test
    fun testCollectWithLifecycleReceivesEmissions() = runTest {
        val owner = TestLifecycleOwner(LifecycleState.STARTED)
        val source = MutableSharedFlow<String>()
        val collected = mutableListOf<String>()

        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            source.collectWithLifecycle(owner, LifecycleState.STARTED) { value ->
                collected.add(value)
            }
        }

        source.emit("alpha")
        source.emit("beta")
        assertEquals(listOf("alpha", "beta"), collected)

        owner.registry.currentState = LifecycleState.DESTROYED
        job.cancel()
    }
}
