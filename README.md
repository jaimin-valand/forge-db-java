# ForgeDB 1.0.0

[![CI](https://github.com/jaimin-valand/forge-db-java/actions/workflows/ci.yml/badge.svg)](https://github.com/jaimin-valand/forge-db-java/actions/workflows/ci.yml)

> A from-scratch Java 21 embedded SQL database engine built to demonstrate database internals, systems engineering, reliability, security, performance, and release engineering.

![Java 21](https://img.shields.io/badge/Java-21-orange)
![Release](https://img.shields.io/badge/release-1.0.0-green)
![Status](https://img.shields.io/badge/status-final-blue)

## What is ForgeDB?

ForgeDB is a small relational database engine implemented from scratch in Java. It is designed as an inspectable engineering project: every major subsystem is small enough to study, test, explain, and evolve.

It is **not** intended to replace PostgreSQL, SQLite, MySQL, or another mature production database.

## Engineering scope

| Layer | Implementation |
|---|---|
| SQL | Lexer, parser, AST, DDL/DML, SELECT, JOIN, GROUP BY, HAVING, ORDER BY, LIMIT/OFFSET, EXPLAIN |
| Query engine | Volcano-style execution, filtering, projection, joins, aggregation, sorting |
| Optimizer | Statistics-driven cost model, access-path selection, join planning |
| Indexing | B+ tree indexes and indexed equality lookups |
| Storage | Fixed-size pages, slotted records, checksums, page types, buffer pool, pinning, dirty-page flushing, free-page reuse |
| Transactions | Transaction state machine, MVCC-lite, commit/rollback |
| Durability | WAL, checkpoints, recovery analysis/redo, incomplete-tail handling |
| Reliability | Deterministic fault injection, chaos scenarios, corruption detection |
| Security | SQL/resource limits, path protection, malformed-input regression/fuzz testing |
| Observability | Structured logs, query traces, latency histograms, health diagnostics, metrics |
| Tooling | Professional CLI, benchmarks, release gates, CI/CD, release manifests and checksums |

## Architecture

```text
                    ForgeDB CLI / SQL API
                              |
                       Lexer -> Parser -> AST
                              |
                    Planner + Cost Optimizer
                              |
                    Volcano-style Executor
                  /       |       |        \
               Scan    Filter    Join    Aggregate
                  \       |       |        /
                   ------ Result Pipeline ------
                              |
                +-------------+-------------+
                |                           |
             B+ Trees                  Buffer Pool
                |                           |
                +-------------+-------------+
                              |
                         Page Storage
                              |
                       WAL + Checkpoints
                              |
                     Crash Recovery / REDO

Cross-cutting: Security | Metrics | Tracing | Health | Testing
```

## Quick start

### Requirements

- JDK 21+
- Maven 3.9+

Build and launch:

```bash
mvn -q -DskipTests package
java -cp target/classes com.jaimin.db.ForgeDbCli
```

Example session:

```sql
CREATE TABLE users (
  id INT PRIMARY KEY,
  email TEXT UNIQUE,
  name TEXT NOT NULL
);

INSERT INTO users VALUES (1, 'jaimin@example.com', 'Jaimin');

EXPLAIN SELECT * FROM users WHERE id = 1;
SELECT * FROM users WHERE id = 1;
```

### Dependency-free release gate

The repository also includes a core validation path that does not require Maven:

```bash
bash scripts/test-gate.sh
```

## Why this is a strong systems project

ForgeDB deliberately goes beyond CRUD. The implementation connects database theory with software-engineering practice:

- **Storage:** pages, buffer management, checksums and free-page reuse.
- **Query processing:** parsing, planning, execution, joins and aggregation.
- **Optimization:** statistics and cost-based access-path/join decisions.
- **Transactions:** explicit lifecycle states, MVCC-lite and rollback semantics.
- **Durability:** write-ahead logging, checkpoints and recovery redo.
- **Resilience:** deterministic crash points, torn-WAL tests and corruption checks.
- **Security:** bounded parser/resource behaviour and malformed-input regression.
- **Operations:** metrics, traces, health diagnostics and a professional CLI.
- **Delivery:** automated gates, versioned artifacts, checksums and CI/CD.

## Validation evidence

The final release process includes:

- Architecture and dependency-boundary checks
- Correctness checks for numeric semantics, constraints, atomic updates and schema rollback
- Transaction/MVCC validation
- JOIN and aggregation validation
- Storage and persistence checks
- WAL/crash-recovery scenarios
- Security regression and **2,000 deterministic malformed SQL inputs**
- Property/invariant checks
- CLI smoke testing
- Java 21 compilation and JAR packaging
- Release manifest and SHA-256 artifact verification

> Benchmark numbers are environment-dependent. They are included as engineering measurements, not universal performance claims.

## Repository guide

| Topic | Documentation |
|---|---|
| Architecture | [`docs/architecture.md`](docs/architecture.md) |
| Getting started | [`docs/getting-started.md`](docs/getting-started.md) |
| SQL reference | [`docs/sql-reference.md`](docs/sql-reference.md) |
| Query engine | [`docs/query-engine-guide.md`](docs/query-engine-guide.md) |
| Optimizer | [`docs/query-optimizer.md`](docs/query-optimizer.md) |
| Storage engine | [`docs/storage-engine-guide.md`](docs/storage-engine-guide.md) |
| Transactions & recovery | [`docs/transactions-recovery-guide.md`](docs/transactions-recovery-guide.md) |
| CLI | [`docs/cli-guide.md`](docs/cli-guide.md) |
| Security | [`docs/security-hardening.md`](docs/security-hardening.md) |
| Fault injection | [`docs/fault-injection.md`](docs/fault-injection.md) |
| Observability | [`docs/observability-guide.md`](docs/observability-guide.md) |
| Testing | [`docs/test-architecture.md`](docs/test-architecture.md) |
| CI/CD | [`docs/ci-cd-v32.md`](docs/ci-cd-v32.md) |
| Release engineering | [`docs/release-engineering-v33.md`](docs/release-engineering-v33.md) |
| Portfolio/interview guide | [`docs/portfolio-v35.md`](docs/portfolio-v35.md) |
| Final release | [`FINAL_RELEASE.md`](FINAL_RELEASE.md) |

## Project structure

```text
forge-db-java/
├── src/main/java/       # Database engine implementation
├── src/test/java/       # Unit/integration/reliability tests
├── docs/                # Architecture and engineering documentation
├── scripts/             # Build, test, release and CLI automation
├── pom.xml
├── CONTRIBUTING.md
├── CHANGELOG.md
└── FINAL_RELEASE.md
```

## Engineering decisions worth discussing in an interview

1. Why a page-oriented storage model?
2. Why use a buffer pool instead of reading every page directly?
3. How does the B+ tree change indexed lookup cost?
4. How does the optimizer decide between a scan and an index access path?
5. How are transaction states prevented from diverging?
6. What does WAL guarantee, and what happens after a crash?
7. How does recovery distinguish committed and incomplete transactions?
8. Why is UPDATE validation performed before mutation?
9. How are resource-exhaustion attacks bounded?
10. How does deterministic fault injection improve reliability testing?

## Scope boundaries

ForgeDB intentionally does not claim:

- PostgreSQL/SQLite/MySQL compatibility
- Full SQL standard coverage
- Multi-node distributed consensus
- Production-grade replication
- Enterprise-grade multi-writer concurrency
- Production replacement durability guarantees

Those boundaries keep the project coherent and make its implementation inspectable.

## License

See [`LICENSE`](LICENSE).

## Release

**ForgeDB 1.0.0** is the frozen first portfolio release. The CI workflow validates the source, runs the dependency-free release gate, packages the JAR and uploads the validated release artifacts. See [`RELEASE_NOTES_1.0.0.md`](RELEASE_NOTES_1.0.0.md).
