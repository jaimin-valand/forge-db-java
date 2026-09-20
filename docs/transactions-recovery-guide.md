# Transactions and Recovery Guide

ForgeDB uses a single-writer transaction model coordinated by `TransactionManager` and an append-only logical WAL.

## State machine

```text
IDLE -> ACTIVE -> COMMITTED
             \-> ABORTED
```

Invalid transitions are rejected explicitly.

## Commit path

```text
BEGIN
  ↓
mutations + WAL records
  ↓
COMMIT WAL record
  ↓
finalise committed versions
  ↓
persist checkpoint
  ↓
WAL compaction
```

## Recovery

Recovery reads WAL records, identifies committed transactions, validates the record stream, and replays committed mutations newer than the persisted LSN. Uncommitted transactions are not replayed.

Recovery is designed to tolerate an incomplete final WAL tail while treating corruption in a completed record as an integrity failure.

## Crash model

Fault-injection scenarios cover crashes after transaction start, after mutation, and after WAL commit. The tests verify that committed state survives recovery while uncommitted work does not become durable.

## Scope

This implementation demonstrates WAL and recovery mechanics but is not a claim of production-grade group commit, distributed consensus, multi-writer isolation or point-in-time recovery.
