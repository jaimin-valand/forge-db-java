# ForgeDB - Portfolio Case Study

## One-line pitch

ForgeDB is a Java 21 embedded SQL database engine built from first principles to demonstrate database internals, storage engineering, query optimization, transactions, recovery, security, observability, testing, and release engineering.

## Problem

Instead of treating a database as an external dependency, this project explores what happens underneath SQL: parsing, planning, execution, indexing, pages, buffering, transactions, write-ahead logging, recovery, and operational diagnostics.

## Engineering scope

- SQL lexer, parser, AST and validation
- Volcano-style execution operators
- Cost-based query optimization with statistics
- INNER JOIN, aggregation, GROUP BY, HAVING, ORDER BY, LIMIT and OFFSET
- B+ tree indexing
- Slotted pages and buffer pool
- Page checksums, page typing and free-page reuse
- Constraints including PRIMARY KEY, UNIQUE and NOT NULL
- MVCC-lite row versions
- Transaction state machine and WAL
- Checkpoint-aware recovery and redo
- Deterministic fault injection and chaos scenarios
- Resource/security limits and parser fuzz regression
- Structured observability and latency histograms
- Professional CLI, CI/CD and release validation

## Engineering outcomes

The project includes deterministic release gates, resilience scenarios, a 2,000-input SQL fuzz regression, architecture/correctness audits, benchmark instrumentation, CLI smoke tests and release artifact validation.

## Important scope statement

ForgeDB is an educational and portfolio-scale embedded database engine. It is not positioned as a production replacement for PostgreSQL, SQLite, MySQL, or another mature database system.
