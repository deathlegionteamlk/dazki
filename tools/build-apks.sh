#!/bin/sh
# Build the dazki manager and sample debug APKs.
# Usage: ./tools/build-apks.sh
set -e
cd "$(dirname "$0")/.."
./gradlew :manager:assembleDebug :sample:assembleDebug --console=plain
echo
echo "Built:"
ls -la manager/build/outputs/apk/debug/manager-debug.apk
ls -la sample/build/outputs/apk/debug/sample-debug.apk
