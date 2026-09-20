# ForgeDB - Interview Guide

## 1. Why build a database from scratch?

The project provides a practical way to demonstrate systems engineering across the complete data path: SQL parsing, query planning, execution, indexing, storage, transactions, recovery and operations.

## 2. Explain the architecture

A client enters through the CLI or database API. SQL is tokenized and parsed into an AST, planned and optimized, then executed through operators. Data access reaches the database layer, transaction/WAL layer and storage engine. Observability surrounds these paths without owning database state.

## 3. Why a B+ tree?

B+ trees provide ordered keys and efficient point/range access while matching page-oriented storage. They also make index-versus-table-scan planning a useful optimization problem.

## 4. How does recovery work?

ForgeDB analyses WAL records, identifies committed transactions, redoes committed mutations after the durable boundary, handles incomplete WAL tails, and persists the recovered state. Recovery is designed to be idempotent for already-applied records.

## 5. How did you test crash safety?

Fault injection introduces deterministic failures at transaction/WAL boundaries. Recovery scenarios cover committed crashes, uncommitted crashes, incomplete WAL tails and page corruption.

## 6. What was a correctness bug you fixed?

The correctness audit identified lexicographic numeric comparison/order issues, partial multi-row UPDATE risk, and incomplete transaction snapshot metadata. These were corrected and promoted into regression tests.

## 7. What was an architecture issue you fixed?

Transaction state had two potential owners. The transaction manager is now the authoritative state machine, while Database exposes derived transaction-state views.

## 8. What security controls exist?

The parser and storage boundaries apply input/resource limits, including SQL size, token, identifier, string and numeric constraints, plus table/column/row/value limits and controlled filesystem handling in the CLI.

## 9. What would you build next?

A production database would require deeper isolation semantics, concurrent transactions, stronger durability guarantees, richer type/null semantics, mature SQL compatibility, extensive benchmarking and multi-platform operational testing.
