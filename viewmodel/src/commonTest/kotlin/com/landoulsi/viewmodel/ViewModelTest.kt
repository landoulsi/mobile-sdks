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
}
