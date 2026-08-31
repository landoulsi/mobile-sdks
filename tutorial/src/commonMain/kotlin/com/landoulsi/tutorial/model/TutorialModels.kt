package com.landoulsi.tutorial.model

import kotlinx.serialization.Serializable

/**
 * Represents the completion status of a tutorial.
 */
@Serializable
enum class CompletionStatus {
    NOT_STARTED,
    IN_PROGRESS,
    COMPLETED,
    SKIPPED,
    DISMISSED;

    val isFinished: Boolean
        get() = this == COMPLETED || this == SKIPPED || this == DISMISSED
}

/**
 * Shape used to highlight UI target elements during spotlighting.
 */
@Serializable
sealed interface SpotlightShape {
    @Serializable
    data object Circle : SpotlightShape

    @Serializable
    data class RoundedRectangle(val cornerRadiusDp: Float = 8f) : SpotlightShape

    @Serializable
    data object Oval : SpotlightShape

    @Serializable
    data object Rectangle : SpotlightShape
}

/**
 * Style of pointer/indicator displayed near the spotlighted element.
 */
@Serializable
enum class PointerStyle {
    HAND,
    ARROW,
    PULSE_RING,
    NONE
}

/**
 * Tooltip positioning relative to the spotlight target.
 */
@Serializable
enum class TooltipPosition {
    TOP,
    BOTTOM,
    START,
    END,
    AUTO
}

/**
 * Action triggered upon completing or interacting with a tutorial step.
 */
@Serializable
sealed interface StepAction {
    @Serializable
    data object Next : StepAction

    @Serializable
    data object Complete : StepAction

    @Serializable
    data class Custom(val actionId: String) : StepAction
}

/**
 * Visual spotlight target specification.
 */
@Serializable
data class SpotlightTarget(
    val tag: String,
    val shape: SpotlightShape = SpotlightShape.RoundedRectangle(),
    val cutoutPaddingDp: Float = 8f
)

/**
 * Context provided when evaluating step conditions.
 */
data class StepConditionContext(
    val appVersionCode: Long? = null,
    val completedStepIds: Set<String> = emptySet(),
    val featureFlags: Map<String, Boolean> = emptyMap(),
    val customAttributes: Map<String, Any> = emptyMap()
) {
    companion object {
        val EMPTY = StepConditionContext()
    }
}

/**
 * Condition determining whether a tutorial step should be displayed.
 */
interface StepCondition {
    fun evaluate(context: StepConditionContext = StepConditionContext.EMPTY): Boolean
}

/**
 * Common built-in step conditions.
 */
object StepConditions {
    data object Always : StepCondition {
        override fun evaluate(context: StepConditionContext): Boolean = true
    }

    data class MinAppVersion(val minVersionCode: Long) : StepCondition {
        override fun evaluate(context: StepConditionContext): Boolean {
            val current = context.appVersionCode ?: return true
            return current >= minVersionCode
        }
    }

    data class FeatureFlag(
        val flagName: String,
        val expectedValue: Boolean = true
    ) : StepCondition {
        override fun evaluate(context: StepConditionContext): Boolean {
            return context.featureFlags[flagName] == expectedValue
        }
    }

    data class PrerequisiteStepCompleted(val prerequisiteStepId: String) : StepCondition {
        override fun evaluate(context: StepConditionContext): Boolean {
            return context.completedStepIds.contains(prerequisiteStepId)
        }
    }

    class CustomPredicate(
        val name: String = "CustomPredicate",
        private val predicate: (StepConditionContext) -> Boolean
    ) : StepCondition {
        override fun evaluate(context: StepConditionContext): Boolean = predicate(context)
    }
}

/**
 * Domain model representing a single step within a tutorial flow.
 */
data class TutorialStep(
    val id: String,
    val title: String,
    val description: String,
    val target: SpotlightTarget? = null,
    val tooltipPosition: TooltipPosition = TooltipPosition.AUTO,
    val pointerStyle: PointerStyle = PointerStyle.PULSE_RING,
    val action: StepAction = StepAction.Next,
    val canSkip: Boolean = true,
    val conditions: List<StepCondition> = emptyList(),
    val metadata: Map<String, String> = emptyMap()
) {
    init {
        require(id.isNotBlank()) { "TutorialStep ID cannot be blank" }
        require(title.isNotBlank()) { "TutorialStep title cannot be blank" }
    }

    fun shouldShow(context: StepConditionContext = StepConditionContext.EMPTY): Boolean {
        return conditions.all { it.evaluate(context) }
    }
}

/**
 * Domain entity representing an entire tutorial tour / flow.
 */
data class Tutorial(
    val id: String,
    val title: String,
    val version: Int = 1,
    val description: String? = null,
    val targetGroup: String? = null,
    val steps: List<TutorialStep>,
    val metadata: Map<String, String> = emptyMap()
) {
    init {
        require(id.isNotBlank()) { "Tutorial ID cannot be blank" }
        require(title.isNotBlank()) { "Tutorial title cannot be blank" }
        require(version >= 1) { "Tutorial version must be >= 1" }
        require(steps.isNotEmpty()) { "Tutorial must contain at least one step" }
        val duplicateIds = steps.groupBy { it.id }.filter { it.value.size > 1 }.keys
        require(duplicateIds.isEmpty()) { "Tutorial steps must have unique IDs, duplicates found: $duplicateIds" }
    }

    val stepCount: Int get() = steps.size

    fun getStep(index: Int): TutorialStep? = steps.getOrNull(index)

    fun getStepById(stepId: String): TutorialStep? = steps.firstOrNull { it.id == stepId }

    fun indexOfStep(stepId: String): Int = steps.indexOfFirst { it.id == stepId }
}

/**
 * Persistent state of a user's progress through a tutorial.
 */
@Serializable
data class TutorialProgress(
    val tutorialId: String,
    val status: CompletionStatus = CompletionStatus.NOT_STARTED,
    val currentStepIndex: Int = 0,
    val completedStepIds: Set<String> = emptySet(),
    val firstSeenEpochMs: Long? = null,
    val lastUpdatedEpochMs: Long? = null,
    val impressionCount: Int = 0,
    val version: Int = 1
) {
    val isCompleted: Boolean get() = status == CompletionStatus.COMPLETED
    val isDismissed: Boolean get() = status == CompletionStatus.DISMISSED
    val isSkipped: Boolean get() = status == CompletionStatus.SKIPPED
    val isInProgress: Boolean get() = status == CompletionStatus.IN_PROGRESS
    val isNotStarted: Boolean get() = status == CompletionStatus.NOT_STARTED
}
