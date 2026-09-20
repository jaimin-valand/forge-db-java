# Getting Started

ForgeDB is a Java 21 embedded SQL database engine. It is intentionally small, inspectable, and designed to demonstrate database internals rather than provide production PostgreSQL/MySQL compatibility.

## Requirements

- JDK 21+
- Maven 3.9+
- POSIX shell for the optional helper scripts

## Build

```bash
mvn -q -DskipTests package
```

The packaged JAR is written to `target/forge-db-1.0.0.jar`.

## Run the CLI

```bash
java -cp target/classes com.jaimin.db.ForgeDbCli
```

Use `:help` inside the shell to inspect supported meta-commands.

## First database

```sql
CREATE TABLE users (
  id INT PRIMARY KEY,
  email TEXT UNIQUE,
  name TEXT NOT NULL
);

INSERT INTO users VALUES (1, 'jaimin@example.com', 'Jaimin');
SELECT * FROM users;
EXPLAIN SELECT * FROM users WHERE id = 1;
```

## Release gate

The dependency-free release gate can be run with:

```bash
bash scripts/test-gate.sh
```

The project also contains Maven/JUnit tests under `src/test/java` when the Maven test dependency is available.

## Project layout

```text
src/main/java/com/jaimin/db/
  Database.java              database facade and lifecycle
  ForgeDbCli.java            interactive shell
  TransactionManager.java    transaction/WAL coordination
  Wal.java                   write-ahead log
  storage/                   pages, buffer pool and persistence
  index/                     B+ tree indexes
  sql/                       lexer, parser and statements
  query/                     planning and execution operators
  observability/             logging, traces, health and latency
  reliability/               fault injection and recovery scenarios
  benchmark/                 repeatable performance workloads

docs/                        architecture and subsystem guides
scripts/                     release/CLI test helpers
benchmark/                   benchmark output location
```
