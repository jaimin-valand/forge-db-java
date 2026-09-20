package com.jaimin.db.sql;

import java.util.*;
import static com.jaimin.db.sql.SqlToken.Type.*;

public final class SqlParser {
  private List<SqlToken> tokens; private int p;
  public SqlStatement parse(String sql){
    tokens=new SqlLexer().tokenize(sql); p=0;
    if (peek("EXPLAIN")) { consume(); return parseSelect(true); }
    if (peek("BEGIN")) { consume(); expectEof(); return new SqlStatement.Begin(); }
    if (peek("COMMIT")) { consume(); expectEof(); return new SqlStatement.Commit(); }
    if (peek("ROLLBACK")) { consume(); expectEof(); return new SqlStatement.Rollback(); }
    if (peek("CREATE")) return parseCreate();
    if (peek("ALTER")) return parseAlter();
    if (peek("INSERT")) return parseInsert();
    if (peek("UPDATE")) return parseUpdate();
    if (peek("DELETE")) return parseDelete();
    if (peek("SELECT")) return parseSelect(false);
    throw error("unsupported statement");
  }
  private SqlStatement.CreateTable parseCreate(){
    consume("CREATE");consume("TABLE");String table=identifier();consume("(");List<SqlStatement.ColumnDef> cols=new ArrayList<>();
    do { String name=identifier(); String type=(peek(",")||peek(")")) ? "TEXT" : identifierOrKeyword(); boolean pk=false, uq=false, nn=false;
      while(!peek(",")&&!peek(")")){ if(peek("PRIMARY")){consume();consume("KEY");pk=true;} else if(peek("UNIQUE")){consume();uq=true;} else if(peek("NOT")){consume();consume("NULL");nn=true;} else throw error("unexpected column constraint"); }
      cols.add(new SqlStatement.ColumnDef(name,type,pk,uq,nn));
    } while(accept(",")); consume(")"); expectEof(); return new SqlStatement.CreateTable(table,List.copyOf(cols));
  }
  private SqlStatement.AlterTableAddColumn parseAlter(){
    consume("ALTER"); consume("TABLE"); String table=identifier(); consume("ADD"); consume("COLUMN");
    String name=identifier(); String type=identifierOrKeyword(); boolean pk=false, uq=false, nn=false; String def=null;
    while(!peekEnd()){
      if(peek("PRIMARY")){consume();consume("KEY");pk=true;}
      else if(peek("UNIQUE")){consume();uq=true;}
      else if(peek("NOT")){consume();consume("NULL");nn=true;}
      else if(peek("DEFAULT")){consume();SqlToken t=next();if(t.type()!=STRING&&t.type()!=NUMBER&&t.type()!=IDENTIFIER)throw error("expected default value");def=t.text();}
      else throw error("unexpected column definition token");
    }
    if(def==null) def="";
    return new SqlStatement.AlterTableAddColumn(table,new SqlStatement.ColumnDef(name,type,pk,uq,nn),def);
  }
  private boolean peekEnd(){ return p<tokens.size() && tokens.get(p).type()==EOF; }

  private SqlStatement.Insert parseInsert(){
    consume("INSERT");consume("INTO");String table=identifier();consume("VALUES");consume("(");List<String> vals=new ArrayList<>();
    if(!peek(")")){ do { SqlToken t=next(); if(t.type()!=STRING&&t.type()!=NUMBER&&t.type()!=IDENTIFIER) throw error("expected value"); vals.add(t.text()); } while(accept(",")); }
    consume(")");expectEof();return new SqlStatement.Insert(table,List.copyOf(vals));
  }

