# ForgeDB Benchmark Suite

ForgeDB includes a small reproducible micro-benchmark suite under
`com.jaimin.db.benchmark.BenchmarkRunner`.

## Workloads

- `table_scan` - scans all rows through the storage API.
- `indexed_lookup` - equality lookup through the secondary index.
- `filtered_scan` - SQL `SELECT` with a filter over the execution pipeline.
- `count_scan` - counts visible rows.

Each workload performs a configurable warm-up followed by measured iterations.
The runner reports average, p50, p95, p99, minimum and maximum latency plus
operations/second.

## Run

With Maven:

```bash
mvn -q -DskipTests package
java -cp target/classes com.jaimin.db.benchmark.BenchmarkRunner
```

Optional parameters:

```bash
java -Dforge.benchmark.rows=10000 \
     -Dforge.benchmark.warmup=50 \
     -Dforge.benchmark.iterations=250 \
     -cp target/classes com.jaimin.db.benchmark.BenchmarkRunner
```

The runner writes `benchmark/results/latest.csv`. Benchmark databases and
results are intentionally ignored by Git so measured output does not pollute
source control.

## Interpretation

These are **micro-benchmarks**, not claims about production database
performance. Results depend on CPU, JVM version, filesystem, garbage
collection, dataset size and operating-system load. Compare runs on the same
environment when evaluating an optimisation.

The most useful comparison is the relative behaviour of indexed equality
lookup versus a full scan, followed by latency percentiles for the execution
pipeline.
