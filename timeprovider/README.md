# `:timeprovider`

A tiny abstraction over the system clock so time-dependent code stays testable.
Kotlin Multiplatform — Android, iOS, and JVM.

## API

`commonMain/.../TimeProvider.kt`:

| Symbol | Role |
| --- | --- |
| `interface TimeProvider` | `currentTimeMillis(): Long` — epoch milliseconds (UTC). |
| `expect fun systemEpochMillis(): Long` | Low-level platform clock; `actual` in `TimeProvider.android.kt` / `TimeProvider.ios.kt` / `TimeProvider.jvm.kt`. |
| `class SystemTimeProvider` | Production impl, backed by `systemEpochMillis()`. |
| `class FakeTimeProvider(initialMillis)` | Test impl: `setTime(...)`, `advanceBy(millis)`, `advanceBy(Duration)`. |

## Usage

```kotlin
class TokenCache(private val time: TimeProvider) {
    fun isExpired(expiryMillis: Long) = time.currentTimeMillis() >= expiryMillis
}

// production
TokenCache(SystemTimeProvider())

// test
val clock = FakeTimeProvider(initialMillis = 1_000L)
val cache = TokenCache(clock)
clock.advanceBy(60.seconds)
```

> Not to be confused with `:location`'s own `TimeProvider`, which formats an
> ISO-8601 string via `currentTimestamp()`.

## Tests

```bash
./gradlew :timeprovider:allTests
```