  private SqlStatement.Update parseUpdate(){
    consume("UPDATE"); String table=identifier(); consume("SET"); List<SqlStatement.Assignment> assigns=new ArrayList<>();
    do { String col=identifier(); consume("="); SqlToken t=next(); if(t.type()!=STRING&&t.type()!=NUMBER&&t.type()!=IDENTIFIER) throw error("expected assignment value"); assigns.add(new SqlStatement.Assignment(col,t.text())); } while(accept(","));
    List<SqlStatement.Condition> conditions=new ArrayList<>(); if(accept("WHERE")){ conditions.add(parseCondition()); while(accept("AND")) conditions.add(parseCondition()); }
    expectEof(); return new SqlStatement.Update(table,List.copyOf(assigns),List.copyOf(conditions));
  }
  private SqlStatement.Delete parseDelete(){
    consume("DELETE"); consume("FROM"); String table=identifier(); List<SqlStatement.Condition> conditions=new ArrayList<>();
    if(accept("WHERE")){ conditions.add(parseCondition()); while(accept("AND")) conditions.add(parseCondition()); }
    expectEof(); return new SqlStatement.Delete(table,List.copyOf(conditions));
  }
  private SqlStatement.Select parseSelect(boolean explain){
    consume("SELECT"); List<String> cols=new ArrayList<>(); List<SqlStatement.Aggregate> aggregates=new ArrayList<>();
    if(accept("*")) cols.add("*");
    else {
      do {
        String first=identifier();
        if(accept("(")) {
          String fn=first.toUpperCase(Locale.ROOT);
          if(!Set.of("COUNT","SUM","AVG","MIN","MAX").contains(fn)) throw error("unsupported aggregate "+first);
          String arg;
          if(accept("*")) arg="*"; else arg=qualifiedIdentifier();
          consume(")");
          aggregates.add(new SqlStatement.Aggregate(fn,arg));
          cols.add(fn+"("+arg+")");
        } else {
          if(accept(".")) first=first+"."+identifier();
          cols.add(first);
        }
      } while(accept(","));
    }
    consume("FROM");String table=identifier(); if(accept("AS")) identifier(); else if(isIdentifierToken()) identifier();
    SqlStatement.Join join=null; boolean hasJoin=false;
    if(accept("INNER")){ consume("JOIN"); hasJoin=true; } else if(accept("JOIN")){ hasJoin=true; }
    if(hasJoin){ String jt=identifier(); String ja=jt; if(accept("AS")) ja=identifier(); else if(isIdentifierToken()) ja=identifier(); consume("ON"); String left=qualifiedIdentifier(); String op=next().text(); if(!op.equals("=")) throw error("only equality JOIN predicates are supported"); String right=qualifiedIdentifier(); join=new SqlStatement.Join(jt,ja,left,op,right); }
    List<SqlStatement.Condition> conditions=new ArrayList<>();
    if(accept("WHERE")){ conditions.add(parseCondition()); while(accept("AND")) conditions.add(parseCondition()); }
    List<String> groupBy=new ArrayList<>();
    if(accept("GROUP")){ consume("BY"); do { groupBy.add(qualifiedIdentifier()); } while(accept(",")); }
    List<SqlStatement.Condition> having=new ArrayList<>();
    if(accept("HAVING")){ having.add(parseAggregateAwareCondition()); while(accept("AND")) having.add(parseAggregateAwareCondition()); }
    String orderBy=null; boolean ascending=true; int limit=-1; int offset=0;
    if(accept("ORDER")){ consume("BY");
      if(isIdentifierToken()) { orderBy=expressionReference(); }
      else throw error("expected ORDER BY expression");
      if(peek("ASC")||peek("DESC")){ ascending=!peek("DESC"); consume(); }
    }
    boolean pagination=true; while(pagination){
      if(accept("LIMIT")){ limit=positiveOrZeroNumber("LIMIT"); }
      else if(accept("OFFSET")){ offset=positiveOrZeroNumber("OFFSET"); }
      else pagination=false;
    }
    expectEof(); return new SqlStatement.Select(explain,cols,table,conditions,orderBy,ascending,limit,offset,join,aggregates,groupBy,having);
  }
  private int positiveOrZeroNumber(String label){ SqlToken t=next(); if(t.type()!=NUMBER) throw error("expected numeric "+label); try { long v=Long.parseLong(t.text()); if(v<0||v>Integer.MAX_VALUE) throw error("invalid "+label); return (int)v; } catch(NumberFormatException e){ throw error("invalid "+label); } }
  private SqlStatement.Condition parseAggregateAwareCondition(){
    String col=expressionReference(); String op=next().text();
    if(!Set.of("=","!=","<>",">","<",">=","<=").contains(op)) throw error("unsupported comparison operator "+op);
    SqlToken t=next(); if(t.type()!=STRING&&t.type()!=NUMBER&&t.type()!=IDENTIFIER) throw error("expected HAVING value");
    return new SqlStatement.Condition(col,op,t.text());
  }
  private String expressionReference(){
    String first=identifier();
    if(accept("(")){ String fn=first.toUpperCase(Locale.ROOT); if(!Set.of("COUNT","SUM","AVG","MIN","MAX").contains(fn)) throw error("unsupported aggregate "+first); String arg=accept("*")?"*":qualifiedIdentifier(); consume(")"); return fn+"("+arg+")"; }
    if(accept(".")) return first+"."+identifier();
    return first;
  }
  private SqlStatement.Condition parseCondition(){
    String col=qualifiedIdentifier(); String op=next().text();
    if(!Set.of("=","!=","<>",">","<",">=","<=").contains(op)) throw error("unsupported comparison operator "+op);
    SqlToken t=next();if(t.type()!=STRING&&t.type()!=NUMBER&&t.type()!=IDENTIFIER)throw error("expected WHERE value");
    return new SqlStatement.Condition(col,op,t.text());
  }
  private String identifier(){SqlToken t=next();if(t.type()!=IDENTIFIER)throw error("expected identifier");return t.text();}
  private boolean isIdentifierToken(){return p<tokens.size() && tokens.get(p).type()==IDENTIFIER;}
  private String qualifiedIdentifier(){String left=identifier();if(accept(".")){String right=identifier();return left+"."+right;}return left;}
  private String identifierOrKeyword(){SqlToken t=next();if(t.type()!=IDENTIFIER&&t.type()!=KEYWORD)throw error("expected type");return t.text();}
  private boolean peek(String s){return p<tokens.size() && tokens.get(p).text().equalsIgnoreCase(s);}
  private boolean accept(String s){if(peek(s)){p++;return true;}return false;}
  private void consume(){p++;} private void consume(String s){if(!peek(s))throw error("expected "+s);p++;}
  private SqlToken next(){if(p>=tokens.size())throw error("unexpected end of input");return tokens.get(p++);}
  private void expectEof(){if(tokens.get(p).type()!=EOF)throw error("unexpected token "+tokens.get(p).text());}
  private IllegalArgumentException error(String m){return new IllegalArgumentException(m+" at token "+p);}
}
