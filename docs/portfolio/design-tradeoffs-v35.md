# ForgeDB - Design Trade-offs

| Decision | Reason | Trade-off |
|---|---|---|
| Java 21 | Strong typing and modern JVM tooling | JVM overhead compared with a native engine |
| Embedded architecture | Simple local deployment and testing | No distributed execution |
| B+ tree indexes | Ordered page-oriented access | More implementation complexity than a scan-only engine |
| MVCC-lite | Demonstrates version visibility and rollback | Not equivalent to full production isolation |
| WAL + checkpoint recovery | Demonstrates durable transaction recovery | Requires careful replay/idempotency rules |
| Cost-based optimizer | Demonstrates statistics-driven planning | Statistics maintenance adds complexity |
| Dependency-free release gate | Portable validation in constrained environments | Less expressive than a full integration test framework |
