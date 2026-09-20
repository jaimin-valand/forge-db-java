package com.jaimin.db.query;

import java.util.*;
import com.jaimin.db.sql.SqlStatement;

/** Compatibility facade over the next-generation cost-based optimizer. */
public final class QueryPlanner {
    private final QueryOptimizer optimizer = new QueryOptimizer();

    public QueryPlan plan(String table, String column, String value, Set<String> indexedColumns) {
        return plan(table, column, value, indexedColumns,
                new TableStatistics(table, 1_000, Map.of(column == null ? "" : column, 1)));
    }

    public QueryPlan plan(String table, String column, String value, Set<String> indexedColumns,
                          TableStatistics stats) {
        List<SqlStatement.Condition> conditions = column == null ? List.of() :
                List.of(new SqlStatement.Condition(column, "=", value));
        return optimizer.choose(table, conditions, indexedColumns, stats);
    }

    public QueryPlan plan(String table, List<SqlStatement.Condition> conditions,
                          Set<String> indexedColumns, TableStatistics stats) {
        return optimizer.choose(table, conditions, indexedColumns, stats);
    }

    public QueryOptimizer.JoinPlan planJoin(TableStatistics left, TableStatistics right,
                                            String leftColumn, String rightColumn,
                                            boolean rightIndexed) {
        return optimizer.chooseJoin(left, right, leftColumn, rightColumn, rightIndexed);
    }
}
