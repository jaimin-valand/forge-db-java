# Architecture

## Design goals
The database should expose the mechanics hidden by high-level database APIs while remaining small enough to reason about.

## Components
1. **Catalog** - maps table names to schemas.
2. **Row store** - stores ordered records.
3. **Equality index** - maps a column value to matching row positions.
4. **Persistence layer** - serializes catalog and records to a binary file.
5. **SQL engine** - parses a deliberately small SQL grammar and dispatches operations.

## Trade-offs
The first milestone keeps rows in memory and persists a compact snapshot after mutations. This is intentionally simpler than a production WAL/page-buffer architecture. The roadmap introduces fixed-size pages, a buffer pool and WAL so each layer can be measured and tested independently.

## Query planning and indexing (Phase 2)

ForgeDB uses a B+ tree for equality secondary-index lookups. The query planner checks whether the predicate column is indexed and emits either `INDEX_LOOKUP` or `TABLE_SCAN`. The `EXPLAIN` command exposes this decision so the execution strategy is inspectable rather than hidden.
