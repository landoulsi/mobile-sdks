# `:storage`

Key-value persistence for Kotlin Multiplatform (Android + iOS): plaintext
settings, an encrypted store for secrets, and a small SQLDelight-backed offline
queue.

## What's here

| Type | File | Backing | Use for |
| --- | --- | --- | --- |
| `StorageManager` | `commonMain/.../StorageManager.kt` | `multiplatform-settings` (`SharedPreferences` / `NSUserDefaults`) | Non-sensitive settings — typed `get*`/`put*`, `remove`, `clear`. |
| `SecureStorage` | `commonMain/.../SecureStorage.kt` | `AndroidSecureStorage` (Jetpack Security `EncryptedSharedPreferences`) / `IosSecureStorage` (Keychain) | Auth tokens and other values that must not sit in plaintext. `getString` / `putString` / `remove`. |
| `OfflineQueue` | `commonMain/.../queue/OfflineQueue.kt` | SQLDelight (`LandoulsiDatabase`, schema `PingQueue.sq`) | FIFO durable queue: `enqueue`, `flush { payload -> Boolean }` (stops at the first `false`, keeping the rest in order). |

`StorageManager` and the platform storage modules are `@Inject`/`@Provides`
annotated and constructor-injectable.

## Usage

```kotlin
storage.putString("locale", "en-GB")
secure.putString("access_token", token)

queue.enqueue(payload, now)
val sent = queue.flush { p -> api.send(p).isSuccessful }
```

## Tests

```bash
./gradlew :storage:allTests
```
