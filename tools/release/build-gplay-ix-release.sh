#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

export ANDROID_HOME="${ANDROID_HOME:-/home/fabian/Android/Sdk}"
export JAVA_HOME="${JAVA_HOME:-/home/fabian/.jdks/temurin-21}"
export PATH="$JAVA_HOME/bin:$PATH"

if [[ ! -x "$JAVA_HOME/bin/java" ]]; then
    printf 'Invalid JAVA_HOME: %s\n' "$JAVA_HOME" >&2
    exit 1
fi

if [[ ! -d "$ANDROID_HOME" ]]; then
    printf 'Invalid ANDROID_HOME: %s\n' "$ANDROID_HOME" >&2
    exit 1
fi

cd "$ROOT_DIR"

./gradlew \
    --no-daemon \
    -Dorg.gradle.jvmargs='-Xmx8g -XX:MaxMetaspaceSize=2g -Dfile.encoding=UTF-8' \
    :app:assembleGplayIxRelease

printf '\nArtifacts:\n'
find app/build/outputs/apk/gplayIx/release -maxdepth 1 -type f -name '*.apk' | sort