# MVCC-lite and Row Versioning

ForgeDB uses a deliberately small versioning layer rather than claiming full production MVCC.

## Model

Each stored row carries:

- `createdTx` - transaction that created the version; `0` means committed.
- `deletedTx` - transaction that superseded/deleted the version; `0` means active.

An update does not mutate the existing row in place. It marks the old version as deleted and appends a new version. A delete marks the visible version as deleted.

## Visibility

A transaction can see its own newly-created versions and cannot see versions deleted by itself. Uncommitted versions from other transactions are hidden.

ForgeDB currently uses a **single-writer transaction model**, so this is an educational MVCC foundation rather than a complete concurrent isolation implementation.

## Commit

At commit, versions created by the transaction become committed (`createdTx = 0`) and superseded versions are physically removed. Indexes are rebuilt after version cleanup.

## Rollback

Rollback restores the transaction snapshot. This provides a deterministic safety net while the WAL records the transaction lifecycle.

## Why this design?

The version metadata is intentionally simple. It demonstrates the core MVCC idea - updates create versions instead of overwriting history - without pretending to implement PostgreSQL/InnoDB-level concurrency semantics.

Future work could add transaction snapshots, reader/writer concurrency, vacuuming, and stronger isolation levels.
