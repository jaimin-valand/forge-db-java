package com.jaimin.db.reliability;

import com.jaimin.db.Database;
import java.nio.file.Path;
import java.util.List;

/** Process entry point for deterministic crash-injection scenarios. */
public final class ChaosScenario {
  private ChaosScenario() {}

  public static void main(String[] args) throws Exception {
    if (args.length != 3) throw new IllegalArgumentException("usage: ChaosScenario <db> <mode> <id>");
    Path db = Path.of(args[0]);
    String mode = args[1];
    String id = args[2];
    switch (mode) {
      case "committed-crash" -> {
        { Database database = new Database(db);
          database.begin();
          database.createTable("chaos", List.of("id", "message"));
          database.recordCreate("CREATE TABLE chaos (id TEXT, message TEXT)");
          database.insert("chaos", List.of(id, "committed"));
          database.recordInsert("INSERT INTO chaos VALUES (" + id + ", 'committed')");
          database.commit();
        }
      }
      case "uncommitted-crash" -> {
        { Database database = new Database(db);
          database.begin();
          database.createTable("chaos", List.of("id", "message"));
          database.recordCreate("CREATE TABLE chaos (id TEXT, message TEXT)");
          database.insert("chaos", List.of(id, "uncommitted"));
          database.recordInsert("INSERT INTO chaos VALUES (" + id + ", 'uncommitted')");
          // Fault injection terminates the process before COMMIT.
        }
      }
      case "verify-committed" -> {
        { Database database = new Database(db);
          if (!database.tables().contains("chaos")) throw new AssertionError("committed table missing");
          if (database.count("chaos") != 1) throw new AssertionError("committed row missing");
        }
      }
      case "verify-uncommitted" -> {
        { Database database = new Database(db);
          if (database.tables().contains("chaos")) throw new AssertionError("uncommitted table survived recovery");
        }
      }
      default -> throw new IllegalArgumentException("unknown mode: " + mode);
    }
  }
}
