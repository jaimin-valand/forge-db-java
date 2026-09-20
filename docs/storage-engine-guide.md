# Storage Engine Guide

ForgeDB uses fixed-size pages, a buffer pool, checksummed page images and typed page metadata.

## Page lifecycle

```text
allocate -> initialize header -> mutate -> mark dirty -> flush -> reload -> validate
```

Page types include `DATA`, `INDEX`, `CATALOG`, `WAL_CHECKPOINT` and `FREE`.

## Buffer pool

The buffer pool provides bounded in-memory page residency and LRU-style eviction. Dirty pages are tracked and pinned pages cannot be evicted until their pin count returns to zero.

## Integrity

Pages carry integrity metadata and CRC validation. Partial or malformed page images are rejected rather than interpreted as valid state.

## Free-page reuse

Deallocated pages can return to the free-page pool and later be allocated again. Storage invariants validate that page ownership and free-page state remain consistent.

## Design trade-off

This is an educational embedded engine. The storage design prioritises inspectability and deterministic tests over the concurrency, compression, parallel I/O and filesystem sophistication of a production database.
