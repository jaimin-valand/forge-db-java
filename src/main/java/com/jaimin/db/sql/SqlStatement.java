package com.jaimin.db.sql;

import java.util.*;

public sealed interface SqlStatement permits SqlStatement.Begin, SqlStatement.Commit, SqlStatement.Rollback, SqlStatement.CreateTable, SqlStatement.AlterTableAddColumn, SqlStatement.Insert, SqlStatement.Update, SqlStatement.Delete, SqlStatement.Select {
  record Begin() implements SqlStatement {}
  record Commit() implements SqlStatement {}
  record Rollback() implements SqlStatement {}
  record ColumnDef(String name, String type, boolean primaryKey, boolean unique, boolean notNull) {}
  record CreateTable(String name, List<ColumnDef> columns) implements SqlStatement {}
  record AlterTableAddColumn(String table, ColumnDef column, String defaultValue) implements SqlStatement {}
  record Insert(String table, List<String> values) implements SqlStatement {}
  record Condition(String column, String operator, String value) {}
  record Assignment(String column, String value) {}
  record Update(String table, List<Assignment> assignments, List<Condition> conditions) implements SqlStatement {}
  record Delete(String table, List<Condition> conditions) implements SqlStatement {}
  record Join(String table, String alias, String leftColumn, String operator, String rightColumn) {}
  record Aggregate(String function, String argument) {
    public Aggregate { function = function.toUpperCase(Locale.ROOT); }
  }
  record Select(boolean explain, List<String> columns, String table, List<Condition> conditions, String orderBy, boolean ascending,
                int limit, int offset, Join join, List<Aggregate> aggregates, List<String> groupBy, List<Condition> having) implements SqlStatement {
    public Select {
      columns=List.copyOf(columns); conditions=List.copyOf(conditions); aggregates=List.copyOf(aggregates); groupBy=List.copyOf(groupBy); having=List.copyOf(having);
      if(limit < -1 || offset < 0) throw new IllegalArgumentException("invalid pagination");
    }
    public Select(boolean explain, List<String> columns, String table, List<Condition> conditions, String orderBy, boolean ascending, int limit, int offset, Join join) {
      this(explain, columns, table, conditions, orderBy, ascending, limit, offset, join, List.of(), List.of(), List.of());
    }
    public Select(boolean explain, List<String> columns, String table, List<Condition> conditions) { this(explain, columns, table, conditions, null, true, -1, 0, null); }
    public Select(boolean explain, String table, String whereColumn, String whereValue) {
      this(explain, List.of("*"), table, whereColumn==null?List.of():List.of(new Condition(whereColumn,"=",whereValue)), null, true, -1, 0, null);
    }
    public String whereColumn(){ return conditions.isEmpty()?null:conditions.get(0).column(); }
    public String whereValue(){ return conditions.isEmpty()?null:conditions.get(0).value(); }
    public boolean isAggregateQuery(){ return !aggregates.isEmpty() || !groupBy.isEmpty() || !having.isEmpty(); }
  }
}
