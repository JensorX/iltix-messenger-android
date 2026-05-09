#!/usr/bin/env bash
set -euo pipefail

# Setup Iltix development workspace.
# Downloads upstream Element X at the pinned version, applies overlay + patches,
# and produces a ready-to-use Android Studio project in workspace/.

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# Read upstream config
source <(grep -v '^#' "$PROJECT_ROOT/iltix.upstream.properties" | grep -v '^\s*$')
TAG="${1:-$UPSTREAM_TAG}"

echo "=== Iltix Workspace Setup ==="
echo "Project root: $PROJECT_ROOT"
echo "Upstream tag:  $TAG"
echo ""

# Ensure patcher is built
echo "--- Building patcher ---"
cd "$PROJECT_ROOT/patcher"
if command -v gradle &>/dev/null; then
    gradle jar --quiet
elif [ -f "./gradlew" ]; then
    ./gradlew jar --quiet
else
    # Use the root project's gradlew or try wrapper
    echo "No Gradle found. Downloading wrapper..."
    gradle wrapper --quiet 2>/dev/null || {
        echo "Installing Gradle wrapper for patcher..."
        mkdir -p gradle/wrapper
        curl -sL "https://services.gradle.org/distributions/gradle-8.5-bin.zip" -o /tmp/gradle.zip
        unzip -qo /tmp/gradle.zip -d /tmp/gradle
        /tmp/gradle/gradle-8.5/bin/gradle wrapper --quiet
        rm -rf /tmp/gradle /tmp/gradle.zip
    }
    ./gradlew jar --quiet
fi
cd "$PROJECT_ROOT"

# Run patcher
echo ""
echo "--- Running patcher ---"
java -jar "$PROJECT_ROOT/patcher/build/libs/iltix-patcher-1.0.0.jar" \
    --root "$PROJECT_ROOT" \
    --upstream-tag "$TAG"

echo ""
echo "=== Done ==="
echo "Open $PROJECT_ROOT/workspace/ in Android Studio."
echo ""
echo "After making changes to Iltix code in workspace/, run:"
echo "  ./scripts/extract-iltix.sh"
echo "to copy changes back to overlay/."
