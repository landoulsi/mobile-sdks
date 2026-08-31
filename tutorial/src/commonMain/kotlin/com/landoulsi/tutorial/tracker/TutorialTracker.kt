package com.landoulsi.tutorial.tracker

import com.landoulsi.tutorial.model.CompletionStatus
import com.landoulsi.tutorial.model.StepConditionContext
import com.landoulsi.tutorial.model.Tutorial
import com.landoulsi.tutorial.model.TutorialProgress
import com.landoulsi.tutorial.storage.TutorialStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Contract for querying and manipulating tutorial progress state across user sessions.
 */
interface TutorialTracker {
    /**
     * Observable reactive stream of all tutorial progress records.
     */
    val progressState: StateFlow<Map<String, TutorialProgress>>

    /**
     * Returns the cached progress for a specific tutorial, or null if never tracked.
     */
    fun getProgress(tutorialId: String): TutorialProgress?

    /**
     * Returns an observable flow of the progress for a specific tutorial.
     */
    fun getProgressFlow(tutorialId: String): Flow<TutorialProgress?>

    /**
     * Determines whether a tutorial should be displayed based on completion status,
     * impression caps, version bumps, and step conditions.
     */
    fun shouldShowTutorial(
        tutorial: Tutorial,
        currentAppVersion: Long? = null,
        maxImpressions: Int = 1,
        conditionContext: StepConditionContext = StepConditionContext.EMPTY
    ): Boolean

    /**
     * Starts or resumes a tutorial flow, incrementing impression count and updating timestamps.
     */
    suspend fun startTutorial(tutorial: Tutorial): TutorialProgress

    /**
     * Marks a specific step within a tutorial as completed and advances the step index.
     */
    suspend fun completeStep(
        tutorialId: String,
        stepId: String,
        nextStepIndex: Int? = null
    ): TutorialProgress

    /**
     * Marks the tutorial as fully completed.
     */
    suspend fun completeTutorial(tutorialId: String): TutorialProgress

    /**
     * Marks the tutorial as skipped by the user.
     */
    suspend fun skipTutorial(tutorialId: String): TutorialProgress

    /**
     * Marks the tutorial as dismissed.
     */
    suspend fun dismissTutorial(tutorialId: String): TutorialProgress

    /**
     * Resets progress for a specific tutorial ID.
     */
    suspend fun resetProgress(tutorialId: String)

    /**
     * Resets progress for all tutorials.
     */
    suspend fun resetAllProgress()

    /**
     * Initializes the tracker by populating memory state from the underlying storage.
     */
    suspend fun initialize()
}

/**
 * Clock interface providing timestamps for testability and cross-platform compatibility.
 */
fun interface TimeProvider {
    fun currentTimeMillis(): Long

    companion object {
        val SYSTEM: TimeProvider = TimeProvider { com.landoulsi.tutorial.currentTimeMillis() }
    }
}

/**
 * Default implementation of [TutorialTracker] managing state transitions and persisting through [TutorialStorage].
 */
