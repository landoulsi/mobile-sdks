# Payment SDK — Project Context Map

_Last updated: 2026-08-19. Factual orientation only; do not add instructions here._

---

## Repository layout

```
payment-sdk/
├── ROADMAP.md               # Goals, scope notes, design direction (prioritizing Google Pay)
├── .orchestrator/           # Orchestrator state (not source)
│   ├── completed.log        # One line per completed goal
│   ├── current_goal.txt     # Goal currently being implemented
│   └── PROJECT_CONTEXT.md   # This file
└── mobile/                  # Android + KMP Gradle project
    ├── settings.gradle.kts  # Registers :app and :shared; rootProject.name = "payment-sdk"
    ├── build.gradle.kts     # Top-level; applies plugins with apply false
    ├── gradle/
    │   └── libs.versions.toml  # Version catalog
    ├── gradle.properties
    ├── gradlew / gradlew.bat
    ├── shared/              # Kotlin Multiplatform shared module (:shared)
    │   ├── build.gradle.kts # KMP library build config (androidTarget, iosTargets)
    │   └── src/
    │       ├── commonMain/kotlin/com/landoulsi/payment/shared/  # Platform.kt
    │       ├── androidMain/kotlin/com/landoulsi/payment/shared/ # Platform.android.kt
    │       └── iosMain/kotlin/com/landoulsi/payment/shared/     # Platform.ios.kt
    └── app/                 # Android application module (:app)
        ├── build.gradle.kts
        └── src/
            ├── main/java/com/landoulsi/payment/
            │   ├── MainActivity.kt         # Entry point; Compose setContent
            │   └── ui/theme/
            │       ├── Color.kt            # Placeholder color tokens
            │       ├── Theme.kt            # PaymentsdkTheme
            │       └── Type.kt             # Default typography
            ├── androidTest/                # Scaffolded test directory
            └── test/                       # Scaffolded test directory
```

---

## Key versions (mobile/gradle/libs.versions.toml)

| Artifact | Version |
|---|---|
| AGP | 9.2.1 |
| Kotlin | 2.2.10 |
| `androidx.core:core-ktx` (alias: `androidx-core-ktx`) | 1.19.0 |
| Compose BOM | 2026.02.01 |
| `lifecycle-runtime-ktx` | 2.10.0 |
| `activity-compose` | 1.13.0 |

---

## SDK Configuration

- `compileSdk`: 37 (aligned with AndroidX Core 1.19.0 requirement)
- `minSdk`: 24
- `targetSdk`: 36

---

## Package / namespace

- Application ID: `com.landoulsi.payment`
- App namespace: `com.landoulsi.payment`
- Shared module namespace: `com.landoulsi.payment.shared`

---

## Tech stack

- **Language:** Kotlin 2.2.10 (Multiplatform)
- **UI:** Jetpack Compose (Material 3), Compose BOM 2026.02.01
- **Build system:** Gradle (version catalog + AGP 9.2.1)
- **Platforms:** Android (JVM 11, minSdk 24, targetSdk 36, compileSdk 37), iOS (X64, Arm64, SimulatorArm64)

---

## Current Modules

- `:app` — Android sample application demonstrating checkout and wallet flows.
- `:shared` — Kotlin Multiplatform library with `commonMain`, `androidMain`, `iosMain` source sets.

---

## Completed Goals (from completed.log)

1. **Android build compileSdk alignment**: Updated `compileSdk` to 37 to satisfy `androidx.core:core-ktx:1.19.0`.
2. **KMP shared module initialization**: Configured `:shared` multiplatform module with `commonMain`, `androidMain`, and `iosMain` source sets.
