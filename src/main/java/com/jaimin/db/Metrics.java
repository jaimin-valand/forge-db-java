package com.jaimin.db;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/** Lightweight in-process observability counters for ForgeDB. */
public final class Metrics {
  private final AtomicLong statements = new AtomicLong();
  private final AtomicLong selects = new AtomicLong();
  private final AtomicLong inserts = new AtomicLong();
  private final AtomicLong updates = new AtomicLong();
  private final AtomicLong deletes = new AtomicLong();
  private final AtomicLong transactions = new AtomicLong();
  private final AtomicLong commits = new AtomicLong();
  private final AtomicLong rollbacks = new AtomicLong();
  private final AtomicLong indexLookups = new AtomicLong();
  private final AtomicLong tableScans = new AtomicLong();
  private final AtomicLong queryNanos = new AtomicLong();
  private final AtomicLong storageReads = new AtomicLong();
  private final AtomicLong storageWrites = new AtomicLong();

  public void statement(){ statements.incrementAndGet(); }
  public void select(){ selects.incrementAndGet(); }
  public void insert(){ inserts.incrementAndGet(); }
  public void update(){ updates.incrementAndGet(); }
  public void delete(){ deletes.incrementAndGet(); }
  public void transaction(){ transactions.incrementAndGet(); }
  public void commit(){ commits.incrementAndGet(); }
  public void rollback(){ rollbacks.incrementAndGet(); }
  public void indexLookup(){ indexLookups.incrementAndGet(); }
  public void tableScan(){ tableScans.incrementAndGet(); }
  public void queryNanos(long nanos){ queryNanos.addAndGet(Math.max(0, nanos)); }
  public void storageRead(){ storageReads.incrementAndGet(); }
  public void storageWrite(){ storageWrites.incrementAndGet(); }
  public PerformanceSnapshot performanceSnapshot(){
    return new PerformanceSnapshot(statements.get(), selects.get(), inserts.get(), updates.get(), deletes.get(),
        indexLookups.get(), tableScans.get(), queryNanos.get(), storageReads.get(), storageWrites.get());
  }

  public Map<String,Long> snapshot(){
    Map<String,Long> m=new LinkedHashMap<>();
    m.put("statements",statements.get()); m.put("selects",selects.get());
    m.put("inserts",inserts.get()); m.put("updates",updates.get()); m.put("deletes",deletes.get());
    m.put("transactions",transactions.get()); m.put("commits",commits.get()); m.put("rollbacks",rollbacks.get());
    m.put("index_lookups",indexLookups.get()); m.put("table_scans",tableScans.get());
    m.put("query_nanos",queryNanos.get()); m.put("storage_reads",storageReads.get()); m.put("storage_writes",storageWrites.get());
    return m;
  }
}
