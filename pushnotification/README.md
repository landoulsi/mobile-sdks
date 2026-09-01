# `:pushnotification`

Push + local notifications for Kotlin Multiplatform. Android is backed by
Firebase Cloud Messaging; iOS by APNs / `UserNotifications`.

## What's here

`PushNotificationManager` (`commonMain/.../PushNotificationManager.kt`) is a façade
composed of four focused interfaces — depend on only the one you need:

| Interface | File | Covers |
| --- | --- | --- |
| `NotificationPermissionController` | `commonMain/.../NotificationPermissionController.kt` | `suspend requestPermission()` / `getPermissionStatus()` -> `GRANTED` / `DENIED` / `NOT_DETERMINED`. |
| `RemoteTokenController` | `commonMain/.../RemoteTokenController.kt` | Register/unregister for remote notifications, `getToken()`, `tokenFlow`, `onNewToken(...)`. |
| `LocalNotificationPresenter` | `commonMain/.../LocalNotificationPresenter.kt` | `showLocalNotification(...)`, `cancelNotification(id)`, `cancelAllNotifications()`. |
| `NotificationChannelManager` | `commonMain/.../NotificationChannelManager.kt` | Android channels; a no-op-style surface on iOS. |

Not every platform implements every capability (iOS has no Android-style
channels) — the split interfaces give a compile-time signal of what's supported.

## Getting an instance

`PushNotificationManagerFactory` is an `expect object` exposing `create()`.
On **Android** you must call `PushNotificationManagerFactory.initialize(...)`
first (see `PushNotificationManagerFactory.android.kt`) to supply the app config
(deep-link schemes, permission requester). iOS needs no initialization.

```kotlin
// Android: once, at startup
PushNotificationManagerFactory.initialize(applicationContext, /* config */)

val push = PushNotificationManagerFactory.create()
if (push.requestPermission() == NotificationPermissionController.PermissionStatus.GRANTED) {
    push.registerForRemoteNotifications()
}
push.tokenFlow.collect { token -> token?.let(::sendToBackend) }
```

## Tests

```bash
./gradlew :pushnotification:allTests
```
