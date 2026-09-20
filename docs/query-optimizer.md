# Query Optimizer

ForgeDB uses a deterministic educational cost model. The optimizer compares
physical access paths instead of choosing an index merely because one exists.

## Access paths

- `TABLE_SCAN`: cost is approximately visible row count.
- `INDEX_LOOKUP`: cost is B+ tree height plus estimated matching rows.
- `NESTED_LOOP_JOIN`: cost is approximately left rows × right rows.
- `INDEX_NESTED_LOOP_JOIN`: cost is approximately left rows × right-side B+ tree height when the right join key is indexed.

Statistics currently include row counts and per-column distinct counts. Equality
selectivity is approximated as `1 / distinctValues`. This is intentionally
transparent and deterministic; it is not a replacement for a production
optimizer's histograms, correlation statistics, or adaptive execution.

`EXPLAIN` exposes candidate strategies, estimated rows and estimated cost so
that a developer can inspect the optimizer's reasoning.
