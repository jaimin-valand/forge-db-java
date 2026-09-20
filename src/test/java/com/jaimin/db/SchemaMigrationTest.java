package com.jaimin.db;

import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class SchemaMigrationTest {
  @Test void addColumnPersistsAndRecordsMigration() throws Exception {
    Path p=Files.createTempFile("forge-schema", ".db"); Files.deleteIfExists(p);
    try {
      SqlEngine e=new SqlEngine(new Database(p));
      e.execute("CREATE TABLE users (id INT PRIMARY KEY, name TEXT NOT NULL)");
      e.execute("INSERT INTO users VALUES (1, 'Jaimin')");
      e.execute("ALTER TABLE users ADD COLUMN country TEXT DEFAULT 'UK'");
      Database reopened=new Database(p);
      assertEquals(2,reopened.schemaVersion());
      assertEquals(List.of(List.of("1","Jaimin","UK")),reopened.rows("users"));
      assertEquals(1,reopened.migrationHistory().size());
    } finally { Files.deleteIfExists(p); Files.deleteIfExists(p.resolveSibling(p.getFileName()+".wal")); }
  }
}
