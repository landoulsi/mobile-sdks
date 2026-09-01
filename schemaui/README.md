# `:schemaui`

Server-driven UI: parse a JSON schema into an IR tree, render it, and route
user actions back to host-registered handlers.

## Pipeline

```
JSON  ──SchemaUIParser──▶  SchemaNode  ──SchemaToIR──▶  UINode (IR)  ──renderer──▶  UI
                                                           │
                                          StateStore  ◀────┘  (reactive form state)
```

## What's here

| Type | File | Role |
| --- | --- | --- |
| `SchemaUIEngine` | `commonMain/.../SchemaUIEngine.kt` | The one class hosts touch: `parseFromString(json): Result<UINode>`, `registerAction` / `registerActionWithState` / `triggerAction`, and the `stateStore`. |
| `SchemaUIParser` | `commonMain/.../SchemaUIParser.kt` | JSON -> `SchemaNode`. |
| `SchemaNode.toIR()` | `commonMain/.../ir/SchemaToIR.kt` | `SchemaNode` -> `UINode`. |
| `StateStore` | `commonMain/.../state/StateStore.kt` | Reactive `key -> String` form state. |
| Compose renderer | `androidMain/.../compose/` | `SchemaUIRenderer` + node renderers. **Android/JVM only** (`composeCompiler` targets `androidJvm`). |
| `SchemaUIKit` | `iosMain/.../SchemaUIKit.kt` | iOS entry point consumed from Swift. |

## Usage (Android)

```kotlin
val engine = SchemaUIEngine()
engine.registerActionWithState("submit") { state -> submit(state["email"]) }
val root = engine.parseFromString(json).getOrThrow()
// pass engine + root to the SchemaUI() composable
```

## Tests

```bash
./gradlew :schemaui:allTests
```
