# `:design`

The shared design system — Compose Multiplatform theme, tokens, and primitive UI
components for Android and iOS.

## What's here

- **`AppTheme`** (`commonMain/.../Theme.kt`) — wraps `MaterialTheme` with the
  module's light/dark `ColorScheme`, `Typography`, and `Shapes`. `dynamicColor`
  resolves per platform via `expect fun dynamicColorScheme(...)`
  (`Theme.android.kt` uses Android 12+ dynamic color; `Theme.ios.kt` falls back
  to the static schemes).
- **Tokens** — `Color.kt`, `Type.kt`, `Shape.kt`, `Space.kt`, `DesignIcons.kt`,
  `Modifiers.kt`.
- **Components** (`commonMain/.../components/`) — each file is prefixed `Design*`:
  `DesignBanner`, `DesignButton` / `DesignOutlinedButton`,
  `DesignCard` / `DesignElevatedCard` / `DesignOutlinedCard`, `DesignChip`,
  `DesignInfoRow`, `DesignStatusIcon`, `DesignSurface`.

## Usage

```kotlin
AppTheme {
    DesignCard {
        DesignInfoRow(label = "Status", value = "Active")
    }
}
```

## Platforms

Android + iOS, via Compose Multiplatform (`compose.runtime`, `compose.foundation`,
`compose.material3`, `compose.ui`).
