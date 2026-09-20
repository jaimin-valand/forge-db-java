# Troubleshooting

## `java` or `mvn` is not found

Install JDK 21+ and Maven 3.9+ and verify:

```bash
java -version
mvn -version
```

## Build succeeds but the CLI does not start

Rebuild from the repository root:

```bash
mvn -q -DskipTests package
```

Then launch the CLI using `target/classes`.

## Recovery reports WAL corruption

Do not delete the WAL or database files blindly. Completed-record corruption is intentionally treated differently from an incomplete final WAL tail. Preserve the files when investigating a recovery failure.

## A page fails checksum validation

Treat this as a storage-integrity failure. Use the relevant test or fault-injection scenario to reproduce the condition rather than bypassing validation.

## A query is unexpectedly slow

Use:

```sql
EXPLAIN SELECT ...;
```

Then inspect `:stats` and `:metrics` to determine whether the workload is scanning or using an index.

## Tests appear to be missing dependencies

The repository includes dependency-free release-gate scripts for core scenarios. Maven/JUnit tests require Maven to resolve the declared test dependency.
