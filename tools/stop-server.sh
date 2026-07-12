#!/bin/sh
# Stop the dazki server on the device.
# Usage: ./tools/stop-server.sh
adb shell pkill -f DazkiServerMain 2>/dev/null || true
echo "Server stopped (if it was running)."
