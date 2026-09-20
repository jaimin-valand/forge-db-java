package com.jaimin.db.sql;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
public class SqlParserTest {
 @Test void parsesSchemaConstraints(){var s=(SqlStatement.CreateTable)new SqlParser().parse("CREATE TABLE users (id INT PRIMARY KEY, email TEXT UNIQUE, name TEXT NOT NULL)");assertEquals(3,s.columns().size());assertTrue(s.columns().get(0).primaryKey());assertTrue(s.columns().get(1).unique());assertTrue(s.columns().get(2).notNull());}
 @Test void parsesInsertAndSelect(){var p=new SqlParser();assertEquals(3,((SqlStatement.Insert)p.parse("INSERT INTO users VALUES (1, 'a@b.com', 'Jaimin')")).values().size());var q=(SqlStatement.Select)p.parse("EXPLAIN SELECT * FROM users WHERE id = 1");assertTrue(q.explain());assertEquals("id",q.whereColumn());}
 @Test void rejectsUnknownSyntax(){assertThrows(IllegalArgumentException.class,()->new SqlParser().parse("DROP TABLE users"));}
}
