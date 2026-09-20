package com.jaimin.db.query;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;

final class QueryPlannerStatisticsTest {
  @Test void chooses_index_when_selective() {
    var stats = new TableStatistics("users", 10_000, Map.of("id", 10_000, "country", 5));
    var plan = new QueryPlanner().plan("users", "id", "42", Set.of("id"), stats);
    assertEquals(QueryPlan.Type.INDEX_LOOKUP, plan.type());
    assertTrue(plan.detail().contains("scanCost="));
    assertTrue(plan.detail().contains("indexCost="));
  }

  @Test void keeps_scan_when_index_is_not_selective_enough() {
    var stats = new TableStatistics("users", 10, Map.of("country", 1));
    var plan = new QueryPlanner().plan("users", "country", "UK", Set.of("country"), stats);
    assertEquals(QueryPlan.Type.TABLE_SCAN, plan.type());
    assertTrue(plan.detail().contains("cost model prefers sequential scan"));
  }

  @Test void non_equality_does_not_use_equality_index() {
    var stats = new TableStatistics("users", 10_000, Map.of("id", 10_000));
    var plan = new QueryPlanner().plan("users", "id", "42", Set.of(), stats);
    assertEquals(QueryPlan.Type.TABLE_SCAN, plan.type());
  }
}
