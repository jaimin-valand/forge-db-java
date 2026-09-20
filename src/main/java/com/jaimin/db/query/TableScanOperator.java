package com.jaimin.db.query;

import java.util.*;

public final class TableScanOperator implements ExecutionOperator {
  private final List<List<String>> rows; private int cursor;
  public TableScanOperator(List<List<String>> rows){this.rows=rows;}
  public List<String> next(){ if(cursor>=rows.size()) return null; return new ArrayList<>(rows.get(cursor++)); }
}
