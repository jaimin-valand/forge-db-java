package com.jaimin.db.query;

import java.util.*;
import com.jaimin.db.sql.SqlStatement;

/** Builds a Volcano-style operator pipeline and applies relational post-processing. */
public final class Executor {
  public List<List<String>> execute(List<List<String>> rows,List<String> columns,SqlStatement.Select select){
    ExecutionOperator op=new TableScanOperator(rows);
    if(!select.conditions().isEmpty()){
      List<FilterOperator.Condition> cs=select.conditions().stream().map(c->new FilterOperator.Condition(c.column(),c.operator(),c.value())).toList();
      op=new FilterOperator(op,columns,cs);
    }
    List<List<String>> source=new ArrayList<>(); List<String> row; while((row=op.next())!=null) source.add(row); op.close();
    if(select.isAggregateQuery()) return finishAggregate(source, columns, select);
    if(select.orderBy()!=null) sort(source, columns, select.orderBy(), select.ascending());
    int from=Math.min(select.offset(), source.size());
    int to=select.limit()<0 ? source.size() : Math.min(from + select.limit(), source.size());
    List<List<String>> paged=source.subList(from,to);
    if(!(select.columns().size()==1 && select.columns().get(0).equals("*"))) {
      ExecutionOperator project=new ProjectOperator(new TableScanOperator(paged),columns,select.columns());
      List<List<String>> out=new ArrayList<>(); while((row=project.next())!=null) out.add(row); project.close(); return out;
    }
    return new ArrayList<>(paged);
  }

  private List<List<String>> finishAggregate(List<List<String>> source, List<String> sourceColumns, SqlStatement.Select select) {
    List<String> groupColumns = select.groupBy().stream().map(c -> resolveReference(c, sourceColumns)).toList();
    Map<String,List<List<String>>> groups = new LinkedHashMap<>();
    if (groupColumns.isEmpty()) groups.put("__all__", new ArrayList<>(source));
    else for (List<String> row : source) {
      String key = groupColumns.stream().map(c -> String.valueOf(row.get(sourceColumns.indexOf(c)))).reduce((a,b)->a+"\u001f"+b).orElse("");
      groups.computeIfAbsent(key, k -> new ArrayList<>()).add(row);
    }
    List<String> resultColumns = new ArrayList<>(groupColumns);
    for (SqlStatement.Aggregate a : select.aggregates()) resultColumns.add(aggregateName(a));
    List<List<String>> result = new ArrayList<>();
    for (List<List<String>> group : groups.values()) {
      List<String> out = new ArrayList<>();
      if (!group.isEmpty()) for (String c : groupColumns) out.add(group.get(0).get(sourceColumns.indexOf(c)));
      else for (int i=0;i<groupColumns.size();i++) out.add("");
      for (SqlStatement.Aggregate a : select.aggregates()) out.add(computeAggregate(a, group, sourceColumns));
      result.add(out);
    }
    if (!select.having().isEmpty()) {
      List<FilterOperator.Condition> hc = select.having().stream().map(c -> new FilterOperator.Condition(resolveAggregateColumn(c.column(), resultColumns), c.operator(), c.value())).toList();
      ExecutionOperator filter = new FilterOperator(new TableScanOperator(result), resultColumns, hc);
      List<List<String>> filtered = new ArrayList<>(); List<String> row; while((row=filter.next())!=null) filtered.add(row); filter.close(); result=filtered;
    }
    if (select.orderBy()!=null) sort(result, resultColumns, resolveAggregateColumn(select.orderBy(), resultColumns), select.ascending());
    int from=Math.min(select.offset(), result.size()); int to=select.limit()<0 ? result.size() : Math.min(from+select.limit(), result.size());
    List<List<String>> paged=result.subList(from,to);
    if (!(select.columns().size()==1 && select.columns().get(0).equals("*"))) {
      List<String> projected=select.columns().stream().map(c -> resolveAggregateColumn(c,resultColumns)).toList();
      ExecutionOperator project=new ProjectOperator(new TableScanOperator(paged), resultColumns, projected);
      List<List<String>> out=new ArrayList<>(); List<String> row; while((row=project.next())!=null) out.add(row); project.close(); return out;
    }
    return new ArrayList<>(paged);
  }

