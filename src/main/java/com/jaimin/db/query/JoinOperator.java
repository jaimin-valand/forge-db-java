package com.jaimin.db.query;

import java.util.*;

/** Volcano-style INNER JOIN operator using an equality predicate. */
public final class JoinOperator implements ExecutionOperator {
  private final List<List<String>> output = new ArrayList<>();
  private int cursor;

  public JoinOperator(List<List<String>> leftRows, List<String> leftColumns,
                      List<List<String>> rightRows, List<String> rightColumns,
                      String leftColumn, String rightColumn) {
    int li = resolve(leftColumns, leftColumn);
    int ri = resolve(rightColumns, rightColumn);
    for (List<String> l : leftRows) {
      for (List<String> r : rightRows) {
        if (Objects.equals(l.get(li), r.get(ri))) {
          List<String> row = new ArrayList<>(l.size() + r.size());
          row.addAll(l); row.addAll(r); output.add(row);
        }
      }
    }
  }

  private static int resolve(List<String> columns, String requested) {
    int exact = columns.indexOf(requested);
    if (exact >= 0) return exact;
    String unqualified = requested.contains(".") ? requested.substring(requested.indexOf('.') + 1) : requested;
    int found = -1;
    for (int i = 0; i < columns.size(); i++) {
      String c = columns.get(i);
      String u = c.contains(".") ? c.substring(c.indexOf('.') + 1) : c;
      if (u.equalsIgnoreCase(unqualified)) {
        if (found >= 0) throw new IllegalArgumentException("ambiguous column: " + requested);
        found = i;
      }
    }
    if (found < 0) throw new IllegalArgumentException("unknown column: " + requested);
    return found;
  }

  @Override public List<String> next() { return cursor < output.size() ? output.get(cursor++) : null; }
  @Override public void close() { cursor = output.size(); }
}
