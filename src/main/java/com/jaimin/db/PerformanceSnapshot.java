package com.jaimin.db;

import java.util.LinkedHashMap;
import java.util.Map;

/** Immutable point-in-time performance counters used by benchmarks and diagnostics. */
public record PerformanceSnapshot(
    long statements,
    long selects,
    long inserts,
    long updates,
    long deletes,
    long indexLookups,
    long tableScans,
    long queryNanos,
    long storageReads,
    long storageWrites) {
  public Map<String, Long> asMap() {
    Map<String, Long> m = new LinkedHashMap<>();
    m.put("statements", statements); m.put("selects", selects);
    m.put("inserts", inserts); m.put("updates", updates); m.put("deletes", deletes);
    m.put("index_lookups", indexLookups); m.put("table_scans", tableScans);
    m.put("query_nanos", queryNanos); m.put("storage_reads", storageReads);
    m.put("storage_writes", storageWrites);
    return m;
  }
}