  private String aggregateName(SqlStatement.Aggregate a) { return a.function()+"("+a.argument()+")"; }
  private String computeAggregate(SqlStatement.Aggregate a, List<List<String>> rows, List<String> columns) {
    if (a.function().equals("COUNT")) return Integer.toString(a.argument().equals("*") ? rows.size() : (int)rows.stream().filter(r -> r.get(columns.indexOf(resolveReference(a.argument(),columns))) != null).count());
    int idx=columns.indexOf(resolveReference(a.argument(),columns)); if(idx<0) throw new IllegalArgumentException("unknown aggregate column: "+a.argument());
    if(rows.isEmpty()) return "0";
    List<String> vals=rows.stream().map(r->r.get(idx)).toList();
    return switch(a.function()) {
      case "SUM" -> Long.toString(vals.stream().mapToLong(this::parseLong).sum());
      case "AVG" -> { double avg=vals.stream().mapToLong(this::parseLong).average().orElse(0); yield formatNumber(avg); }
      case "MIN" -> vals.stream().min(this::compareValues).orElse("");
      case "MAX" -> vals.stream().max(this::compareValues).orElse("");
      default -> throw new IllegalArgumentException("unsupported aggregate: "+a.function());
    };
  }
  private long parseLong(String v){ try{return Long.parseLong(v);}catch(NumberFormatException e){throw new IllegalArgumentException("aggregate requires numeric values: "+v);}}
  private String formatNumber(double v){ return v == Math.rint(v) ? Long.toString((long)v) : Double.toString(v); }
  private String resolveAggregateColumn(String requested,List<String> columns){ if(columns.contains(requested)) return requested; String u=requested.contains(".")?requested.substring(requested.indexOf('.')+1):requested; String found=null; for(String c:columns){ if(c.equalsIgnoreCase(requested)||c.equalsIgnoreCase(u)){ if(found!=null) throw new IllegalArgumentException("ambiguous aggregate expression: "+requested); found=c; } } if(found==null) throw new IllegalArgumentException("unknown aggregate/group column: "+requested); return found; }

  public List<List<String>> executeJoin(List<List<String>> leftRows, List<String> leftColumns,
                                         List<List<String>> rightRows, List<String> rightColumns,
                                         SqlStatement.Select select) {
    SqlStatement.Join join = select.join();
    if (join == null) throw new IllegalArgumentException("join definition is required");
    List<String> qualifiedLeft = qualify(leftColumns, select.table());
    List<String> qualifiedRight = qualify(rightColumns, join.alias());
    JoinOperator joined = new JoinOperator(leftRows, qualifiedLeft, rightRows, qualifiedRight,
            qualifyReference(join.leftColumn(), select.table(), join.alias()),
            qualifyReference(join.rightColumn(), select.table(), join.alias()));
    List<List<String>> source = new ArrayList<>(); List<String> row;
    while ((row = joined.next()) != null) source.add(row); joined.close();
    List<String> combined = new ArrayList<>(qualifiedLeft); combined.addAll(qualifiedRight);
    if (!select.conditions().isEmpty()) {
      List<FilterOperator.Condition> cs = select.conditions().stream()
          .map(c -> new FilterOperator.Condition(resolveReference(c.column(), combined), c.operator(), c.value())).toList();
      ExecutionOperator filter = new FilterOperator(new TableScanOperator(source), combined, cs);
      source = new ArrayList<>(); while ((row = filter.next()) != null) source.add(row); filter.close();
    }
    if (select.orderBy() != null) sort(source, combined, resolveReference(select.orderBy(), combined), select.ascending());
    int from=Math.min(select.offset(), source.size());
    int to=select.limit()<0 ? source.size() : Math.min(from + select.limit(), source.size());
    List<List<String>> paged=source.subList(from,to);
    if (!(select.columns().size()==1 && select.columns().get(0).equals("*"))) {
      List<String> projectedColumns = select.columns().stream().map(c -> resolveReference(c, combined)).toList();
      ExecutionOperator project=new ProjectOperator(new TableScanOperator(paged),combined,projectedColumns);
      List<List<String>> out=new ArrayList<>(); while((row=project.next())!=null) out.add(row); project.close(); return out;
    }
    return new ArrayList<>(paged);
  }

  private static List<String> qualify(List<String> columns, String table) {
    return columns.stream().map(c -> table + "." + c).toList();
  }
  private static String qualifyReference(String ref, String leftTable, String rightTable) {
    if (ref.contains(".")) return ref;
    if (leftTable.equals(ref) || rightTable.equals(ref)) return ref;
    throw new IllegalArgumentException("JOIN column must be qualified: " + ref);
  }
  private static String resolveReference(String requested, List<String> columns) {
    if (columns.contains(requested)) return requested;
    String unqualified=requested.contains(".")?requested.substring(requested.indexOf('.')+1):requested;
    String found=null;
    for(String c:columns){String u=c.substring(c.indexOf('.')+1); if(u.equalsIgnoreCase(unqualified)){if(found!=null)throw new IllegalArgumentException("ambiguous column: "+requested);found=c;}}
    if(found==null)throw new IllegalArgumentException("unknown column: "+requested);
    return found;
  }

  private void sort(List<List<String>> rows,List<String> columns,String column,boolean ascending){
    int idx=columns.indexOf(column); if(idx<0) throw new IllegalArgumentException("unknown ORDER BY column: "+column);
    Comparator<List<String>> cmp=(a,b)->compareValues(a.get(idx),b.get(idx));
    if(!ascending) cmp=cmp.reversed(); rows.sort(cmp);
  }
  private int compareValues(String a,String b){
    try { return Long.compare(Long.parseLong(a),Long.parseLong(b)); } catch(NumberFormatException ignored) {}
    return a.compareTo(b);
  }
}
