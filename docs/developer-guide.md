# Developer Guide

## Core boundaries

`Database` is the application-facing facade. It coordinates catalog, storage, query execution and transaction state without requiring callers to understand page formats.

The SQL layer owns syntax. The query layer owns planning/execution. Storage owns durable pages. Reliability owns failure scenarios. Observability reports what happened but should not decide what the database means.

## Adding a feature

Start by identifying the subsystem boundary and its invariant. Implement the smallest deterministic behaviour, add a focused regression test, run the dependency-free release gate, then update the relevant documentation.

## Debugging

Prefer `EXPLAIN`, CLI diagnostics, metrics, structured logs and deterministic fault scenarios over adding ad-hoc print statements to core storage code.
