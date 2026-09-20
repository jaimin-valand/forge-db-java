#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

command -v mvn >/dev/null 2>&1 || {
  echo 'ERROR: Maven 3.9+ is required for the full local CI path.'
  echo 'The dependency-free release gate remains available via: bash scripts/test-gate.sh'
  exit 2
}

printf '[1/6] Maven compile\n'
mvn -B -ntp -DskipTests compile

printf '[2/6] Maven tests\n'
mvn -B -ntp test

printf '[3/6] Dependency-free release gate\n'
bash scripts/test-gate.sh

printf '[4/6] CLI smoke test\n'
bash scripts/cli-smoke.sh

printf '[5/6] Maven package\n'
mvn -B -ntp -DskipTests package

printf '[6/6] Final release gate\n'
bash scripts/final-release-gate.sh

printf '\nCI LOCAL PASS: ForgeDB 1.0.0\n'
