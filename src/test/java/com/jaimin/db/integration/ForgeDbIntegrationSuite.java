package com.jaimin.db.integration;

import com.jaimin.db.Database;
import com.jaimin.db.SqlEngine;
import java.nio.file.*;
import java.util.*;

/**
 * Dependency-free end-to-end release-gate suite.
 *
 * It exercises the public SQL path across parsing, planning, indexing,
 * constraints, transactions, persistence, schema evolution and recovery.
 */
public final class ForgeDbIntegrationSuite {
  public static void main(String[] args) throws Exception {
    Path dir = Files.createTempDirectory("forgedb-integration-");
    try {
      Path dbPath = dir.resolve("forge.db");
      Database db = new Database(dbPath);
      SqlEngine sql = new SqlEngine(db);

      sql.execute("CREATE TABLE users (id INT PRIMARY KEY, name TEXT NOT NULL, age INT)");
      sql.execute("INSERT INTO users VALUES (1, 'Jaimin', 28)");
      sql.execute("INSERT INTO users VALUES (2, 'Alex', 31)");
      sql.execute("INSERT INTO users VALUES (3, 'Mina', 24)");

      assertEquals(3, db.count("users"), "initial row count");
      assertEquals(1, sql.execute("SELECT name, age FROM users WHERE age >= 30").size(), "projection/filter");
      assertTrue(sql.execute("EXPLAIN SELECT * FROM users WHERE id = 2").getFirst().getFirst().contains("INDEX_LOOKUP"), "planner chose index");

      boolean duplicateRejected = false;
      try { sql.execute("INSERT INTO users VALUES (1, 'Duplicate', 40)"); }
      catch (IllegalArgumentException expected) { duplicateRejected = true; }
      assertTrue(duplicateRejected, "primary-key violation rejected");

      sql.execute("BEGIN");
      sql.execute("UPDATE users SET age = 29 WHERE id = 1");
      sql.execute("DELETE FROM users WHERE id = 3");
      assertEquals("29", sql.execute("SELECT age FROM users WHERE id = 1").getFirst().getFirst(), "transactional update visible");
      assertEquals(2, db.count("users"), "transactional delete visible");
      sql.execute("ROLLBACK");
      assertEquals("28", sql.execute("SELECT age FROM users WHERE id = 1").getFirst().getFirst(), "rollback restored update");
      assertEquals(3, db.count("users"), "rollback restored delete");

      sql.execute("BEGIN");
      sql.execute("UPDATE users SET age = 30 WHERE id = 1");
      sql.execute("DELETE FROM users WHERE id = 3");
      sql.execute("COMMIT");
      assertEquals("30", sql.execute("SELECT age FROM users WHERE id = 1").getFirst().getFirst(), "committed update");
      assertEquals(2, db.count("users"), "committed delete");

      sql.execute("ALTER TABLE users ADD COLUMN country TEXT DEFAULT 'UK'");
      assertEquals(2L, db.schemaVersion(), "schema version");
      assertEquals("UK", sql.execute("SELECT country FROM users WHERE id = 1").getFirst().getFirst(), "migration default");

      Database reopened = new Database(dbPath);
      SqlEngine reopenedSql = new SqlEngine(reopened);
      assertEquals(2, reopened.count("users"), "row count after reload");
      assertEquals("30", reopenedSql.execute("SELECT age FROM users WHERE id = 1").getFirst().getFirst(), "committed update after reload");
      assertEquals("UK", reopenedSql.execute("SELECT country FROM users WHERE id = 2").getFirst().getFirst(), "schema migration after reload");
      assertEquals(2L, reopened.schemaVersion(), "schema version after reload");

      System.out.println("PASS: ForgeDB end-to-end integration suite");
      System.out.println("  SQL parsing/execution       PASS");
      System.out.println("  index-aware planning       PASS");
      System.out.println("  constraints                PASS");
      System.out.println("  rollback semantics         PASS");
      System.out.println("  commit durability          PASS");
      System.out.println("  schema migration           PASS");
      System.out.println("  database reload            PASS");
    } finally {
      deleteRecursively(dir);
    }
  }

  private static void assertTrue(boolean value, String message) {
    if (!value) throw new AssertionError(message);
  }

  private static void assertEquals(Object expected, Object actual, String message) {
    if (!Objects.equals(expected, actual)) {
      throw new AssertionError(message + ": expected=" + expected + ", actual=" + actual);
    }
  }

  private static void deleteRecursively(Path root) throws Exception {
    if (!Files.exists(root)) return;
    try (var walk = Files.walk(root)) {
      walk.sorted(Comparator.reverseOrder()).forEach(path -> {
        try { Files.deleteIfExists(path); }
        catch (Exception e) { throw new RuntimeException(e); }
      });
    }
  }
}
