package com.jaimin.db;

import com.jaimin.db.SqlEngine;
import java.nio.file.*;
import java.util.*;

/** Phase 31 semantic audit: constraints, numeric predicates, statement atomicity and rollback metadata. */
public final class CorrectnessAuditTest {
  public static void main(String[] args) throws Exception {
    Path dir=Files.createTempDirectory("forgedb-correctness-");
    Path file=dir.resolve("db.fdb");
    Database db=new Database(file); SqlEngine sql=new SqlEngine(db);
    sql.execute("CREATE TABLE users (id INT PRIMARY KEY, name TEXT UNIQUE, age INT NOT NULL)");
    sql.execute("INSERT INTO users VALUES (2, 'Bob', 20)");
    sql.execute("INSERT INTO users VALUES (10, 'Zoe', 30)");

    check(sql.execute("SELECT id FROM users WHERE id > 2").equals(List.of(List.of("10"))), "numeric comparison");
    check(sql.execute("SELECT id FROM users ORDER BY id ASC").equals(List.of(List.of("2"),List.of("10"))), "numeric order");

    boolean duplicate=false;
    try { sql.execute("UPDATE users SET name = 'Zoe'"); } catch (IllegalArgumentException e) { duplicate=true; }
    check(duplicate, "unique constraint rejects multi-row collision");
    check(sql.execute("SELECT name FROM users ORDER BY id ASC").equals(List.of(List.of("Bob"),List.of("Zoe"))), "failed update is atomic");

    long beforeSchema=db.schemaVersion();
    sql.execute("BEGIN");
    sql.execute("ALTER TABLE users ADD COLUMN city TEXT DEFAULT 'London'");
    check(db.schemaVersion()==beforeSchema+1, "schema version increments");
    sql.execute("ROLLBACK");
    check(db.schemaVersion()==beforeSchema, "rollback restores schema version");
    check(!db.columns("users").contains("city"), "rollback removes schema mutation");
    check(db.migrationHistory().isEmpty(), "rollback restores migration history");

    Database reloaded=new Database(file);
    check(reloaded.schemaVersion()==beforeSchema, "persisted schema version remains correct");
    check(!reloaded.columns("users").contains("city"), "reloaded schema remains correct");
    System.out.println("PASS: correctness audit - numeric semantics, uniqueness, atomic update and schema rollback");
  }
  private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
}
