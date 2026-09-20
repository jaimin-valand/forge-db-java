package com.jaimin.db;

import java.nio.file.*;

/** Dependency-free smoke test for ForgeDB's lightweight MVCC/CRUD layer. */
public final class MvccCrudTest {
  public static void main(String[] args) throws Exception {
    Path dir=Files.createTempDirectory("forgedb-mvcc-"); Path dbFile=dir.resolve("data.fdb");
    Database db=new Database(dbFile); SqlEngine sql=new SqlEngine(db);
    sql.execute("CREATE TABLE users (id INT PRIMARY KEY, name TEXT NOT NULL, age INT)");
    sql.execute("INSERT INTO users VALUES (1, 'A', 30)");
    sql.execute("INSERT INTO users VALUES (2, 'B', 40)");

    sql.execute("BEGIN");
    sql.execute("UPDATE users SET age = 31 WHERE id = 1");
    if(!sql.execute("SELECT * FROM users WHERE id = 1").get(0).get(2).equals("31")) throw new AssertionError("own update not visible");
    sql.execute("DELETE FROM users WHERE id = 2");
    if(sql.execute("SELECT * FROM users WHERE id = 2").size()!=0) throw new AssertionError("delete not visible");
    sql.execute("ROLLBACK");
    if(!sql.execute("SELECT * FROM users WHERE id = 1").get(0).get(2).equals("30")) throw new AssertionError("rollback failed");
    if(sql.execute("SELECT * FROM users WHERE id = 2").size()!=1) throw new AssertionError("rollback delete failed");

    sql.execute("BEGIN");
    sql.execute("UPDATE users SET age = 32 WHERE id = 1");
    sql.execute("DELETE FROM users WHERE id = 2");
    sql.execute("COMMIT");

    Database reopened=new Database(dbFile); SqlEngine sql2=new SqlEngine(reopened);
    if(!sql2.execute("SELECT * FROM users WHERE id = 1").get(0).get(2).equals("32")) throw new AssertionError("committed update lost");
    if(sql2.execute("SELECT * FROM users WHERE id = 2").size()!=0) throw new AssertionError("committed delete lost");
    System.out.println("PASS: MVCC-lite CRUD/rollback/commit/reload");
  }
}
