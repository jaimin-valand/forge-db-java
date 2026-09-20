package com.jaimin.db.query;

import java.util.*;

/**
 * Lightweight in-memory statistics used by ForgeDB's educational cost model.
 * Statistics are derived from currently visible rows; they are intentionally
 * simple and deterministic rather than pretending to be a production optimizer.
 */
public final class TableStatistics {
    private final String table;
    private final int rowCount;
    private final Map<String, Integer> distinctCounts;

    public TableStatistics(String table, int rowCount, Map<String, Integer> distinctCounts) {
        this.table = Objects.requireNonNull(table);
        this.rowCount = Math.max(0, rowCount);
        this.distinctCounts = Map.copyOf(distinctCounts);
    }

    public String table() { return table; }
    public int rowCount() { return rowCount; }
    public int distinctCount(String column) { return distinctCounts.getOrDefault(column, 0); }
    public Map<String, Integer> distinctCounts() { return distinctCounts; }

    public double equalitySelectivity(String column) {
        int distinct = distinctCount(column);
        if (rowCount == 0) return 0.0;
        return distinct <= 0 ? 1.0 : Math.min(1.0, 1.0 / distinct);
    }

    public int estimatedEqualityRows(String column) {
        return (int) Math.ceil(rowCount * equalitySelectivity(column));
    }

    @Override public String toString() {
        return "TableStatistics{" + table + ", rows=" + rowCount + ", distinct=" + distinctCounts + "}";
    }
}
