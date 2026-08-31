package com.landoulsi.tutorial.dsl

import com.landoulsi.tutorial.model.IndicatorStyle
import com.landoulsi.tutorial.model.OnboardingFlow
import com.landoulsi.tutorial.model.PagerConfig
import com.landoulsi.tutorial.model.PointerStyle
import com.landoulsi.tutorial.model.SpotlightShape
import com.landoulsi.tutorial.model.SpotlightTarget
import com.landoulsi.tutorial.model.StepAction
import com.landoulsi.tutorial.model.StepCondition
import com.landoulsi.tutorial.model.StepConditionContext
import com.landoulsi.tutorial.model.StepConditions
import com.landoulsi.tutorial.model.TooltipPosition
import com.landoulsi.tutorial.model.Tutorial
import com.landoulsi.tutorial.model.TutorialPage
import com.landoulsi.tutorial.model.TutorialStep

@DslMarker
annotation class TutorialDsl

/**
 * Fluent builder for declaring [TutorialStep] definitions.
 */
@TutorialDsl
class TutorialStepBuilder(val id: String) {
    var title: String = ""
    var description: String = ""
    var target: SpotlightTarget? = null
    var tooltipPosition: TooltipPosition = TooltipPosition.AUTO
    var pointerStyle: PointerStyle = PointerStyle.PULSE_RING
    var action: StepAction = StepAction.Next
    var canSkip: Boolean = true

    private val conditions = mutableListOf<StepCondition>()
    private val metadata = mutableMapOf<String, String>()

    fun target(
        tag: String,
        shape: SpotlightShape = SpotlightShape.RoundedRectangle(),
        cutoutPaddingDp: Float = 8f
    ) {
        this.target = SpotlightTarget(tag = tag, shape = shape, cutoutPaddingDp = cutoutPaddingDp)
    }

    fun condition(condition: StepCondition) {
        conditions.add(condition)
    }

    fun minAppVersion(minVersionCode: Long) {
        condition(StepConditions.MinAppVersion(minVersionCode))
    }

    fun featureFlag(flagName: String, expectedValue: Boolean = true) {
        condition(StepConditions.FeatureFlag(flagName, expectedValue))
    }

    fun requiresStep(stepId: String) {
        condition(StepConditions.PrerequisiteStepCompleted(stepId))
    }

    fun customCondition(name: String = "CustomPredicate", predicate: (StepConditionContext) -> Boolean) {
        condition(StepConditions.CustomPredicate(name, predicate))
    }

    fun metadata(key: String, value: String) {
        metadata[key] = value
    }

    fun metadata(entries: Map<String, String>) {
        metadata.putAll(entries)
    }

    fun build(): TutorialStep {
        return TutorialStep(
            id = id,
            title = title,
            description = description,
            target = target,
            tooltipPosition = tooltipPosition,
            pointerStyle = pointerStyle,
            action = action,
            canSkip = canSkip,
            conditions = conditions.toList(),
            metadata = metadata.toMap()
        )
    }
}

/**
 * Fluent builder for declaring [Tutorial] flows.
 */
@TutorialDsl
class TutorialBuilder(val id: String, val version: Int = 1) {
    var title: String = ""
    var description: String? = null
    var targetGroup: String? = null

    private val stepBuilders = mutableListOf<TutorialStepBuilder>()
    private val metadata = mutableMapOf<String, String>()

    fun step(id: String, block: TutorialStepBuilder.() -> Unit = {}) {
        val builder = TutorialStepBuilder(id).apply(block)
        stepBuilders.add(builder)
    }

    fun metadata(key: String, value: String) {
        metadata[key] = value
    }

    fun metadata(entries: Map<String, String>) {
        metadata.putAll(entries)
    }

    fun build(): Tutorial {
        return Tutorial(
            id = id,
            title = title,
            version = version,
            description = description,
            targetGroup = targetGroup,
            steps = stepBuilders.map { it.build() },
            metadata = metadata.toMap()
        )
    }
}

/**
 * Entry point DSL function for constructing a [Tutorial].
 */
fun tutorial(id: String, version: Int = 1, block: TutorialBuilder.() -> Unit): Tutorial {
    return TutorialBuilder(id = id, version = version).apply(block).build()
}

/**
 * Fluent builder for declaring [TutorialPage] definitions in onboarding sequences.
 */
@TutorialDsl
class TutorialPageBuilder(val id: String) {
    var title: String = ""
    var description: String = ""
    var imageRes: String? = null
    var badge: String? = null
    var actionText: String? = null
    private val metadata = mutableMapOf<String, String>()

    fun metadata(key: String, value: String) {
        metadata[key] = value
    }

    fun metadata(entries: Map<String, String>) {
        metadata.putAll(entries)
    }

    fun build(): TutorialPage {
        return TutorialPage(
            id = id,
            title = title,
            description = description,
            imageRes = imageRes,
            badge = badge,
            actionText = actionText,
            metadata = metadata.toMap()
        )
    }
}

/**
 * Fluent builder for declaring [OnboardingFlow] sequences.
 */
@TutorialDsl
class OnboardingFlowBuilder(val id: String, val version: Int = 1) {
    var title: String = ""
    var description: String? = null
    var targetGroup: String? = null
    var pagerConfig: PagerConfig = PagerConfig.Default
    var indicatorStyle: IndicatorStyle = IndicatorStyle.ExpandingPill()

    private val pageBuilders = mutableListOf<TutorialPageBuilder>()
    private val metadata = mutableMapOf<String, String>()

    fun page(id: String, block: TutorialPageBuilder.() -> Unit = {}) {
        val builder = TutorialPageBuilder(id).apply(block)
        pageBuilders.add(builder)
    }

    fun pagerConfig(config: PagerConfig) {
        this.pagerConfig = config
    }

    fun indicatorStyle(style: IndicatorStyle) {
        this.indicatorStyle = style
    }

    fun metadata(key: String, value: String) {
        metadata[key] = value
    }

    fun metadata(entries: Map<String, String>) {
        metadata.putAll(entries)
    }

    fun build(): OnboardingFlow {
        return OnboardingFlow(
            id = id,
            title = title,
            version = version,
            description = description,
            targetGroup = targetGroup,
            pages = pageBuilders.map { it.build() },
            pagerConfig = pagerConfig,
            indicatorStyle = indicatorStyle,
            metadata = metadata.toMap()
        )
    }
}

/**
 * Entry point DSL function for constructing an [OnboardingFlow].
 */
fun onboardingFlow(id: String, version: Int = 1, block: OnboardingFlowBuilder.() -> Unit): OnboardingFlow {
    return OnboardingFlowBuilder(id = id, version = version).apply(block).build()
}

