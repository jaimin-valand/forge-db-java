# Query Engine Guide

ForgeDB separates parsing, planning and execution.

```text
SQL
 ↓
Lexer
 ↓
AST / Statement
 ↓
Statistics + Optimizer
 ↓
Query Plan
 ↓
Operators
 ↓
Storage / Indexes
```

## Operators

- `TableScanOperator`
- `FilterOperator`
- `ProjectOperator`
- `JoinOperator`

## Optimizer

The optimizer compares candidate plans using table statistics and access-path costs. Equality predicates can use B+ tree indexes when the indexed column is suitable; otherwise the planner can select a table scan.

Later planning stages account for joins, aggregation, ordering and pagination.

## EXPLAIN

`EXPLAIN` is intentionally inspectable. It should be possible to explain why an index lookup was selected rather than treating optimization as opaque behaviour.
