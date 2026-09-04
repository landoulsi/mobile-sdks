package com.landoulsi.demo.ui

import com.landoulsi.viewmodel.DefaultLifecycleObserver
import com.landoulsi.viewmodel.Lifecycle
import com.landoulsi.viewmodel.LifecycleOwner
import com.landoulsi.viewmodel.LifecycleState
import com.landoulsi.viewmodel.ViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Immutable UI state for the ViewModel SDK showcase screen.
 */
data class ViewModelDemoUiState(
    val counter: Int = 0,
    val backgroundJobActive: Boolean = false,
    val backgroundJobTicks: Int = 0,
    val lifecycleEvents: List<String> = emptyList(),
)

/**
 * Showcase ViewModel demonstrating the :viewmodel SDK capabilities:
 *  - [StateFlow] observation: a counter stream updated every second.
 *  - Coroutine auto-cancellation: a toggleable background job running in [viewModelScope]
 *    that is cancelled when the ViewModel is cleared.
 *  - Lifecycle event logging: a [DefaultLifecycleObserver] attached to the host [Lifecycle]
 *    that records every state transition.
 */
class ViewModelDemoViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ViewModelDemoUiState())
    val uiState: StateFlow<ViewModelDemoUiState> = _uiState.asStateFlow()

    private var backgroundJob: Job? = null
    private var attachedLifecycle: Lifecycle? = null
    private var lifecycleObserver: DefaultLifecycleObserver? = null

    init {
        viewModelScope.launch {
            while (true) {
                delay(COUNTER_INTERVAL_MS)
                _uiState.update { it.copy(counter = it.counter + 1) }
            }
        }
    }

    /**
     * Attaches a [DefaultLifecycleObserver] to the host [Lifecycle] and records every
     * state transition into [ViewModelDemoUiState.lifecycleEvents].
     *
     * The observer is immediately brought up to the lifecycle's current state, so the
     * event log starts with the states the host has already reached.
     */
    fun attachLifecycle(lifecycle: Lifecycle) {
        if (attachedLifecycle === lifecycle) return
        detachLifecycle()

        val observer = object : DefaultLifecycleObserver {
            override fun onStateChanged(owner: LifecycleOwner, state: LifecycleState) {
                recordLifecycleEvent("State: $state")
            }

            override fun onCreate(owner: LifecycleOwner) = recordLifecycleEvent("onCreate")

            override fun onStart(owner: LifecycleOwner) = recordLifecycleEvent("onStart")

            override fun onResume(owner: LifecycleOwner) = recordLifecycleEvent("onResume")

            override fun onPause(owner: LifecycleOwner) = recordLifecycleEvent("onPause")

            override fun onStop(owner: LifecycleOwner) = recordLifecycleEvent("onStop")

            override fun onDestroy(owner: LifecycleOwner) = recordLifecycleEvent("onDestroy")
        }

        attachedLifecycle = lifecycle
        lifecycleObserver = observer
        lifecycle.addObserver(observer)
    }

    /**
     * Removes the lifecycle observer previously registered by [attachLifecycle].
     */
    fun detachLifecycle() {
        attachedLifecycle?.removeObserver(lifecycleObserver ?: return)
        attachedLifecycle = null
        lifecycleObserver = null
    }

    /**
     * Starts or stops a long-running background job in [viewModelScope].
     *
     * The job is automatically cancelled when the ViewModel is cleared (e.g. when the
     * user leaves the screen), demonstrating structured-concurrency auto-cancellation.
     */
    fun toggleBackgroundJob() {
        val activeJob = backgroundJob
        if (activeJob?.isActive == true) {
            activeJob.cancel()
            backgroundJob = null
            _uiState.update { it.copy(backgroundJobActive = false) }
        } else {
            backgroundJob = viewModelScope.launch {
                _uiState.update { it.copy(backgroundJobActive = true) }
                while (isActive) {
                    delay(BACKGROUND_JOB_INTERVAL_MS)
                    _uiState.update { it.copy(backgroundJobTicks = it.backgroundJobTicks + 1) }
                }
            }
        }
    }

    private fun recordLifecycleEvent(event: String) {
        val timestamp = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date())
        _uiState.update { state ->
            state.copy(
                lifecycleEvents = (state.lifecycleEvents + "[$timestamp] $event").takeLast(MAX_EVENTS),
            )
        }
    }

    private companion object {
        const val COUNTER_INTERVAL_MS = 1_000L
        const val BACKGROUND_JOB_INTERVAL_MS = 500L
        const val MAX_EVENTS = 40
    }
}