# Query Features

ForgeDB supports result ordering and bounded pagination.

- `ORDER BY column` sorts ascending by default.
- `ORDER BY column DESC` reverses the ordering.
- `LIMIT n` bounds the returned row count.
- `OFFSET n` skips rows after filtering and before projection.
- Numeric strings are compared numerically; other values are compared lexically.

The implementation is intentionally explicit and small: sorting happens in-memory after filtering and before projection. It is therefore suitable for an educational embedded engine, not a claim of production-scale external sorting.
