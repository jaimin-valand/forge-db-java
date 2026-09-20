# ForgeDB Architecture Diagram

```text
CLI / SQL
   │
   ▼
Lexer → Parser → AST
   │
   ▼
Planner → Statistics → Cost Optimizer
   │
   ▼
Execution Operators
   ├── Scan
   ├── Filter
   ├── Projection
   ├── Join
   ├── Aggregate
   └── Sort / Limit / Offset
   │
   ├──────────────► B+ Tree Indexes
   │
   ▼
Database / Transactions
   ├── MVCC-lite
   ├── State Machine
   └── Constraints
   │
   ├──────────────► WAL / Checkpoints / Recovery
   │
   ▼
Storage Engine
   ├── Buffer Pool
   ├── Pages / Slotted Records
   ├── Checksums
   └── Free-page Reuse

Cross-cutting: Security • Metrics • Tracing • Health • Chaos Tests • CI/CD
```
