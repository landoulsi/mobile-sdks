# `:tutorial`

Onboarding flows and in-app coach marks (spotlight tooltips) for Compose
Multiplatform. Targets Android, iOS, and JVM.

## What's here

| Area | Files | Role |
| --- | --- | --- |
| DSL | `commonMain/.../dsl/TutorialBuilder.kt` | `tutorial { step("id") { ... } }` for spotlight walkthroughs; `onboardingFlow { page("id") { ... } }` for paged intros. |
| Models | `commonMain/.../model/TutorialModels.kt` | `Tutorial`, `TutorialStep`, `SpotlightTarget`, `OnboardingFlow`, `TutorialPage`, `StepConditions` (min app version, feature flag, prerequisite step, custom predicate), progress types. |
| Tracker | `commonMain/.../tracker/TutorialTracker.kt` | Observable progress (`progressState`), completion/skip state across sessions. |
| Storage | `commonMain/.../storage/TutorialStorage.kt` | Persistence of progress (backed by `multiplatform-settings`). |
| UI | `commonMain/.../ui/` | `TutorialPager`, `PageIndicator` composables. |

## Usage

```kotlin
val flow = tutorial(id = "home_intro", version = 1) {
    title = "Getting around"
    step("search") {
        title = "Search"
        description = "Find anything from here"
        target(tag = "home_search_bar")
        featureFlag("search_v2")
    }
}
```

Register composable targets with the matching `tag`, then drive the flow through
a `TutorialTracker`.

## Tests

```bash
./gradlew :tutorial:allTests
```
