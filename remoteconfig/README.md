# `:remoteconfig`

Remote configuration / feature flags for Kotlin Multiplatform. Backed by Firebase
Remote Config on Android and iOS; an in-memory implementation is provided for
tests and previews.

## API

`RemoteConfigManager` (`commonMain/.../RemoteConfigManager.kt`) — every call
returns `Result<...>`:

- **Lifecycle** — `fetch(minimumFetchIntervalInSeconds?)`, `activate()`,
  `fetchAndActivate()`, `setDefaults(map)`.
- **Typed getters** — `getString` / `getBoolean` / `getInt` / `getLong` /
  `getDouble`, each returning the value or `null`.
- **Local overrides** — `putString` / `putBoolean` / ... store an in-memory
  override (developer/debug menus); `clearLocalOverrides()` drops them.

## Implementations

| Class | Source set |
| --- | --- |
| `FirebaseRemoteConfigManager` | `androidMain` / `iosMain` |
| `InMemoryRemoteConfigManager` | `commonMain` — no Firebase dependency |
| `RemoteConfigSettings` | `commonMain` — fetch-timeout / interval config |

## Usage

```kotlin
config.setDefaults(mapOf("checkout_v2" to false))
config.fetchAndActivate()
val enabled = config.getBoolean("checkout_v2").getOrNull() ?: false
```

## Tests

```bash
./gradlew :remoteconfig:allTests
```
