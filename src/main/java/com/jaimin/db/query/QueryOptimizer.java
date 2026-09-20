package com.jaimin.db.query;

import java.util.*;
import com.jaimin.db.sql.SqlStatement;

/**
 * Deterministic educational cost model. It compares physical access paths and
 * deliberately exposes its assumptions instead of pretending to be a full
 * production optimizer.
 */
public final class QueryOptimizer {
    public QueryPlan choose(String table, List<SqlStatement.Condition> conditions,
                            Set<String> indexedColumns, TableStatistics stats) {
        SqlStatement.Condition equality = conditions.stream()
                .filter(c -> "=".equals(c.operator()) && indexedColumns.contains(c.column()))
                .findFirst().orElse(null);

        double scanRows = stats.rowCount();
        double scanCost = Math.max(1.0, scanRows);
        List<PlanCandidate> candidates = new ArrayList<>();
        candidates.add(new PlanCandidate("TABLE_SCAN", scanRows, scanCost,
                "sequentially inspect visible rows"));

        if (equality != null) {
            double estimatedRows = Math.max(1.0, stats.estimatedEqualityRows(equality.column()));
            double height = Math.max(1.0, Math.ceil(log2(Math.max(2, stats.rowCount()))));
            double indexCost = height + estimatedRows;
            candidates.add(new PlanCandidate("INDEX_LOOKUP", estimatedRows, indexCost,
                    "B+ tree equality lookup + fetch estimated matches"));
            if (indexCost < scanCost) {
                return new QueryPlan(QueryPlan.Type.INDEX_LOOKUP, table, equality.column(), equality.value(),
                        "selected lowest-cost equality access path",
                        estimatedRows, indexCost, candidates);
            }
            return new QueryPlan(QueryPlan.Type.TABLE_SCAN, table, equality.column(), equality.value(),
                    "index candidate rejected because estimated scan cost is lower",
                    estimatedRows, scanCost, candidates);
        }

        String reason = conditions.isEmpty() ? "no usable predicate" : "no equality predicate on an indexed column";
        return new QueryPlan(QueryPlan.Type.TABLE_SCAN, table, null, null,
                "selected sequential scan; " + reason, scanRows, scanCost, candidates);
    }

    /** Join strategy comparison used by the next-generation planner. */
    public JoinPlan chooseJoin(TableStatistics left, TableStatistics right,
                               String leftColumn, String rightColumn,
                               boolean rightIndexed) {
        double nested = Math.max(1.0, (double) left.rowCount() * Math.max(1, right.rowCount()));
        double rightIndex = rightIndexed
                ? Math.max(1.0, left.rowCount()) * Math.max(1.0, Math.ceil(log2(Math.max(2, right.rowCount()))))
                : Double.POSITIVE_INFINITY;
        List<JoinPlan.Candidate> candidates = List.of(
                new JoinPlan.Candidate("NESTED_LOOP_JOIN", nested, "left rows × right rows"),
                new JoinPlan.Candidate("INDEX_NESTED_LOOP_JOIN", rightIndex,
                        rightIndexed ? "probe right-side B+ tree per left row" : "right join column has no usable index")
        );
        if (rightIndex < nested) return new JoinPlan("INDEX_NESTED_LOOP_JOIN", rightIndex, candidates);
        return new JoinPlan("NESTED_LOOP_JOIN", nested, candidates);
    }

    private static double log2(double x) { return Math.log(x) / Math.log(2); }

    public record JoinPlan(String strategy, double estimatedCost, List<Candidate> candidates) {
        public JoinPlan { candidates = List.copyOf(candidates); }
        public record Candidate(String strategy, double estimatedCost, String rationale) {}
    }
}
