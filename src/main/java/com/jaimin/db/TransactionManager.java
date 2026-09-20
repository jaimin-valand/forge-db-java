package com.jaimin.db;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import com.jaimin.db.reliability.FaultInjector;

/** Coordinates a single-writer transaction and delegates durable logging to WAL. */
public final class TransactionManager {
  public enum State { IDLE, ACTIVE, COMMITTED, ABORTED }
  private final Database database;
  private final Wal wal;
  private State state = State.IDLE;
  private long transactionId;
  private Database.Snapshot snapshot;

  TransactionManager(Database database, Path walPath) throws IOException {
    this.database = database;
    this.wal = new Wal(walPath);
  }

  public synchronized long begin() throws IOException {
    if (state == State.ACTIVE) throw new IllegalStateException("transaction already active");
    if (state == State.COMMITTED || state == State.ABORTED) state = State.IDLE;
    transactionId = wal.nextTransactionId();
    snapshot = database.snapshot();
    wal.append(new Wal.Record(wal.nextLsn(), transactionId, Wal.Type.BEGIN, ""));
    state = State.ACTIVE;
    return transactionId;
  }

  public synchronized boolean active() { return state == State.ACTIVE; }
  public synchronized State state() { return state; }
  public synchronized long currentTransactionId() { return transactionId; }

  public synchronized void recordCreate(String sql) throws IOException {
    requireActive();
    wal.append(new Wal.Record(wal.nextLsn(), transactionId, Wal.Type.CREATE_TABLE, sql));
  }

  public synchronized void recordInsert(String sql) throws IOException {
    requireActive();
    wal.append(new Wal.Record(wal.nextLsn(), transactionId, Wal.Type.INSERT, sql));
  }

  public synchronized void recordUpdate(String sql) throws IOException {
    requireActive();
    wal.append(new Wal.Record(wal.nextLsn(), transactionId, Wal.Type.UPDATE, sql));
  }

  public synchronized void recordDelete(String sql) throws IOException {
    requireActive();
    wal.append(new Wal.Record(wal.nextLsn(), transactionId, Wal.Type.DELETE, sql));
  }

  public synchronized void commit() throws IOException {
    requireActive();
    long commitLsn = wal.nextLsn();
    wal.append(new Wal.Record(commitLsn, transactionId, Wal.Type.COMMIT, ""));
    FaultInjector.afterWalCommit();
    database.finalizeCommittedVersions(transactionId);
    database.setLastAppliedLsn(commitLsn);
    database.persistForTransactionCommit();
    wal.compact(commitLsn);
    state = State.COMMITTED;
    snapshot = null;
  }

  public synchronized void rollback() throws IOException {
    requireActive();
    long abortLsn = wal.nextLsn();
    wal.append(new Wal.Record(abortLsn, transactionId, Wal.Type.ABORT, ""));
    database.restore(snapshot);
    database.persistForTransactionCommit();
    wal.compact(abortLsn);
    state = State.ABORTED;
    snapshot = null;
  }

  public synchronized void recover() throws IOException {
    List<Wal.Record> records = wal.readAll();
    Map<Long, List<Wal.Record>> byTx = new LinkedHashMap<>();
    Set<Long> committed = new HashSet<>();
    for (Wal.Record r : records) {
      byTx.computeIfAbsent(r.txId(), ignored -> new ArrayList<>()).add(r);
      if (r.type() == Wal.Type.COMMIT) committed.add(r.txId());
    }
    long maxLsn = database.lastAppliedLsn();
    for (var entry : byTx.entrySet()) {
      if (!committed.contains(entry.getKey())) continue;
      for (Wal.Record r : entry.getValue()) {
        if (r.lsn() <= maxLsn) continue;
        if (r.type() == Wal.Type.CREATE_TABLE || r.type() == Wal.Type.INSERT || r.type() == Wal.Type.UPDATE || r.type() == Wal.Type.DELETE) {
          database.applyRecoverySql(r.payload());
        }
        maxLsn = Math.max(maxLsn, r.lsn());
      }
    }
    if (maxLsn > database.lastAppliedLsn()) {
      database.setLastAppliedLsn(maxLsn);
      database.persistForTransactionCommit();
    }
    if (!records.isEmpty()) wal.compact(maxLsn);
  }

  private void requireActive() {
    if (state != State.ACTIVE) throw new IllegalStateException("transaction is not active: " + state);
  }
}
