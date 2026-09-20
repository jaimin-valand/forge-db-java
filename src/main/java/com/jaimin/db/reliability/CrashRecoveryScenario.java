package com.jaimin.db.reliability;

import com.jaimin.db.Database;
import java.nio.file.Path;

/** Small process entry point used by the integration test to simulate a crash. */
public final class CrashRecoveryScenario {
  private CrashRecoveryScenario() {}

  public static void main(String[] args) throws Exception {
    if (args.length != 2) throw new IllegalArgumentException("usage: CrashRecoveryScenario <db> <mode>");
    Path dbPath = Path.of(args[0]);
    String mode = args[1];
    try (var ignored = new AutoCloseable() { public void close() {} }) {
      Database db = new Database(dbPath);
      if ("crash-after-wal-commit".equals(mode)) {
        db.begin();
        db.createTable("events", java.util.List.of("id", "message"));
        db.recordCreate("CREATE TABLE events (id TEXT, message TEXT)");
        db.insert("events", java.util.List.of("1", "before-crash"));
        db.recordInsert("INSERT INTO events VALUES (1, 'before-crash')");
        db.commit(); // FaultInjector halts before snapshot checkpoint.
      } else if ("verify-recovery".equals(mode)) {
        if (!db.tables().contains("events")) throw new AssertionError("events table was not recovered");
        if (db.count("events") != 1) throw new AssertionError("recovered row count != 1");
      } else {
        throw new IllegalArgumentException("unknown mode: " + mode);
      }
    }
  }
}
