# `:analytics`

Vendor-agnostic analytics event tracking for Kotlin Multiplatform (Android + iOS).

## What's here

| Type | File | Role |
| --- | --- | --- |
| `EventTracker` | `commonMain/.../EventTracker.kt` | The contract: `trackEvent`, `identifyUser`, `setUserProperty`. Fire-and-forget, non-suspend. |
| `Event` / `AnalyticsValue` | `commonMain/.../Event.kt` | Immutable event value type. Names are validated `snake_case`; property values are restricted to string/long/double/boolean so PII/secrets can't ride in on arbitrary objects. |
| `EventMapper` | `commonMain/.../EventMapper.kt` | Pure `Event` -> flat map with sensitive-key redaction (`email`, `card_`, `token`, ...). |
| `CompositeEventTracker` | `commonMain/.../CompositeEventTracker.kt` | Broadcasts every call to a list of delegate trackers; one failing backend never blocks the others. |
| `LoggingEventTracker` | `commonMain/.../LoggingEventTracker.kt` | Backend-free tracker that prints events; for dev/tests/demos. |
| `FirebaseEventTracker` | `androidMain` (takes a `FirebaseAnalytics`) / `iosMain` | Firebase Analytics adapter. Each platform provides its own class — there is no `expect`/`actual`. |

## Usage

```kotlin
// commonMain: build a tracker from the platform pieces passed in
fun analyticsTracker(firebase: EventTracker): EventTracker =
    CompositeEventTracker(listOf(LoggingEventTracker(), firebase))

tracker.identifyUser("a1b2c3-opaque-id")   // opaque id only, never PII
tracker.trackEvent(
    Event(
        eventName = "checkout_started",
        properties = mapOf("cart_size" to AnalyticsValue.Long(3)),
    ),
)
```

## Notes

- The user id is owned by the tracker (`identifyUser`) and attached at delivery time — it is never a per-event field.
- `trackEvent` swallows failures by contract; callers can rely on fire-and-forget semantics.

## Tests

```bash
./gradlew :analytics:allTests
```
