package com.jaimin.db.reliability;

import com.jaimin.db.storage.Page;
import com.jaimin.db.storage.PageFile;
import com.jaimin.db.storage.PageType;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Deterministic resilience scorecard: crash, torn WAL tail and page corruption. */
public final class ChaosResilienceTest {
  public static void main(String[] args) throws Exception {
    int passed = 0;
    passed += processCrashScenario("committed-crash", "verify-committed", "AFTER_WAL_COMMIT");
    passed += processCrashScenario("uncommitted-crash", "verify-uncommitted", "AFTER_MUTATION");
    passed += tornWalTailScenario();
    passed += pageCorruptionScenario();
    System.out.println("CHAOS SCORECARD: " + passed + "/4 scenarios passed");
    if (passed != 4) throw new AssertionError("chaos resilience gate failed");
  }

  private static int processCrashScenario(String crashMode, String verifyMode, String point) throws Exception {
    Path dir = Files.createTempDirectory("forgedb-chaos-");
    Path db = dir.resolve("chaos.db");
    String cp = System.getProperty("java.class.path");
    Process crashed = new ProcessBuilder(javaBin(),
        "-Dforge.fault.point=" + point, "-Dforge.fault.action=HALT", "-cp", cp,
        ChaosScenario.class.getName(), db.toString(), crashMode, "1")
        .redirectErrorStream(true).start();
    int exit = crashed.waitFor();
    if (exit != 86) throw new AssertionError(crashMode + " expected exit 86, got " + exit);
    Process recovered = new ProcessBuilder(javaBin(), "-cp", cp,
        ChaosScenario.class.getName(), db.toString(), verifyMode, "1")
        .redirectErrorStream(true).inheritIO().start();
    int verify = recovered.waitFor();
    deleteRecursively(dir);
    if (verify != 0) throw new AssertionError(verifyMode + " failed with " + verify);
    System.out.println("PASS: " + crashMode + " -> " + verifyMode);
    return 1;
  }

  private static int tornWalTailScenario() throws Exception {
    Path dir = Files.createTempDirectory("forgedb-torn-wal-");
    Path db = dir.resolve("tail.db");
    Path wal = dir.resolve("tail.db.wal");
    String cp = System.getProperty("java.class.path");
    Process crashed = new ProcessBuilder(javaBin(), "-Dforge.fault.point=AFTER_WAL_COMMIT", "-Dforge.fault.action=HALT",
        "-cp", cp, ChaosScenario.class.getName(), db.toString(), "committed-crash", "2")
        .redirectErrorStream(true).start();
    if (crashed.waitFor() != 86) throw new AssertionError("torn WAL setup did not crash");
    long size = Files.size(wal);
    if (size < 5) throw new AssertionError("WAL unexpectedly small");
    // Preserve the valid COMMIT record, then append a deliberately torn next record.
    try (OutputStream out = Files.newOutputStream(wal, StandardOpenOption.APPEND)) {
      out.write(new byte[] {0x46, 0x44, 0x57});
    }
    Process recovered = new ProcessBuilder(javaBin(), "-cp", cp, ChaosScenario.class.getName(), db.toString(), "verify-committed", "2")
        .redirectErrorStream(true).inheritIO().start();
    int verify = recovered.waitFor();
    deleteRecursively(dir);
    if (verify != 0) throw new AssertionError("torn WAL recovery failed");
    System.out.println("PASS: incomplete final WAL tail tolerated during recovery");
    return 1;
  }

  private static int pageCorruptionScenario() throws Exception {
    Path dir = Files.createTempDirectory("forgedb-page-corrupt-");
    Path pageFile = dir.resolve("pages.dat");
    try (PageFile pages = new PageFile(pageFile)) {
      Page page = pages.allocate(PageType.DATA);
      page.writePayload("integrity".getBytes());
      pages.write(page);
    }
    byte[] bytes = Files.readAllBytes(pageFile);
    bytes[Page.HEADER_SIZE] ^= 0x01;
    Files.write(pageFile, bytes);
    boolean detected = false;
    try (PageFile pages = new PageFile(pageFile)) {
      pages.read(0).readPayload();
    } catch (IllegalStateException expected) { detected = true; }
    deleteRecursively(dir);
    if (!detected) throw new AssertionError("page corruption was not detected");
    System.out.println("PASS: page checksum corruption detected");
    return 1;
  }

  private static String javaBin() { return Path.of(System.getProperty("java.home"), "bin", "java").toString(); }
  private static void deleteRecursively(Path root) throws Exception {
    if (!Files.exists(root)) return;
    try (var walk = Files.walk(root)) {
      walk.sorted(Comparator.reverseOrder()).forEach(p -> { try { Files.deleteIfExists(p); } catch (Exception e) { throw new RuntimeException(e); } });
    }
  }
}
