# ForgeDB 1.0.0

## First stable portfolio release

ForgeDB 1.0.0 freezes the architecture after the full development roadmap and final release gate.

### Validated areas

- SQL lexer/parser and execution engine
- Cost-based query planning and optimization
- JOIN and aggregation execution
- B+ tree indexing
- Page-based storage and buffer pool
- MVCC-lite transaction semantics
- WAL and crash recovery
- Checkpoints and integrity validation
- Security limits and deterministic parser fuzzing
- Chaos/fault-injection recovery scenarios
- Professional CLI
- Observability and diagnostics
- Dependency-free integration/release testing
- CI/CD and release engineering

### Release evidence

- 11/11 dependency-free release scenarios passed
- 2,000/2,000 malformed SQL fuzz cases rejected safely
- Java 21 compilation passed
- CLI smoke test passed
- Release JAR packaged and manifest validated
- SHA-256 checksum verified

### Positioning

ForgeDB is a learning and portfolio implementation of database-engineering concepts. It is not intended to replace mature production database systems.
