package com.landoulsi.viewmodel

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ViewModelTest {

    private class TestViewModel(scope: CoroutineScope? = null) :
        ViewModel(scope ?: CoroutineScope(SupervisorJob() + Dispatchers.Default)) {
        var onClearedCallCount = 0
            private set

        override fun onCleared() {
            super.onCleared()
            onClearedCallCount++
        }
    }

    @Test
    fun testCoroutinesExecuteInViewModelScope() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val viewModel = TestViewModel(CoroutineScope(SupervisorJob() + testDispatcher))

        var executed = false
        viewModel.viewModelScope.launch {
            executed = true
        }

        advanceUntilIdle()
        assertTrue(executed, "Coroutine launched in viewModelScope should execute")
    }

    @Test
    fun testClearCancelsActiveJobsInViewModelScope() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val viewModel = TestViewModel(CoroutineScope(SupervisorJob() + testDispatcher))

        var jobStarted = false
        var jobCancelled = false

        viewModel.viewModelScope.launch {
            jobStarted = true
            try {
                delay(10_000)
            } catch (e: CancellationException) {
                jobCancelled = true
                throw e
            }
        }

        testScheduler.advanceTimeBy(100)
        assertTrue(jobStarted, "Job should have started")
        assertTrue(viewModel.viewModelScope.isActive, "Scope should initially be active")

        viewModel.clear()

        advanceUntilIdle()
        assertFalse(viewModel.viewModelScope.isActive, "Scope should be cancelled after clear()")
        assertTrue(jobCancelled, "Active job should have caught CancellationException")
    }

    @Test
    fun testClearIsIdempotentAndCallsOnClearedOnce() = runTest {
        val viewModel = TestViewModel()

        assertEquals(0, viewModel.onClearedCallCount)

        viewModel.clear()
        assertEquals(1, viewModel.onClearedCallCount, "First clear() call should invoke onCleared()")

        viewModel.clear()
        assertEquals(1, viewModel.onClearedCallCount, "Subsequent clear() calls should be idempotent")

        viewModel.clear()
        assertEquals(1, viewModel.onClearedCallCount, "Subsequent clear() calls should be idempotent")
    }

    private class TestLifecycleOwner(
        initialState: LifecycleState = LifecycleState.INITIALIZED,
    ) : LifecycleOwner {
        val registry = LifecycleRegistry(this, initialState)
        override val lifecycle: Lifecycle get() = registry
    }

    @Test
    fun testBindToLifecycleOwnerCancelsScopeOnDestroyed() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val viewModel = TestViewModel(CoroutineScope(SupervisorJob() + testDispatcher))
        val owner = TestLifecycleOwner(LifecycleState.RESUMED)

        viewModel.bindToLifecycle(owner)
        assertEquals(0, viewModel.onClearedCallCount)

        owner.registry.currentState = LifecycleState.DESTROYED
        assertEquals(1, viewModel.onClearedCallCount)
        assertFalse(viewModel.viewModelScope.isActive)
    }

    @Test
    fun testBindToLifecycleCancelsScopeOnDestroyed() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val viewModel = TestViewModel(CoroutineScope(SupervisorJob() + testDispatcher))
        val owner = TestLifecycleOwner(LifecycleState.RESUMED)

        viewModel.bindToLifecycle(owner.lifecycle)
        assertEquals(0, viewModel.onClearedCallCount)

        owner.registry.currentState = LifecycleState.DESTROYED
        assertEquals(1, viewModel.onClearedCallCount)
        assertFalse(viewModel.viewModelScope.isActive)
    }

    @Test
    fun testBindToLifecycleWhenAlreadyDestroyedClearsImmediately() = runTest {
        val viewModel = TestViewModel()
        val owner = TestLifecycleOwner(LifecycleState.DESTROYED)

        viewModel.bindToLifecycle(owner)
        assertEquals(1, viewModel.onClearedCallCount)
        assertFalse(viewModel.viewModelScope.isActive)
    }

    @Test
    fun testBindToLifecycleMultipleTimesIsIdempotent() = runTest {
        val viewModel = TestViewModel()
        val owner = TestLifecycleOwner(LifecycleState.CREATED)

        viewModel.bindToLifecycle(owner)
        viewModel.bindToLifecycle(owner)

        owner.registry.currentState = LifecycleState.DESTROYED
        assertEquals(1, viewModel.onClearedCallCount)
    }
}

