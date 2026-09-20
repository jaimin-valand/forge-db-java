# ForgeDB Portfolio Guide - 1.0.0

## Positioning

ForgeDB should be presented as a from-scratch Java 21 relational database engine and systems-engineering portfolio project. The strongest message is not the number of features; it is the engineering progression from storage primitives to SQL semantics, transactions, recovery, resilience, observability and release automation.

## Suggested GitHub presentation

1. Lead with the one-sentence project description.
2. Show the architecture diagram before the implementation details.
3. Highlight measurable engineering evidence: release gates, security fuzzing, crash/recovery scenarios and benchmark instrumentation.
4. Link readers to focused docs rather than putting every implementation detail in the README.
5. State limitations explicitly.

## Interview-ready themes

### Storage
Explain why fixed-size pages, slotted records, checksums, buffer-pool pinning and free-page reuse form the storage foundation.

### Query processing
Explain the separation between parsing, planning, optimization and execution, and why statistics are needed for cost-based decisions.

### Transactions and recovery
Explain WAL ordering, transaction states, snapshots, commit records, recovery analysis and redo.

### Correctness
Use the atomic UPDATE validation work as an example of preventing partial statement effects when uniqueness constraints fail.

### Reliability
Describe deterministic fault points and how crash scenarios distinguish committed from uncommitted work.

### Security
Discuss bounded SQL/resource inputs and the 2,000-case deterministic malformed-input regression suite.

### Delivery
Describe the dependency-free release gate, CI workflow, release manifest, JAR validation and SHA-256 checksum.

## What not to claim

Do not present ForgeDB as PostgreSQL-compatible or production-equivalent to mature distributed databases. It is a deliberately scoped educational/systems implementation designed to make database internals inspectable and explainable.
