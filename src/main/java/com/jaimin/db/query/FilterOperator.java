package com.jaimin.db.query;

import java.util.*;

public final class FilterOperator implements ExecutionOperator {
  private final ExecutionOperator child; private final List<String> columns; private final List<Condition> conditions;
  public record Condition(String column,String operator,String value) {}
  public FilterOperator(ExecutionOperator child,List<String> columns,List<Condition> conditions){this.child=child;this.columns=columns;this.conditions=conditions;}
  public List<String> next(){
    List<String> row;
    while((row=child.next())!=null){ boolean ok=true; for(Condition c:conditions){int i=columns.indexOf(c.column()); if(i<0)throw new IllegalArgumentException("unknown column: "+c.column()); if(!compare(row.get(i),c.operator(),c.value())){ok=false;break;}} if(ok)return row; }
    return null;
  }
  private boolean compare(String actual,String op,String expected){
    int cmp;
    try { cmp=Integer.compare(Integer.parseInt(actual),Integer.parseInt(expected)); }
    catch(Exception e){ cmp=actual.compareTo(expected); }
    return switch(op){case "=" -> actual.equals(expected);case "!=","<>" -> !actual.equals(expected);case ">" -> cmp>0;case "<" -> cmp<0;case ">=" -> cmp>=0;case "<=" -> cmp<=0;default -> throw new IllegalArgumentException("unsupported operator: "+op);};
  }
}
