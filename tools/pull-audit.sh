#!/bin/sh
# Pull the audit log from the device.
# Usage: ./tools/pull-audit.sh [output-path]
set -e
OUT="${1:-./dazki-audit.jsonl}"
adb pull /data/local/tmp/dazki-audit.jsonl "$OUT"
echo "Pulled audit log to $OUT"
echo "Lines: $(wc -l < "$OUT")"
