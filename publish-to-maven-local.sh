#!/usr/bin/env bash
#
# Publishes all mobile-sdks KMP library modules to the local Maven repository
# (~/.m2/repository) under group "com.landoulsi" with the given version.
#
# Usage:
#   ./publish-to-maven-local.sh [VERSION]
#
# Examples:
#   ./publish-to-maven-local.sh            # publishes 1.0.0 (default)
#   ./publish-to-maven-local.sh 1.2.0      # publishes 1.2.0
#
# Consuming projects reference the published artifacts as:
#   implementation("com.landoulsi:<module>:<VERSION>")
# e.g. implementation("com.landoulsi:location:1.2.0")
#
set -euo pipefail

VERSION="${1:-1.0.0}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "Publishing mobile-sdks to Maven local with version: $VERSION"

./gradlew \
  :update:publishToMavenLocal \
  :design:publishToMavenLocal \
  :logger:publishToMavenLocal \
  :location:publishToMavenLocal \
  :biometric:publishToMavenLocal \
  :storage:publishToMavenLocal \
  :tutorial:publishToMavenLocal \
  :socialauth:publishToMavenLocal \
  :survey:publishToMavenLocal \
  :pushnotification:publishToMavenLocal \
  :document:publishToMavenLocal \
  :analytics:publishToMavenLocal \
  :remoteconfig:publishToMavenLocal \
  :screenshot:publishToMavenLocal \
  :timeprovider:publishToMavenLocal \
  :schemaui:publishToMavenLocal \
  :fraud:publishToMavenLocal \
  :viewmodel:publishToMavenLocal \
  :diagnostic:publishToMavenLocal \
  :permission:publishToMavenLocal \
  :payment:shared:publishToMavenLocal \
  -PsdkVersion="$VERSION"

echo ""
echo "Done. Published com.landoulsi:*:$VERSION to ~/.m2/repository"
