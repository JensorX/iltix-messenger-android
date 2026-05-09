#!/usr/bin/env bash
set -euo pipefail

# Extract Iltix-specific files from workspace/ back to overlay/.
# Run this after editing Iltix code in the workspace to persist changes.

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
WORKSPACE="$PROJECT_ROOT/workspace"
OVERLAY="$PROJECT_ROOT/overlay"

if [ ! -d "$WORKSPACE" ]; then
    echo "Error: workspace/ does not exist. Run setup-workspace.sh first."
    exit 1
fi

echo "=== Extracting Iltix files from workspace → overlay ==="

# Iltix core modules
for module in lib components theme; do
    src="$WORKSPACE/iltix/$module"
    dst="$OVERLAY/iltix/$module"
    if [ -d "$src" ]; then
        echo "  iltix/$module/"
        rm -rf "$dst"
        cp -r "$src" "$dst"
    fi
done

# Iltix docs
if [ -d "$WORKSPACE/iltix/docs" ]; then
    echo "  iltix/docs/"
    rm -rf "$OVERLAY/iltix/docs"
    cp -r "$WORKSPACE/iltix/docs" "$OVERLAY/iltix/docs"
fi

# App icon
if [ -d "$WORKSPACE/appicon/iltix" ]; then
    echo "  appicon/iltix/"
    rm -rf "$OVERLAY/appicon/iltix"
    cp -r "$WORKSPACE/appicon/iltix" "$OVERLAY/appicon/iltix"
fi

# ix source set
if [ -d "$WORKSPACE/app/src/ix" ]; then
    echo "  app/src/ix/"
    rm -rf "$OVERLAY/app/src/ix"
    cp -r "$WORKSPACE/app/src/ix" "$OVERLAY/app/src/ix"
fi

# Feature-level de.iltix packages
FEATURE_ILTIX_DIRS=(
    "features/home/impl/src/main/kotlin/de/iltix"
    "features/messages/impl/src/main/kotlin/de/iltix"
    "features/preferences/impl/src/main/kotlin/de/iltix"
)
for dir in "${FEATURE_ILTIX_DIRS[@]}"; do
    src="$WORKSPACE/$dir"
    dst="$OVERLAY/$dir"
    if [ -d "$src" ]; then
        echo "  $dir/"
        rm -rf "$dst"
        mkdir -p "$(dirname "$dst")"
        cp -r "$src" "$dst"
    fi
done

# Library-level de.iltix packages
LIB_ILTIX_DIRS=(
    "libraries/push/impl/src/main/kotlin/de/iltix"
)
for dir in "${LIB_ILTIX_DIRS[@]}"; do
    src="$WORKSPACE/$dir"
    dst="$OVERLAY/$dir"
    if [ -d "$src" ]; then
        echo "  $dir/"
        rm -rf "$dst"
        mkdir -p "$(dirname "$dst")"
        cp -r "$src" "$dst"
    fi
done

echo ""
echo "=== Extraction complete ==="
echo "Review changes with: git diff overlay/"
