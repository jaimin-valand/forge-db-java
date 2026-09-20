# Transactions, WAL and Crash Recovery

ForgeDB uses a **single-writer transaction model** for its embedded engine.

## Transaction lifecycle

```text
BEGIN
  -> capture in-memory rollback snapshot
  -> append WAL BEGIN
  -> execute CREATE/INSERT
  -> append logical mutation records
COMMIT
  -> append WAL COMMIT and force it to disk
  -> checkpoint the committed LSN into the database snapshot
  -> atomically replace the snapshot
  -> compact WAL
```

`ROLLBACK` restores the transaction snapshot and writes an ABORT record.

## Write-ahead logging

The WAL is a binary append-only stream containing:

- LSN (log sequence number)
- transaction ID
- record type
- UTF-8 logical SQL payload for mutations

A transaction is considered committed only when its `COMMIT` record exists.
The WAL is forced to disk before the database snapshot is checkpointed.

## Crash recovery

On startup ForgeDB:

1. Loads the last database snapshot.
2. Reads the WAL.
3. Groups records by transaction.
4. Ignores transactions without `COMMIT`.
5. Replays committed mutations newer than the snapshot checkpoint LSN.
6. Persists the recovered state.
7. Compacts the WAL.

The checkpoint LSN prevents double application if a crash happens after the
snapshot is written but before WAL compaction completes.

## Design trade-off

This implementation uses logical WAL records rather than physical page-level
logging. That keeps the educational engine understandable while demonstrating
real durability concepts: write-ahead ordering, commit records, checkpoints,
atomic snapshots and recovery of committed work.

A future production-oriented version could move to physiological/page-level WAL
and ARIES-style recovery.
