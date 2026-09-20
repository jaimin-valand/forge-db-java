package com.jaimin.db;

import java.util.*;
import com.jaimin.db.sql.*;
import com.jaimin.db.query.Executor;

/** SQL facade: lexer -> parser -> planner/executor -> MVCC-aware storage. */
public final class SqlEngine {
  private final Database db; private final SqlParser parser=new SqlParser(); private final Executor executor=new Executor();
  public SqlEngine(Database db){this.db=db;}
  public List<List<String>> execute(String sql)throws Exception{
    long started = PerformanceTimer.now();
    try {
      SqlStatement st=parser.parse(sql);
    if(st instanceof SqlStatement.Begin){db.begin();return List.of();}
    if(st instanceof SqlStatement.Commit){db.commit();return List.of();}
    if(st instanceof SqlStatement.Rollback){db.rollback();return List.of();}
    if(st instanceof SqlStatement.CreateTable c){db.createTableWithDefinitions(c.name(),c.columns());if(db.inTransaction())db.recordCreate(sql);return List.of();}
    if(st instanceof SqlStatement.AlterTableAddColumn a){db.addColumn(a.table(),a.column(),a.defaultValue(),sql);if(db.inTransaction())db.recordCreate(sql);return List.of(List.of("SCHEMA_VERSION",Long.toString(db.schemaVersion())));}
    if(st instanceof SqlStatement.Insert i){db.insert(i.table(),i.values());if(db.inTransaction())db.recordInsert(sql);return List.of();}
    if(st instanceof SqlStatement.Update u){int n=db.update(u.table(),u.assignments(),u.conditions());if(db.inTransaction())db.recordUpdate(sql);return List.of(List.of("UPDATED",Integer.toString(n)));}
    if(st instanceof SqlStatement.Delete d){int n=db.delete(d.table(),d.conditions());if(db.inTransaction())db.recordDelete(sql);return List.of(List.of("DELETED",Integer.toString(n)));}
    if(st instanceof SqlStatement.Select s){
      db.metrics().select();
      if(s.explain()){ 
        if (s.join() != null) return List.of(List.of("Plan{NESTED_LOOP_JOIN, left="+s.table()+", right="+s.join().table()+", predicate="+s.join().leftColumn()+"="+s.join().rightColumn()+"}"));
        return List.of(List.of(db.explain(s.table(),s.conditions()).toString())); 
      }
      if (s.join() != null) return executor.executeJoin(db.rows(s.table()), db.columns(s.table()), db.rows(s.join().table()), db.columns(s.join().table()), s);
      return executor.execute(db.rows(s.table()),db.columns(s.table()),s);
    }
      throw new IllegalStateException("unknown statement");
    } finally {
      db.metrics().queryNanos(PerformanceTimer.elapsedNanos(started));
    }
  }
}
