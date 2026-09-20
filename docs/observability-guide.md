# Observability Guide

ForgeDB exposes counters, traces, structured logs, latency histograms and health diagnostics without requiring an external telemetry service.

## Signals

- **Metrics:** statements, CRUD operations, transactions, scans, index lookups and storage activity.
- **Traces:** query-level trace IDs, operation status and elapsed time.
- **Logs:** structured JSON events with log levels.
- **Health:** transaction state and runtime diagnostics.
- **Latency:** percentile estimates including p50, p95 and p99.

## Principle

Observability is kept separate from query correctness. Instrumentation should add evidence about behaviour without changing database semantics.

See `docs/observability-v2.md` for implementation-level details.
