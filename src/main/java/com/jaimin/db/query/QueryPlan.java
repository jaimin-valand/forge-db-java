package com.jaimin.db.query;

import java.util.*;

/** Immutable physical-plan decision with cost-model evidence for EXPLAIN. */
public record QueryPlan(
        Type type,
        String table,
        String predicateColumn,
        String predicateValue,
        String detail,
        double estimatedRows,
        double estimatedCost,
        List<PlanCandidate> candidates) {
    public enum Type { TABLE_SCAN, INDEX_LOOKUP }

    public QueryPlan {
        candidates = candidates == null ? List.of() : List.copyOf(candidates);
    }

    public QueryPlan(Type type, String table, String predicateColumn, String predicateValue, String detail) {
        this(type, table, predicateColumn, predicateValue, detail, -1, Double.NaN, List.of());
    }

    @Override public String toString() {
        StringBuilder b = new StringBuilder("Plan{").append(type)
                .append(", table=").append(table);
        if (predicateColumn != null) b.append(", predicate=").append(predicateColumn)
                .append("='").append(predicateValue).append("'");
        b.append(", estimatedRows=").append(fmt(estimatedRows))
         .append(", estimatedCost=").append(fmt(estimatedCost))
         .append(", ").append(detail);
        if (!candidates.isEmpty()) b.append(", candidates=").append(candidates);
        return b.append('}').toString();
    }

    private static String fmt(double v) {
        if (Double.isNaN(v)) return "n/a";
        if (Double.isInfinite(v)) return "INF";
        return String.format(Locale.ROOT, "%.2f", v);
    }
}
