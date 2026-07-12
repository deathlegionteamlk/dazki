#!/bin/sh
# Clear the audit log on the device.
# Usage: ./tools/clear-audit.sh
adb shell "rm -f /data/local/tmp/dazki-audit.jsonl" 2>/dev/null || true
echo "Audit log cleared."
