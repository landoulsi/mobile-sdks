# `:survey`

In-app surveys (NPS, feedback forms) for Kotlin Multiplatform. A survey is
authored as JSON, rendered through the `:schemaui` engine, and the collected
answers are POSTed back to a server.

## Flow

```
JSON / URL ──▶ SurveyController.loadFromJson / loadFromServer
                     │  parse (SurveyParser) → SurveyDefinition
                     ▼
              SurveySchemaBuilder → :schemaui UINode tree ──▶ Survey() (Android) / SurveyKit (iOS)
                     │  answers flow into engine.stateStore; tree rebuilt on every change
                     ▼
              SurveyController.submit → validate → SurveyClient POSTs SurveyResponse
```

`SurveyController.state` is a `StateFlow<SurveyState>`: `Idle` → `Loading` →
`Ready` (carries the render tree, validation errors, submit progress) →
`Submitted` / `LoadError`.

## What's here

| Type | File | Role |
| --- | --- | --- |
| `SurveyController` | `commonMain/.../SurveyController.kt` | Orchestrates load → render → collect → submit. Owns a `:schemaui` `SchemaUIEngine`. |
| `SurveyState` | `commonMain/.../SurveyState.kt` | Sealed observable state. |
| `SurveyParser` | `commonMain/.../SurveyParser.kt` | Lenient JSON → `SurveyDefinition`; unknown question types degrade to `UnknownQuestion`. |
| `SurveySchemaBuilder` | `commonMain/.../SurveySchemaBuilder.kt` | Pure `SurveyDefinition` + answers → `:schemaui` `UINode` tree. |
| `SurveyClient` / `SurveyClientFactory` | `commonMain/.../SurveyClient.kt` | Ktor transport: `fetchDefinition(url)`, `submit(url, response)`. Platform engine (OkHttp / Darwin) via `expect fun defaultSurveyHttpClient()`. |
| `model/SurveyDefinition.kt`, `model/SurveyResponse.kt` | `commonMain` | Wire formats (`kotlinx.serialization`). Question types: `shortText`, `longText`, `singleChoice`, `multiChoice`, `rating`, `boolean`. |
| `Survey(...)` composable | `androidMain/.../compose/SurveyView.kt` | Renders a `SurveyController` on Android. |
| `SurveyKit` | `iosMain/.../SurveyKit.kt` | iOS entry point, consumed from Swift. |

Depends on `:schemaui` (rendering), `:timeprovider` (`SurveyResponse.submittedAtMillis`),
and `:logger`.

## Usage

```kotlin
val controller = SurveyController()
controller.loadFromServer("https://api.example.com/surveys/nps-2026-q1")

controller.state.collect { state ->
    when (state) {
        is SurveyState.Ready     -> render(state.node, controller.engine)
        is SurveyState.Submitted -> showThanks()
        is SurveyState.LoadError -> showError(state.message)
        else                     -> showSpinner()
    }
}
// controller.close() when done
```
