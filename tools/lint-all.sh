#!/bin/sh
# Run Android lint on every module.
# Usage: ./tools/lint-all.sh
set -e
cd "$(dirname "$0")/.."
./gradlew :api:lint :hidden-api-stubs:lint :manager:lint :sample:lint --console=plain
echo
echo "Lint reports:"
ls -la api/build/reports/lint-results-debug.html 2>/dev/null || true
ls -la hidden-api-stubs/build/reports/lint-results-debug.html 2>/dev/null || true
ls -la manager/build/reports/lint-results-debug.html 2>/dev/null || true
ls -la sample/build/reports/lint-results-debug.html 2>/dev/null || true
