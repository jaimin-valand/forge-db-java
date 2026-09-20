package com.jaimin.db;

import org.junit.jupiter.api.*;
import java.nio.file.*;
import java.util.*;

public class QueryFeaturesTest {
  @Test void orderByLimitOffsetAndProjectionWork() throws Exception {
    Path dir=Files.createTempDirectory("forge-query-");
    Database db=new Database(dir);
    SqlEngine sql=new SqlEngine(db);
    sql.execute("CREATE TABLE users (id INT PRIMARY KEY, name TEXT NOT NULL)");
    sql.execute("INSERT INTO users VALUES (1, 'Zoe')");
    sql.execute("INSERT INTO users VALUES (2, 'Alice')");
    sql.execute("INSERT INTO users VALUES (3, 'Mike')");
    sql.execute("INSERT INTO users VALUES (4, 'Bob')");
    Assertions.assertEquals(List.of(List.of("2","Alice"), List.of("4","Bob")), sql.execute("SELECT id, name FROM users ORDER BY name ASC LIMIT 2"));
    Assertions.assertEquals(List.of(List.of("3","Mike"), List.of("1","Zoe")), sql.execute("SELECT id, name FROM users ORDER BY name DESC OFFSET 1 LIMIT 2"));
  }
}