class DefaultTutorialTracker(
    private val storage: TutorialStorage,
    private val timeProvider: TimeProvider = TimeProvider.SYSTEM,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) : TutorialTracker {

    private val mutex = Mutex()
    private val _progressState = MutableStateFlow<Map<String, TutorialProgress>>(emptyMap())
    override val progressState: StateFlow<Map<String, TutorialProgress>> = _progressState.asStateFlow()

    private var isInitialized = false

    init {
        coroutineScope.launch {
            initialize()
        }
    }

    override suspend fun initialize() {
        mutex.withLock {
            if (!isInitialized) {
                val stored = storage.getAllProgress()
                _progressState.value = stored
                isInitialized = true
            }
        }
    }

    private suspend fun ensureInitialized() {
        if (!isInitialized) {
            val stored = storage.getAllProgress()
            _progressState.value = stored
            isInitialized = true
        }
    }

    override fun getProgress(tutorialId: String): TutorialProgress? {
        return _progressState.value[tutorialId]
    }

    override fun getProgressFlow(tutorialId: String): Flow<TutorialProgress?> {
        return progressState.map { it[tutorialId] }.distinctUntilChanged()
    }

    override fun shouldShowTutorial(
        tutorial: Tutorial,
        currentAppVersion: Long?,
        maxImpressions: Int,
        conditionContext: StepConditionContext
    ): Boolean {
        val progress = getProgress(tutorial.id)
        val effectiveContext = if (currentAppVersion != null && conditionContext.appVersionCode == null) {
            conditionContext.copy(appVersionCode = currentAppVersion)
        } else {
            conditionContext
        }

        if (progress == null || progress.status == CompletionStatus.NOT_STARTED) {
            return tutorial.steps.any { it.shouldShow(effectiveContext) }
        }

        // Version bump re-trigger
        if (tutorial.version > progress.version) {
            return tutorial.steps.any { it.shouldShow(effectiveContext) }
        }

        // Finished in current version
        if (progress.status.isFinished) {
            return false
        }

        // Impression cap
        if (progress.impressionCount >= maxImpressions) {
            return false
        }

        return true
    }

    override suspend fun startTutorial(tutorial: Tutorial): TutorialProgress = mutex.withLock {
        ensureInitialized()
        val now = timeProvider.currentTimeMillis()
        val existing = _progressState.value[tutorial.id]
        val isVersionBump = existing != null && tutorial.version > existing.version

        val newProgress = if (existing == null || isVersionBump) {
            TutorialProgress(
                tutorialId = tutorial.id,
                status = CompletionStatus.IN_PROGRESS,
                currentStepIndex = 0,
                completedStepIds = emptySet(),
                firstSeenEpochMs = existing?.firstSeenEpochMs ?: now,
                lastUpdatedEpochMs = now,
                impressionCount = (existing?.impressionCount ?: 0) + 1,
                version = tutorial.version
            )
        } else {
            existing.copy(
                status = CompletionStatus.IN_PROGRESS,
                impressionCount = existing.impressionCount + 1,
                lastUpdatedEpochMs = now
            )
        }

        storage.saveProgress(newProgress)
        _progressState.update { it + (tutorial.id to newProgress) }
        newProgress
    }

    override suspend fun completeStep(
        tutorialId: String,
        stepId: String,
        nextStepIndex: Int?
    ): TutorialProgress = mutex.withLock {
        ensureInitialized()
        val now = timeProvider.currentTimeMillis()
        val current = _progressState.value[tutorialId] ?: TutorialProgress(
            tutorialId = tutorialId,
            status = CompletionStatus.IN_PROGRESS,
            firstSeenEpochMs = now,
            lastUpdatedEpochMs = now,
            impressionCount = 1
        )

        val updatedCompleted = current.completedStepIds + stepId
        val targetIndex = nextStepIndex ?: (current.currentStepIndex + 1)
        val updated = current.copy(
            completedStepIds = updatedCompleted,
            currentStepIndex = targetIndex,
            status = CompletionStatus.IN_PROGRESS,
            lastUpdatedEpochMs = now
        )

        storage.saveProgress(updated)
        _progressState.update { it + (tutorialId to updated) }
        updated
    }

    override suspend fun completeTutorial(tutorialId: String): TutorialProgress = mutex.withLock {
        ensureInitialized()
        val now = timeProvider.currentTimeMillis()
        val current = _progressState.value[tutorialId] ?: TutorialProgress(
            tutorialId = tutorialId,
            firstSeenEpochMs = now,
            impressionCount = 1
        )

        val updated = current.copy(
            status = CompletionStatus.COMPLETED,
            lastUpdatedEpochMs = now
        )

        storage.saveProgress(updated)
        _progressState.update { it + (tutorialId to updated) }
        updated
    }

    override suspend fun skipTutorial(tutorialId: String): TutorialProgress = mutex.withLock {
        ensureInitialized()
        val now = timeProvider.currentTimeMillis()
        val current = _progressState.value[tutorialId] ?: TutorialProgress(
            tutorialId = tutorialId,
            firstSeenEpochMs = now,
            impressionCount = 1
        )

        val updated = current.copy(
            status = CompletionStatus.SKIPPED,
            lastUpdatedEpochMs = now
        )

        storage.saveProgress(updated)
        _progressState.update { it + (tutorialId to updated) }
        updated
    }

    override suspend fun dismissTutorial(tutorialId: String): TutorialProgress = mutex.withLock {
        ensureInitialized()
        val now = timeProvider.currentTimeMillis()
        val current = _progressState.value[tutorialId] ?: TutorialProgress(
            tutorialId = tutorialId,
            firstSeenEpochMs = now,
            impressionCount = 1
        )

        val updated = current.copy(
            status = CompletionStatus.DISMISSED,
            lastUpdatedEpochMs = now
        )

        storage.saveProgress(updated)
        _progressState.update { it + (tutorialId to updated) }
        updated
    }

    override suspend fun resetProgress(tutorialId: String): Unit = mutex.withLock {
        ensureInitialized()
        storage.clearProgress(tutorialId)
        _progressState.update { it - tutorialId }
    }

    override suspend fun resetAllProgress(): Unit = mutex.withLock {
        ensureInitialized()
        storage.clearAll()
        _progressState.value = emptyMap()
    }
}
