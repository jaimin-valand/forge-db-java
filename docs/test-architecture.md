# ForgeDB Test Architecture

## Purpose

The test architecture is designed to catch correctness regressions at the lowest useful level while preserving a dependency-free release gate for the critical database path.

## Test layers

| Layer | Scope | Typical feedback |
|---|---|---|
| Unit | parser, planner, storage primitives | milliseconds |
| Component | B+ tree, transactions, joins, aggregation | milliseconds |
| Integration | SQL through persistence | milliseconds/seconds |
| Reliability | crash/recovery/integrity | seconds |
| Security | hostile and oversized inputs | milliseconds |
| Property/invariant | deterministic generated scenarios | milliseconds/seconds |
| Release gate | critical end-to-end path | bounded local suite |

## Dependency-free release gate

`src/test/java/com/jaimin/db/TestMatrix.java` invokes tests that expose a normal `main` entry point. This avoids coupling the critical gate to the presence of JUnit artifacts.

The gate currently covers:

1. transaction state transitions
2. MVCC CRUD and rollback/commit/reload
3. aggregation
4. advanced optimizer
5. crash recovery
6. integrity hardening
7. SQL security hardening
8. storage engine 2.0
9. full end-to-end integration
10. property/invariant checks

## Property-style checks

ForgeDB uses deterministic property-style tests instead of introducing a heavyweight property-testing dependency at this stage. The checks validate invariants across multiple generated inputs, including page allocation/reuse, buffer-pool capacity and SQL persistence round trips.

## Correctness policy

Performance thresholds are deliberately excluded from correctness tests. Latency depends on CPU, JVM, filesystem and CI environment. Performance tests record observations; correctness tests assert deterministic behavior.

## Failure policy

The release gate is fail-fast. A failing scenario prints the underlying exception and stops the matrix, making the first regression easy to identify. CI should archive the test output as a build artifact.
