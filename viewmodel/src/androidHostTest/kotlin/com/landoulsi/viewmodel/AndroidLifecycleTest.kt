package com.landoulsi.viewmodel

import androidx.lifecycle.Lifecycle as AndroidxLifecycle
import androidx.lifecycle.LifecycleOwner as AndroidxLifecycleOwner
import androidx.lifecycle.LifecycleRegistry as AndroidxLifecycleRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AndroidLifecycleTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class TestAndroidLifecycleOwner : AndroidxLifecycleOwner {
        val registry = AndroidxLifecycleRegistry(this)
        override val lifecycle: AndroidxLifecycle get() = registry
    }

    private class TestKmpLifecycleOwner(
        initialState: LifecycleState = LifecycleState.INITIALIZED,
    ) : LifecycleOwner {
        val registry = LifecycleRegistry(this, initialState)
        override val lifecycle: Lifecycle get() = registry
    }

    @Test
    fun stateMapping_isAccurateInBothDirections() {
        assertEquals(LifecycleState.DESTROYED, AndroidxLifecycle.State.DESTROYED.toKmpState())
        assertEquals(LifecycleState.INITIALIZED, AndroidxLifecycle.State.INITIALIZED.toKmpState())
        assertEquals(LifecycleState.CREATED, AndroidxLifecycle.State.CREATED.toKmpState())
        assertEquals(LifecycleState.STARTED, AndroidxLifecycle.State.STARTED.toKmpState())
        assertEquals(LifecycleState.RESUMED, AndroidxLifecycle.State.RESUMED.toKmpState())

        assertEquals(AndroidxLifecycle.State.DESTROYED, LifecycleState.DESTROYED.toAndroidxState())
        assertEquals(AndroidxLifecycle.State.INITIALIZED, LifecycleState.INITIALIZED.toAndroidxState())
        assertEquals(AndroidxLifecycle.State.CREATED, LifecycleState.CREATED.toAndroidxState())
        assertEquals(AndroidxLifecycle.State.STARTED, LifecycleState.STARTED.toAndroidxState())
        assertEquals(AndroidxLifecycle.State.RESUMED, LifecycleState.RESUMED.toAndroidxState())
    }

    @Test
    fun eventMapping_mapsToExpectedKmpState() {
        assertEquals(LifecycleState.CREATED, AndroidxLifecycle.Event.ON_CREATE.toTargetLifecycleState())
        assertEquals(LifecycleState.STARTED, AndroidxLifecycle.Event.ON_START.toTargetLifecycleState())
        assertEquals(LifecycleState.RESUMED, AndroidxLifecycle.Event.ON_RESUME.toTargetLifecycleState())
        assertEquals(LifecycleState.STARTED, AndroidxLifecycle.Event.ON_PAUSE.toTargetLifecycleState())
        assertEquals(LifecycleState.CREATED, AndroidxLifecycle.Event.ON_STOP.toTargetLifecycleState())
        assertEquals(LifecycleState.DESTROYED, AndroidxLifecycle.Event.ON_DESTROY.toTargetLifecycleState())

        // toKmpState alias behaves identically
        assertEquals(LifecycleState.CREATED, AndroidxLifecycle.Event.ON_CREATE.toKmpState())
        assertEquals(LifecycleState.STARTED, AndroidxLifecycle.Event.ON_START.toKmpState())
        assertEquals(LifecycleState.RESUMED, AndroidxLifecycle.Event.ON_RESUME.toKmpState())
        assertEquals(LifecycleState.STARTED, AndroidxLifecycle.Event.ON_PAUSE.toKmpState())
        assertEquals(LifecycleState.CREATED, AndroidxLifecycle.Event.ON_STOP.toKmpState())
        assertEquals(LifecycleState.DESTROYED, AndroidxLifecycle.Event.ON_DESTROY.toKmpState())

        // ON_ANY must throw IllegalArgumentException as it is a dispatch filter
        assertFailsWith<IllegalArgumentException> {
            AndroidxLifecycle.Event.ON_ANY.toTargetLifecycleState()
        }
        assertFailsWith<IllegalArgumentException> {
            AndroidxLifecycle.Event.ON_ANY.toKmpState()
        }
    }

    @Test
    fun androidLifecycleBridge_reflectsStateAndDispatchesLinearCallbacks() {
        val androidOwner = TestAndroidLifecycleOwner()
        val bridge = AndroidLifecycleBridge(androidOwner.lifecycle)

        assertEquals(LifecycleState.INITIALIZED, bridge.currentState)
        assertEquals(LifecycleState.INITIALIZED, bridge.stateFlow.value)

        val events = mutableListOf<String>()
        val observer = object : DefaultLifecycleObserver {
            override fun onCreate(owner: LifecycleOwner) { events.add("onCreate") }
            override fun onStart(owner: LifecycleOwner) { events.add("onStart") }
            override fun onResume(owner: LifecycleOwner) { events.add("onResume") }
            override fun onPause(owner: LifecycleOwner) { events.add("onPause") }
            override fun onStop(owner: LifecycleOwner) { events.add("onStop") }
            override fun onDestroy(owner: LifecycleOwner) { events.add("onDestroy") }
            override fun onStateChanged(owner: LifecycleOwner, state: LifecycleState) {
                events.add("state:$state")
            }
        }

        bridge.addObserver(observer)
        assertTrue(bridge.hasObservers())
        assertEquals(1, bridge.observerCount)

        // Progress forward
        androidOwner.registry.handleLifecycleEvent(AndroidxLifecycle.Event.ON_CREATE)
        assertEquals(LifecycleState.CREATED, bridge.currentState)

        androidOwner.registry.handleLifecycleEvent(AndroidxLifecycle.Event.ON_START)
        assertEquals(LifecycleState.STARTED, bridge.currentState)

        androidOwner.registry.handleLifecycleEvent(AndroidxLifecycle.Event.ON_RESUME)
        assertEquals(LifecycleState.RESUMED, bridge.currentState)

        // Step backward
        androidOwner.registry.handleLifecycleEvent(AndroidxLifecycle.Event.ON_PAUSE)
        assertEquals(LifecycleState.STARTED, bridge.currentState)

        androidOwner.registry.handleLifecycleEvent(AndroidxLifecycle.Event.ON_STOP)
        assertEquals(LifecycleState.CREATED, bridge.currentState)

        androidOwner.registry.handleLifecycleEvent(AndroidxLifecycle.Event.ON_DESTROY)
        assertEquals(LifecycleState.DESTROYED, bridge.currentState)

        val expectedEvents = listOf(
            "state:CREATED", "onCreate",
            "state:STARTED", "onStart",
            "state:RESUMED", "onResume",
            "state:STARTED", "onPause",
            "state:CREATED", "onStop",
            "state:DESTROYED", "onDestroy"
        )
        assertEquals(expectedEvents, events)
    }

    @Test
    fun androidLifecycleBridge_catchesUpLateSubscriber() {
        val androidOwner = TestAndroidLifecycleOwner()
        androidOwner.registry.handleLifecycleEvent(AndroidxLifecycle.Event.ON_CREATE)
        androidOwner.registry.handleLifecycleEvent(AndroidxLifecycle.Event.ON_START)
        androidOwner.registry.handleLifecycleEvent(AndroidxLifecycle.Event.ON_RESUME)

        val bridge = AndroidLifecycleBridge(androidOwner.lifecycle)
        val events = mutableListOf<String>()

        val observer = object : DefaultLifecycleObserver {
            override fun onCreate(owner: LifecycleOwner) { events.add("onCreate") }
            override fun onStart(owner: LifecycleOwner) { events.add("onStart") }
            override fun onResume(owner: LifecycleOwner) { events.add("onResume") }
            override fun onStateChanged(owner: LifecycleOwner, state: LifecycleState) {
                events.add("state:$state")
            }
        }

        bridge.addObserver(observer)

        val expectedEvents = listOf(
            "state:CREATED", "onCreate",
            "state:STARTED", "onStart",
            "state:RESUMED", "onResume"
        )
        assertEquals(expectedEvents, events)
    }

    @Test
    fun androidLifecycleBridge_createdMidLifecycle_emitsMonotonicallyWithoutDownwardWobbles() {
        val androidOwner = TestAndroidLifecycleOwner()
        androidOwner.registry.handleLifecycleEvent(AndroidxLifecycle.Event.ON_CREATE)
        androidOwner.registry.handleLifecycleEvent(AndroidxLifecycle.Event.ON_START)
        androidOwner.registry.handleLifecycleEvent(AndroidxLifecycle.Event.ON_RESUME)

        // Bridge created when AndroidX is already RESUMED
        val bridge = AndroidLifecycleBridge(androidOwner.lifecycle)
        assertEquals(LifecycleState.RESUMED, bridge.currentState)
        assertEquals(LifecycleState.RESUMED, bridge.stateFlow.value)

        val observedStates = mutableListOf<LifecycleState>()
        bridge.addObserver(object : LifecycleObserver {
            override fun onStateChanged(owner: LifecycleOwner, state: LifecycleState) {
                observedStates.add(state)
            }
        })

        // Observer should have caught up monotonically without any fake pause/stop
        assertEquals(
            listOf(LifecycleState.CREATED, LifecycleState.STARTED, LifecycleState.RESUMED),
            observedStates
        )
    }

    @Test
    fun androidLifecycleBridge_removeObserver_updatesObserverCount() {
        val androidOwner = TestAndroidLifecycleOwner()
        val bridge = AndroidLifecycleBridge(androidOwner.lifecycle)

        val observer = object : LifecycleObserver {
            override fun onStateChanged(owner: LifecycleOwner, state: LifecycleState) {}
        }

        assertFalse(bridge.hasObservers())
        assertEquals(0, bridge.observerCount)

        bridge.addObserver(observer)
        assertTrue(bridge.hasObservers())
        assertEquals(1, bridge.observerCount)

        bridge.removeObserver(observer)
        assertFalse(bridge.hasObservers())
        assertEquals(0, bridge.observerCount)
    }

    @Test
    fun asKmpLifecycleOwner_createsValidLifecycleOwner() {
        val androidOwner = TestAndroidLifecycleOwner()
        val kmpOwner = androidOwner.asKmpLifecycleOwner()

        assertEquals(LifecycleState.INITIALIZED, kmpOwner.lifecycle.currentState)

        var createdCalled = false
        kmpOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onCreate(owner: LifecycleOwner) {
                createdCalled = true
            }
        })

        androidOwner.registry.handleLifecycleEvent(AndroidxLifecycle.Event.ON_CREATE)
        assertTrue(createdCalled)
        assertEquals(LifecycleState.CREATED, kmpOwner.lifecycle.currentState)
    }

    @Test
    fun asKmpLifecycle_createsValidLifecycle() {
        val androidOwner = TestAndroidLifecycleOwner()
        val kmpLifecycle = androidOwner.lifecycle.asKmpLifecycle()

        assertEquals(LifecycleState.INITIALIZED, kmpLifecycle.currentState)

        var resumedCalled = false
        kmpLifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                resumedCalled = true
            }
        })

        androidOwner.registry.handleLifecycleEvent(AndroidxLifecycle.Event.ON_CREATE)
        androidOwner.registry.handleLifecycleEvent(AndroidxLifecycle.Event.ON_START)
        androidOwner.registry.handleLifecycleEvent(AndroidxLifecycle.Event.ON_RESUME)

        assertTrue(resumedCalled)
        assertEquals(LifecycleState.RESUMED, kmpLifecycle.currentState)
    }

    @Test
    fun viewModel_bindToLifecycle_cancelsRunningCoroutineOnDestroy() {
        val androidOwner = TestAndroidLifecycleOwner()
        androidOwner.registry.handleLifecycleEvent(AndroidxLifecycle.Event.ON_CREATE)
        androidOwner.registry.handleLifecycleEvent(AndroidxLifecycle.Event.ON_START)
        androidOwner.registry.handleLifecycleEvent(AndroidxLifecycle.Event.ON_RESUME)

        var cleared = false
        val viewModel = object : ViewModel() {
            override fun onCleared() {
                cleared = true
                super.onCleared()
            }
        }

        viewModel.bindToLifecycle(androidOwner)
        assertFalse(cleared)

        // Launch a long-running coroutine in viewModelScope
        var iterations = 0
        val job = viewModel.viewModelScope.launch {
            while (isActive) {
                iterations++
                delay(100)
            }
        }

        testDispatcher.scheduler.advanceTimeBy(250)
        assertTrue(iterations > 0)
        assertTrue(job.isActive)
        assertFalse(job.isCancelled)
        assertTrue(viewModel.viewModelScope.isActive)

        // Trigger destroy
        androidOwner.registry.handleLifecycleEvent(AndroidxLifecycle.Event.ON_DESTROY)
        assertTrue(cleared)
        assertTrue(job.isCancelled)
        assertFalse(job.isActive)
        assertFalse(viewModel.viewModelScope.isActive)
    }

    @Test
    fun viewModel_bindToAndroidxLifecycle_cancelsRunningCoroutineOnDestroy() {
        val androidOwner = TestAndroidLifecycleOwner()
        androidOwner.registry.handleLifecycleEvent(AndroidxLifecycle.Event.ON_CREATE)

        var cleared = false
        val viewModel = object : ViewModel() {
            override fun onCleared() {
                cleared = true
                super.onCleared()
            }
        }

        viewModel.bindToLifecycle(androidOwner.lifecycle)
        assertFalse(cleared)

        val job = viewModel.viewModelScope.launch {
            while (isActive) {
                delay(100)
            }
        }

        testDispatcher.scheduler.advanceTimeBy(150)
        assertTrue(job.isActive)

        androidOwner.registry.handleLifecycleEvent(AndroidxLifecycle.Event.ON_DESTROY)
        assertTrue(cleared)
        assertTrue(job.isCancelled)
        assertFalse(viewModel.viewModelScope.isActive)
    }

    @Test
    fun viewModel_bindToKmpLifecycleOwner_cancelsRunningCoroutineOnDestroy() {
        val kmpOwner = TestKmpLifecycleOwner(LifecycleState.RESUMED)

        var cleared = false
        val viewModel = object : ViewModel() {
            override fun onCleared() {
                cleared = true
                super.onCleared()
            }
        }

        viewModel.bindToLifecycle(kmpOwner)
        assertFalse(cleared)

        val job = viewModel.viewModelScope.launch {
            while (isActive) {
                delay(100)
            }
        }

        testDispatcher.scheduler.advanceTimeBy(150)
        assertTrue(job.isActive)

        kmpOwner.registry.handleLifecycleEvent(LifecycleState.DESTROYED)
        assertTrue(cleared)
        assertTrue(job.isCancelled)
        assertFalse(viewModel.viewModelScope.isActive)
    }

    @Test
    fun viewModel_bindToKmpLifecycle_cancelsRunningCoroutineOnDestroy() {
        val kmpOwner = TestKmpLifecycleOwner(LifecycleState.RESUMED)

        var cleared = false
        val viewModel = object : ViewModel() {
            override fun onCleared() {
                cleared = true
                super.onCleared()
            }
        }

        viewModel.bindToLifecycle(kmpOwner.lifecycle)
        assertFalse(cleared)

        val job = viewModel.viewModelScope.launch {
            while (isActive) {
                delay(100)
            }
        }

        testDispatcher.scheduler.advanceTimeBy(150)
        assertTrue(job.isActive)

        kmpOwner.registry.handleLifecycleEvent(LifecycleState.DESTROYED)
        assertTrue(cleared)
        assertTrue(job.isCancelled)
        assertFalse(viewModel.viewModelScope.isActive)
    }

    @Test
    fun viewModel_bindToLifecycle_clearsImmediatelyIfAlreadyDestroyed() {
        val androidOwner = TestAndroidLifecycleOwner()
        androidOwner.registry.handleLifecycleEvent(AndroidxLifecycle.Event.ON_DESTROY)

        var cleared = false
        val viewModel = object : ViewModel() {
            override fun onCleared() {
                cleared = true
                super.onCleared()
            }
        }

        viewModel.bindToLifecycle(androidOwner)
        assertTrue(cleared)
        assertFalse(viewModel.viewModelScope.isActive)
    }

    @Test
    fun viewModel_bindToKmpLifecycle_clearsImmediatelyIfAlreadyDestroyed() {
        val kmpOwner = TestKmpLifecycleOwner(LifecycleState.DESTROYED)

        var cleared = false
        val viewModel = object : ViewModel() {
            override fun onCleared() {
                cleared = true
                super.onCleared()
            }
        }

        viewModel.bindToLifecycle(kmpOwner.lifecycle)
        assertTrue(cleared)
        assertFalse(viewModel.viewModelScope.isActive)
    }

    @Test
    fun viewModel_bindToLifecycle_idempotentWhenCalledMultipleTimes() {
        val androidOwner = TestAndroidLifecycleOwner()
        androidOwner.registry.handleLifecycleEvent(AndroidxLifecycle.Event.ON_CREATE)

        var clearCount = 0
        val viewModel = object : ViewModel() {
            override fun onCleared() {
                clearCount++
                super.onCleared()
            }
        }

        viewModel.bindToLifecycle(androidOwner)
        viewModel.bindToLifecycle(androidOwner)

        androidOwner.registry.handleLifecycleEvent(AndroidxLifecycle.Event.ON_DESTROY)
        assertEquals(1, clearCount)
    }
}

