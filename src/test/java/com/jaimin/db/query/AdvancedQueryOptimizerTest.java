package com.jaimin.db.query;

import java.util.*;
import com.jaimin.db.sql.SqlStatement;

/** Dependency-free optimizer regression tests. */
public final class AdvancedQueryOptimizerTest {
  public static void main(String[] args) {
    QueryPlanner planner = new QueryPlanner();
    TableStatistics stats = new TableStatistics("users", 10_000, Map.of("id", 10_000, "country", 5));
    QueryPlan indexed = planner.plan("users", List.of(new SqlStatement.Condition("id", "=", "42")), Set.of("id"), stats);
    assert indexed.type() == QueryPlan.Type.INDEX_LOOKUP : indexed;
    assert indexed.candidates().size() == 2 : indexed;
    assert indexed.estimatedRows() == 1.0 : indexed;

    QueryPlan scan = planner.plan("users", List.of(new SqlStatement.Condition("country", "=", "UK")), Set.of("id"), stats);
    assert scan.type() == QueryPlan.Type.TABLE_SCAN : scan;
    assert scan.detail().contains("no equality predicate") == false || scan.detail().contains("indexed") : scan;

    var join = planner.planJoin(
        new TableStatistics("users", 100, Map.of("id", 100)),
        new TableStatistics("orders", 10_000, Map.of("user_id", 100)),
        "id", "user_id", true);
    assert join.strategy().equals("INDEX_NESTED_LOOP_JOIN") : join;
    assert join.candidates().size() == 2 : join;
    System.out.println("PASS: advanced query optimizer tests");
  }
}
