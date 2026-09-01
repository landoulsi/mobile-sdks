# `:screenshot`

Capture a screenshot on a trigger, attach metadata, and upload it to a backend.
Kotlin Multiplatform (Android + iOS).

## What's here

| Type | File | Role |
| --- | --- | --- |
| `ScreenshotManager` | `commonMain/.../ScreenshotManager.kt` | Entry point. Wires triggers -> capture -> metadata -> upload. |
| `ScreenshotConfig` | `commonMain/.../config/ScreenshotConfig.kt` | `isEnabled`, capture options, server target, metadata options, trigger config. |
| `ScreenshotCapturer` | `commonMain` interface + `AndroidScreenshotCapturer` / `IosScreenshotCapturer` | Platform screen capture. |
| `MetadataCollector` | `commonMain` + platform impls | Device/app context for the payload. |
| `ScreenshotUploader` / `KtorScreenshotUploader` | `commonMain/.../network/` | Multipart upload. |
| Trigger handlers | `commonMain/.../trigger/` | Manual, push-payload, and app-event triggers; `registerTrigger(...)` for custom ones. |

## Usage

```kotlin
val manager = ScreenshotManager(config, capturer)
manager.setOnUploadResultListener { result -> /* Result<UploadResponse> */ }

// explicit
val result = manager.captureAndUpload(triggerType = ScreenshotTriggerType.MANUAL)

// or driven by inbound signals
manager.handlePushNotification(remoteMessage.data)   // returns true if it matched
manager.onAppEvent("bug_report_opened")
```

## Tests

```bash
./gradlew :screenshot:allTests
```
