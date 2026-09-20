package com.jaimin.db;

import org.junit.jupiter.api.*;
import java.nio.file.*;
import java.util.*;

public class JoinQueryTest {
  @Test void innerJoinProjectionAndFilterWork() throws Exception {
    Path dir=Files.createTempDirectory("forge-join-");
    Database db=new Database(dir); SqlEngine sql=new SqlEngine(db);
    sql.execute("CREATE TABLE users (id INT PRIMARY KEY, name TEXT NOT NULL)");
    sql.execute("CREATE TABLE orders (id INT PRIMARY KEY, user_id INT NOT NULL, amount INT)");
    sql.execute("INSERT INTO users VALUES (1, 'Jaimin')");
    sql.execute("INSERT INTO users VALUES (2, 'Alice')");
    sql.execute("INSERT INTO orders VALUES (10, 1, 50)");
    sql.execute("INSERT INTO orders VALUES (11, 1, 75)");
    sql.execute("INSERT INTO orders VALUES (12, 2, 20)");
    Assertions.assertEquals(List.of(List.of("Jaimin","50"), List.of("Jaimin","75")),
      sql.execute("SELECT users.name, orders.amount FROM users JOIN orders ON users.id = orders.user_id WHERE orders.amount >= 50 ORDER BY orders.amount ASC"));
    Assertions.assertEquals(3, sql.execute("SELECT * FROM users JOIN orders ON users.id = orders.user_id").size());
  }

  @Test void joinExplainIsExplicit() throws Exception {
    Path dir=Files.createTempDirectory("forge-join-explain-"); Database db=new Database(dir); SqlEngine sql=new SqlEngine(db);
    sql.execute("CREATE TABLE a (id INT PRIMARY KEY)"); sql.execute("CREATE TABLE b (id INT PRIMARY KEY, a_id INT)");
    Assertions.assertTrue(sql.execute("EXPLAIN SELECT * FROM a JOIN b ON a.id = b.a_id").get(0).get(0).contains("NESTED_LOOP_JOIN"));
  }
}
