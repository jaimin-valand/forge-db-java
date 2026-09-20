package com.jaimin.db;

import java.nio.file.*;
import java.util.*;

public final class AggregationQueryTest {
  public static void main(String[] args) throws Exception {
    Path dir=Files.createTempDirectory("forge-agg-"); Database db=new Database(dir.resolve("db")); SqlEngine e=new SqlEngine(db);
    e.execute("CREATE TABLE orders (id INT PRIMARY KEY, user_id INT, amount INT);");
    e.execute("INSERT INTO orders VALUES (1, 10, 50);"); e.execute("INSERT INTO orders VALUES (2, 10, 75);"); e.execute("INSERT INTO orders VALUES (3, 20, 200);");
    check(e.execute("SELECT COUNT(*) FROM orders;").equals(List.of(List.of("3"))), "count");
    check(e.execute("SELECT user_id, COUNT(*), SUM(amount) FROM orders GROUP BY user_id ORDER BY user_id ASC;").equals(List.of(List.of("10","2","125"),List.of("20","1","200"))), "group aggregate");
    check(e.execute("SELECT user_id, COUNT(*), SUM(amount) FROM orders GROUP BY user_id HAVING SUM(amount) >= 150 ORDER BY SUM(amount) DESC;").equals(List.of(List.of("20","1","200"))), "having");
    System.out.println("PASS: aggregation query tests");
  }
  private static void check(boolean ok,String name){if(!ok)throw new AssertionError(name);}
}
