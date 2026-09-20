# ForgeDB Phase 30 - Architecture Audit

## Audit scope

The Phase 30 review evaluates package boundaries, ownership of mutable state, security-limit centralisation, observability isolation, transaction lifecycle, and release-test integration.

## Findings and repairs

### 1. Transaction state ownership

**Finding:** `Database` duplicated transaction lifecycle state alongside `TransactionManager`.

**Repair:** `TransactionManager` is now the single owner of transaction state. `Database.inTransaction()`, `transactionState()`, and `currentTransactionId()` are derived views. This prevents state divergence when a transaction transition throws.

### 2. Layer boundaries

The architecture is intentionally organised as:

```text
CLI
 ↓
SQL facade / parser
 ↓
Query planning + execution
 ↓
Database / transaction coordination
 ↓
Storage + indexes + WAL
```

Cross-cutting concerns such as security limits, metrics and observability remain centralised rather than duplicated across operators.

### 3. Dependency rules

The audit enforces these boundaries:

- storage does not import SQL parser classes;
- query execution does not import CLI classes;
- SQL parser code does not import storage primitives;
- observability does not depend on query execution;
- transaction state has one mutable owner;
- resource limits are defined centrally.

### 4. Risk register

| Area | Status | Residual risk |
|---|---|---|
| Transaction ownership | PASS | Single-writer architecture remains intentional |
| Storage boundaries | PASS | Storage is still embedded rather than service-isolated |
| Query boundaries | PASS | Executor uses SQL AST types by design |
| Security limits | PASS | Limits are conservative and configurable in source |
| Observability | PASS | Structured logging is local-process only |
| CLI separation | PASS | CLI remains an application shell over the core |
| Dependency management | PASS | Maven/JUnit are the only declared external dependencies |

## Architecture scorecard

```text
State ownership             PASS
Package boundaries          PASS
Security centralisation     PASS
Observability isolation     PASS
Error-path state integrity  PASS
Dependency hygiene          PASS
```

This audit is an engineering review, not a claim that ForgeDB has the same operational architecture as a production distributed database.
