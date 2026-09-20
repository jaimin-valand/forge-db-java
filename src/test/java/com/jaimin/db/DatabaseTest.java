package com.jaimin.db;
import static org.junit.jupiter.api.Assertions.*;import java.nio.file.*;import java.util.*;import org.junit.jupiter.api.*;
class DatabaseTest { Path p;
 @BeforeEach void setup()throws Exception{p=Files.createTempFile("mini-db","bin");Files.deleteIfExists(p);}
 @Test void persistsAndIndexes()throws Exception{Database d=new Database(p);d.createTable("users",List.of("id","name"));d.insert("users",List.of("1","Jaimin"));d.insert("users",List.of("2","Alex"));assertEquals(2,d.count("users"));assertEquals("Jaimin",d.selectWhere("users","name","Jaimin").getFirst().get(1));Database reopened=new Database(p);assertEquals(2,reopened.count("users"));}
 @Test void sqlWorks()throws Exception{Database d=new Database(p);SqlEngine e=new SqlEngine(d);e.execute("CREATE TABLE users (id,name)");e.execute("INSERT INTO users VALUES ('1','Jaimin')");assertEquals(1,e.execute("SELECT * FROM users WHERE name = 'Jaimin'").size());}
}
