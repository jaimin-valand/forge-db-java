package com.jaimin.db;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Dependency-boundary and state-ownership regression checks. */
public final class ArchitectureAuditTest {
  public static void main(String[] args) throws Exception {
    if (!ArchitectureAudit.passes()) throw new AssertionError("architecture rules failed");

    Path sourceRoot = Path.of("src/main/java/com/jaimin/db");
    assertNoImports(sourceRoot.resolve("storage"), "com.jaimin.db.sql.");
    assertNoImports(sourceRoot.resolve("query"), "com.jaimin.db.ForgeDbCli");
    assertNoImports(sourceRoot.resolve("sql"), "com.jaimin.db.storage.");
    assertNoImports(sourceRoot.resolve("observability"), "com.jaimin.db.query.");

    Path db = Files.createTempFile("forge-architecture-", ".db");
    Files.deleteIfExists(db);
    Database database = new Database(db);
    try {
      if (database.transactionState() != TransactionManager.State.IDLE) throw new AssertionError("initial state");
      boolean rejected = false;
      try { database.commit(); } catch (IllegalStateException expected) { rejected = true; }
      if (!rejected) throw new AssertionError("commit outside transaction must be rejected");
      if (database.inTransaction()) throw new AssertionError("failed commit changed transaction state");
      database.begin();
      if (database.transactionState() != TransactionManager.State.ACTIVE) throw new AssertionError("begin state");
      database.rollback();
      if (database.transactionState() != TransactionManager.State.ABORTED) throw new AssertionError("rollback state");
      if (database.inTransaction()) throw new AssertionError("rollback left active state");
    } finally {
      Files.deleteIfExists(db);
      Files.deleteIfExists(Path.of(db + ".wal"));
    }
    System.out.println("PASS: architecture boundaries and transaction state ownership");
  }

  private static void assertNoImports(Path root, String forbidden) throws Exception {
    try (var stream = Files.walk(root)) {
      for (Path p : stream.filter(f -> f.toString().endsWith(".java")).toList()) {
        String source = Files.readString(p);
        if (source.contains("import " + forbidden)) {
          throw new AssertionError("forbidden dependency in " + p + ": " + forbidden);
        }
      }
    }
  }
}
