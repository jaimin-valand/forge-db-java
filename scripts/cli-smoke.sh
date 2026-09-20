#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
rm -rf "$ROOT/build/cli-smoke" && mkdir -p "$ROOT/build/cli-smoke"
find "$ROOT/src/main/java" -name '*.java' > "$ROOT/build/cli-smoke/sources.txt"
javac --release 21 -d "$ROOT/build/cli-smoke" @"$ROOT/build/cli-smoke/sources.txt"
printf '%s\n' ':version' ':help' 'CREATE TABLE users (id INT PRIMARY KEY, name TEXT);' 'INSERT INTO users VALUES (1, '\''Jaimin'\'');' ':tables' ':schema users' ':indexes users' ':tx' ':metrics' ':history 5' ':export build/cli-smoke/export.sql' ':quit' | java -cp "$ROOT/build/cli-smoke" com.jaimin.db.ForgeDbCli "$ROOT/build/cli-smoke/test.db" > "$ROOT/build/cli-smoke/output.txt"
grep -q 'ForgeDB 1.0.0' "$ROOT/build/cli-smoke/output.txt"
grep -q 'users' "$ROOT/build/cli-smoke/output.txt"
test -s "$ROOT/build/cli-smoke/export.sql"
echo 'PASS: professional CLI smoke test'
