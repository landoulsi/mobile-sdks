#!/bin/bash
# Build/test verification gate for roadmap_implement.sh's --build-cmd
# auto-detection (see that flag's doc comment in roadmap_implement.sh).
# Runs from wherever it's invoked from, but resolves paths relative to its
# own location so it also works when run directly (./ci_verify.sh).
set -e

ROOT="$(cd "$(dirname "$0")" && pwd)"

echo "=== Mobile (Kotlin Multiplatform: :app + :shared android targets) ==="
# `test`, not `allTests` -- allTests also runs shared's iosX64Test/
# iosSimulatorArm64Test, which need an Xcode/simulator setup this gate can't
# assume is present; failing on missing tooling is worse than not gating on
# it at all. `test` covers :app:testDebugUnitTest and :shared's Android host
# tests (confirmed via `./gradlew test --dry-run`).
( cd "$ROOT/mobile" && ./gradlew test --console=plain -q )

echo "All verifications passed."
