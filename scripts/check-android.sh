#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

java scripts/CompareScreenshots.java --self-test

# Generic build/check tasks also select Apple KMP targets and cannot run on Linux.
./gradlew \
    -Pmelon.verification=true \
    :apps:android:app:assembleRelease \
    :apps:android:benchmark:assembleBenchmark \
    :apps:android:benchmark:assembleProfile \
    testDebugUnitTest jvmTest lintDebug --stacktrace "$@"
