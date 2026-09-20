package com.jaimin.db.benchmark;

import com.jaimin.db.Database;
import com.jaimin.db.SqlEngine;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Reproducible micro-benchmark suite for ForgeDB.
 *
 * The benchmark intentionally measures the database API rather than JVM startup.
 * Results are emitted as CSV and a human-readable console summary.
 */
public final class BenchmarkRunner {
  private static final int ROWS = Integer.getInteger("forge.benchmark.rows", 2_000);
  private static final int WARMUP = Integer.getInteger("forge.benchmark.warmup", 25);
  private static final int ITERATIONS = Integer.getInteger("forge.benchmark.iterations", 100);

  private BenchmarkRunner() {}

  public static void main(String[] args) throws Exception {
    Path workDir = args.length > 0 ? Path.of(args[0]) : Path.of("benchmark", "results");
    Files.createDirectories(workDir);
    Path dbPath = workDir.resolve("forge-benchmark.db");
    Files.deleteIfExists(dbPath);
    Files.deleteIfExists(dbPath.resolveSibling(dbPath.getFileName() + ".wal"));

    Database db = new Database(dbPath);
    SqlEngine sql = new SqlEngine(db);
    sql.execute("CREATE TABLE bench (id INT PRIMARY KEY, category TEXT, payload TEXT);");

    db.begin();
    try {
      for (int i = 1; i <= ROWS; i++) {
        db.insert("bench", List.of(Integer.toString(i), "c" + (i % 10), "payload-" + i));
        db.recordInsert("INSERT INTO bench VALUES (" + i + ", 'c" + (i % 10) + "', 'payload-" + i + "')");
      }
      db.commit();
    } catch (Exception e) {
      try { db.rollback(); } catch (Exception ignored) {}
      throw e;
    }

    List<Result> results = new ArrayList<>();
    results.add(run("table_scan", "rows=" + ROWS, WARMUP, ITERATIONS,
        () -> { db.selectAll("bench"); }));
    results.add(run("indexed_lookup", "rows=" + ROWS, WARMUP, ITERATIONS,
        () -> { db.selectWhere("bench", "id", Integer.toString(ROWS / 2)); }));
    results.add(run("filtered_scan", "rows=" + ROWS, WARMUP, ITERATIONS,
        () -> { sql.execute("SELECT * FROM bench WHERE category = 'c5';"); }));
    results.add(run("count_scan", "rows=" + ROWS, WARMUP, ITERATIONS,
        () -> { db.count("bench"); }));
    results.add(run("ordered_limit", "rows=" + ROWS, WARMUP, ITERATIONS,
        () -> { sql.execute("SELECT * FROM bench ORDER BY id DESC LIMIT 25 OFFSET 10;"); }));
    results.add(run("group_count", "rows=" + ROWS, WARMUP, ITERATIONS,
        () -> { sql.execute("SELECT category, COUNT(*) FROM bench GROUP BY category;"); }));

    Path csv = workDir.resolve("latest.csv");
    try (BufferedWriter out = Files.newBufferedWriter(csv)) {
      out.write("benchmark,workload,rows,iterations,avg_us,p50_us,p95_us,p99_us,min_us,max_us,ops_per_sec\n");
      for (Result r : results) out.write(r.csv() + "\n");
    }

    System.out.println("ForgeDB Benchmark Suite");
    System.out.println("rows=" + ROWS + ", warmup=" + WARMUP + ", iterations=" + ITERATIONS);
    System.out.println();
    System.out.printf("%-18s %10s %10s %10s %10s %12s%n", "benchmark", "avg(us)", "p50(us)", "p95(us)", "p99(us)", "ops/sec");
    for (Result r : results) {
      System.out.printf("%-18s %10.2f %10.2f %10.2f %10.2f %12.2f%n",
          r.name, r.avgUs, r.p50Us, r.p95Us, r.p99Us, r.opsPerSec);
    }
    System.out.println();
    System.out.println("CSV: " + csv.toAbsolutePath());
  }

  private static Result run(String name, String workload, int warmup, int iterations, ThrowingRunnable action) throws Exception {
    for (int i = 0; i < warmup; i++) action.run();
    long[] samples = new long[iterations];
    for (int i = 0; i < iterations; i++) {
      long start = System.nanoTime();
      action.run();
      samples[i] = System.nanoTime() - start;
    }
    Arrays.sort(samples);
    double avgNs = Arrays.stream(samples).average().orElse(0);
    return new Result(name, workload, samples.length,
        nanosToMicros(avgNs), nanosToMicros(percentile(samples, .50)),
        nanosToMicros(percentile(samples, .95)), nanosToMicros(percentile(samples, .99)),
        nanosToMicros(samples[0]), nanosToMicros(samples[samples.length - 1]),
        1_000_000_000d / avgNs);
  }

  private static long percentile(long[] sorted, double p) {
    int index = (int) Math.ceil(p * sorted.length) - 1;
    return sorted[Math.max(0, Math.min(sorted.length - 1, index))];
  }

  private static double nanosToMicros(double ns) { return ns / 1_000d; }

  @FunctionalInterface
  private interface ThrowingRunnable { void run() throws Exception; }

  private record Result(String name, String workload, int iterations, double avgUs,
                        double p50Us, double p95Us, double p99Us,
                        double minUs, double maxUs, double opsPerSec) {
    String csv() {
      return String.format(Locale.ROOT, "%s,%s,%d,%d,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f",
          name, workload, ROWS, iterations, avgUs, p50Us, p95Us, p99Us, minUs, maxUs, opsPerSec);
    }
  }
}
