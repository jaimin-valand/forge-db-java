package com.jaimin.db;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** Phase 23 correctness/performance instrumentation checks. */
public class PerformanceEngineeringTest {
  @Test void metricsCaptureQueryTimeAndCounters() throws Exception {
    Path dir = Files.createTempDirectory("forge-perf-");
    try (Database db = new Database(dir.resolve("db"))) {
      SqlEngine sql = new SqlEngine(db);
      sql.execute("CREATE TABLE t (id INT PRIMARY KEY, category TEXT);");
      for (int i = 1; i <= 50; i++) db.insert("t", java.util.List.of(Integer.toString(i), "c" + (i % 5)));
      sql.execute("SELECT * FROM t WHERE category = 'c1';");
      PerformanceSnapshot snapshot = db.metrics().performanceSnapshot();
      assertTrue(snapshot.statements() >= 2);
      assertTrue(snapshot.selects() >= 1);
      assertTrue(snapshot.queryNanos() > 0);
    }
  }
}
