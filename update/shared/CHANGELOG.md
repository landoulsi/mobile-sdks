# Landoulsi Update SDK (`:update:shared`)

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/),
and this project adheres to [Semantic Versioning](https://semver.org/).

## [1.0.0] - 2026-08-27

### Added
- Added automatic version checking that compares the current app version against remote configuration to determine update requirements.
- Added UpdateConfig domain model exposing latestVersion, minRequiredVersion, isUpdateRequired flag, and optional update URL.
- Added UpdateState sealed class with four states: NoUpdate, UpdateRecommended, UpdateRequired, and Error, enabling clear update flow orchestration.
- Added Android NativeUpdateManager wrapping Google Play In-App Updates API for flexible and immediate update flows.
- Added KMP commonMain source set with shared version comparison logic (ComparableVersion) and platform-agnostic update evaluation.

### Changed
- (none)

### Fixed
- (none)

### Deprecated
- (none)

### Removed
- (none)

### Security
- (none)

## [Unreleased]

### Added
- Added `PlayInAppUpdateManager`, a lifecycle-driven wrapper over Google Play In-App Updates that handles update-type selection, the flexible-download listener lifecycle, and resuming an interrupted immediate update.
- Added `UpdatePolicy` to turn Play's reported staleness and priority into a flexible/immediate/no-op decision, with app-tunable thresholds.
- Added `UpdateEvent` for flexible-update download and install progress.

### Changed
- Replaced the stubbed `NativeUpdateManager` with the real `PlayInAppUpdateManager` implementation backed by `com.google.android.play:app-update-ktx`.
- Dropped the unused Ktor and kotlinx.serialization dependencies; the module no longer fetches remote configuration itself.

### Removed
- Removed the unused `Platform` expect/actual boilerplate.

### Fixed
- (reserved)

### Deprecated
- (reserved)

### Removed
- (reserved)

### Security
- (reserved)

---

[Unreleased]: https://github.com/landoulsi/mobile-sdks/compare/1.0.0...HEAD
[1.0.0]: https://github.com/landoulsi/mobile-sdks/releases/tag/1.0.0