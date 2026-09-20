package com.jaimin.db.reliability;

import java.nio.file.*;
import java.util.*;

/**
 * Process-level crash/recovery smoke test. Uses a child JVM so the database
 * can be terminated abruptly without terminating the test runner.
 */
public final class CrashRecoveryIntegration {
  public static void main(String[] args) throws Exception {
    Path dir = Files.createTempDirectory("forgedb-crash-");
    Path db = dir.resolve("crash.db");
    String cp = System.getProperty("java.class.path");

    Process crashed = new ProcessBuilder(
        javaBin(), "-Dforge.fault.afterWalCommit=HALT", "-cp", cp,
        CrashRecoveryScenario.class.getName(), db.toString(), "crash-after-wal-commit")
        .redirectErrorStream(true).start();
    int exit = crashed.waitFor();
    if (exit != 86) throw new AssertionError("expected simulated crash exit 86, got " + exit);

    Process recovered = new ProcessBuilder(
        javaBin(), "-cp", cp,
        CrashRecoveryScenario.class.getName(), db.toString(), "verify-recovery")
        .redirectErrorStream(true).inheritIO().start();
    int recoveryExit = recovered.waitFor();
    if (recoveryExit != 0) throw new AssertionError("recovery verification failed: " + recoveryExit);

    System.out.println("PASS: crash after WAL COMMIT recovered committed state");
    deleteRecursively(dir);
  }

  private static String javaBin() {
    return Path.of(System.getProperty("java.home"), "bin", "java").toString();
  }

  private static void deleteRecursively(Path root) throws Exception {
    if (!Files.exists(root)) return;
    try (var walk = Files.walk(root)) {
      walk.sorted(Comparator.reverseOrder()).forEach(p -> {
        try { Files.deleteIfExists(p); } catch (Exception e) { throw new RuntimeException(e); }
      });
    }
  }
}
