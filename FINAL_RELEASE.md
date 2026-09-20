# ForgeDB 1.0.0 Final Release

ForgeDB 1.0.0 is the frozen first portfolio release of the embedded SQL database engine.

## Release contract

- Java 21
- Semantic version: 1.0.0
- Dependency-free release gate: 11/11
- Security fuzz regression: 2,000 deterministic malformed SQL inputs
- Crash/recovery and integrity scenarios
- Storage, query, transaction, optimizer and CLI validation
- Release JAR with SHA-256 checksum and machine-readable manifest
- GitHub-ready CI workflow and contribution templates

## Final validation

The source tree was cleanly compiled with Java 21, the dependency-free release gate passed, the CLI smoke test passed, and `forge-db-1.0.0.jar` was packaged and checksummed.

## Scope

ForgeDB is an educational and portfolio database engine. It is not positioned as a production replacement for PostgreSQL, SQLite, MySQL, or other mature database systems.
