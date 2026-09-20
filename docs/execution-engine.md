# ForgeDB Execution Engine

## Design

ForgeDB uses a compact Volcano-style pull model. Operators expose `next()` and produce one row at a time. This keeps execution composable and avoids materialising intermediate results between every relational operation.

```text
                 SQL AST
                    |
              Query Planner
                    |
              TableScanOperator
                    |
              FilterOperator
                    |
             ProjectOperator
                    |
                  Rows
```

## Operators

### Table Scan

Reads a table snapshot sequentially. It is the baseline physical access path when no selective index plan is available.

### Filter

Evaluates predicates against row values. Numeric values are compared numerically where possible; other values use lexical comparison for ordering operators.

### Project

Reduces each row to the requested output columns. Projection is applied after filtering so downstream operators receive only the requested shape.

## Why this architecture?

A direct `SELECT` implementation would tightly couple SQL parsing, planning and storage. The operator boundary gives ForgeDB a clean extension point for future operators such as index scans, joins, aggregation, sorting and limit/offset.
