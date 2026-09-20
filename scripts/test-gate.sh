#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
BUILD="$ROOT/target/dependency-free-gate"
rm -rf "$BUILD"
mkdir -p "$BUILD/main" "$BUILD/test"
find "$ROOT/src/main/java" -name '*.java' -print > "$BUILD/main-sources.txt"
javac --release 21 -d "$BUILD/main" @"$BUILD/main-sources.txt"
cat > "$BUILD/test-sources.txt" <<SRC
$ROOT/src/test/java/com/jaimin/db/TransactionStateMachineTest.java
$ROOT/src/test/java/com/jaimin/db/CorrectnessAuditTest.java
$ROOT/src/test/java/com/jaimin/db/ArchitectureAuditTest.java
$ROOT/src/test/java/com/jaimin/db/MvccCrudTest.java
$ROOT/src/test/java/com/jaimin/db/AggregationQueryTest.java
$ROOT/src/test/java/com/jaimin/db/query/AdvancedQueryOptimizerTest.java
$ROOT/src/test/java/com/jaimin/db/reliability/CrashRecoveryIntegration.java
$ROOT/src/test/java/com/jaimin/db/reliability/IntegrityHardeningTest.java
$ROOT/src/test/java/com/jaimin/db/sql/SecurityHardeningTest.java
$ROOT/src/test/java/com/jaimin/db/sql/SecurityFuzzRegressionTest.java
$ROOT/src/test/java/com/jaimin/db/storage/StorageEngineV2Test.java
$ROOT/src/test/java/com/jaimin/db/integration/ForgeDbIntegrationSuite.java
$ROOT/src/test/java/com/jaimin/db/PropertyInvariantTest.java
$ROOT/src/test/java/com/jaimin/db/TestMatrix.java
SRC
# Keep the gate independent of JUnit; JUnit tests are executed by Maven when dependencies are available.
javac --release 21 -cp "$BUILD/main" -d "$BUILD/test" @"$BUILD/test-sources.txt"
java -cp "$BUILD/main:$BUILD/test" com.jaimin.db.ArchitectureAuditTest
java -cp "$BUILD/main:$BUILD/test" com.jaimin.db.CorrectnessAuditTest
java -cp "$BUILD/main:$BUILD/test" com.jaimin.db.TestMatrix
