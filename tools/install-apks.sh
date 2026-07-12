#!/bin/sh
# Install both dazki APKs on a connected device.
# Usage: ./tools/install-apks.sh [serial]
set -e
SERIAL="${1:-}"
ADB="adb"
if [ -n "$SERIAL" ]; then
    ADB="adb -s $SERIAL"
fi
$ADB install -r manager/build/outputs/apk/debug/manager-debug.apk
$ADB install -r sample/build/outputs/apk/debug/sample-debug.apk
echo "Installed both APKs."
