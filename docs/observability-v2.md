# Observability 2.0

ForgeDB exposes a dependency-free observability layer designed for an embedded database.

## Signals

- Structured JSON logs with TRACE/DEBUG/INFO/WARN/ERROR levels.
- Query traces with trace IDs, duration and success/failure fields.
- Logarithmic latency histogram with p50/p95/p99 estimates.
- Runtime health report covering catalog and transaction state.
- Machine-readable JSON diagnostic snapshot.
- Existing database counters remain available through `Metrics`.

## Design

Observability is intentionally separated from database correctness paths. Metrics use atomic counters, logging is dependency-free, and diagnostic snapshots do not mutate database state.

## CLI integration

The professional shell can expose these signals through future meta-commands without requiring a logging framework or external agent.
