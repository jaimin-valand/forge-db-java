package com.jaimin.db.query;

import java.util.*;

public final class ProjectOperator implements ExecutionOperator {
  private final ExecutionOperator child; private final List<Integer> positions;
  public ProjectOperator(ExecutionOperator child,List<String> sourceColumns,List<String> requested){
    this.child=child; this.positions=new ArrayList<>(); for(String c:requested){int i=sourceColumns.indexOf(c);if(i<0)throw new IllegalArgumentException("unknown column: "+c);positions.add(i);}
  }
  public List<String> next(){List<String> row=child.next();if(row==null)return null;List<String> out=new ArrayList<>();for(int i:positions)out.add(row.get(i));return out;}
}
