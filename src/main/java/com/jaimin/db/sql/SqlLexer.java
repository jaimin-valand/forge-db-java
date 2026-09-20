package com.jaimin.db.sql;

import java.util.*;

/** Small deterministic lexer used by ForgeDB instead of regex-based SQL dispatch. */
public final class SqlLexer {
  private static final int MAX_SQL_LENGTH = 1_000_000;
  private static final int MAX_TOKENS = 20_000;
  private static final int MAX_IDENTIFIER_LENGTH = 128;
  private static final int MAX_STRING_LENGTH = 65_536;
  private static final int MAX_NUMBER_LENGTH = 19;

  private static final Set<String> KEYWORDS = Set.of(
      "CREATE","TABLE","INSERT","INTO","VALUES","SELECT","FROM","WHERE","EXPLAIN",
      "PRIMARY","KEY","UNIQUE","NOT","NULL","INT","INTEGER","TEXT","AND","BEGIN","COMMIT","ROLLBACK",
      "ALTER","ADD","COLUMN","DEFAULT","UPDATE","SET","DELETE","ORDER","BY","ASC","DESC","LIMIT","OFFSET","JOIN","INNER","ON","AS","GROUP","HAVING");

  public List<SqlToken> tokenize(String sql) {
    if (sql == null) throw new IllegalArgumentException("SQL must not be null");
    if (sql.length() > MAX_SQL_LENGTH) throw new IllegalArgumentException("SQL exceeds maximum length of " + MAX_SQL_LENGTH);
    List<SqlToken> out = new ArrayList<>();
    int i = 0;
    while (i < sql.length()) {
      char ch = sql.charAt(i);
      if (Character.isWhitespace(ch) || ch == ';') { i++; continue; }
      if (ch == '\'' ) {
        int start = i++;
        StringBuilder b = new StringBuilder();
        while (i < sql.length()) {
          if (sql.charAt(i) == '\'' && i + 1 < sql.length() && sql.charAt(i + 1) == '\'') { b.append('\''); i += 2; continue; }
          if (sql.charAt(i) == '\'') { i++; break; }
          b.append(sql.charAt(i++));
        }
        if (i == sql.length() && (sql.length() == 0 || sql.charAt(sql.length() - 1) != '\'')) throw error("unterminated string", start);
        if (b.length() > MAX_STRING_LENGTH) throw error("string literal exceeds maximum length", start);
        out.add(new SqlToken(SqlToken.Type.STRING, b.toString(), start));
        checkTokenLimit(out, start); continue;
      }
      if (Character.isDigit(ch) || (ch == '-' && i + 1 < sql.length() && Character.isDigit(sql.charAt(i+1)))) {
        int start=i++; while(i<sql.length() && Character.isDigit(sql.charAt(i))) i++;
        if (i - start > MAX_NUMBER_LENGTH) throw error("numeric literal exceeds maximum length", start);
        out.add(new SqlToken(SqlToken.Type.NUMBER, sql.substring(start,i),start));
        checkTokenLimit(out, start); continue;
      }
      if (Character.isLetter(ch) || ch == '_') {
        int start=i++; while(i<sql.length() && (Character.isLetterOrDigit(sql.charAt(i)) || sql.charAt(i)=='_')) i++;
        String word=sql.substring(start,i); 
        if (word.length() > MAX_IDENTIFIER_LENGTH) throw error("identifier exceeds maximum length", start);
        String upper=word.toUpperCase(Locale.ROOT);
        out.add(new SqlToken(KEYWORDS.contains(upper)?SqlToken.Type.KEYWORD:SqlToken.Type.IDENTIFIER, KEYWORDS.contains(upper)?upper:word,start));
        checkTokenLimit(out, start); continue;
      }
      if (",()=*<>!.".indexOf(ch) >= 0) {
        int start=i++;
        if (i<sql.length() && (ch=='!' || ch=='<' || ch=='>') && sql.charAt(i)=='=') i++;
        out.add(new SqlToken(SqlToken.Type.SYMBOL,sql.substring(start,i),start));
        checkTokenLimit(out, start); continue;
      }
      throw error("unexpected character '"+ch+"'", i);
    }
    out.add(new SqlToken(SqlToken.Type.EOF,"",sql.length())); return out;
  }
  private static void checkTokenLimit(List<SqlToken> tokens, int pos) {
    if (tokens.size() > MAX_TOKENS) throw new IllegalArgumentException("SQL exceeds maximum token count of " + MAX_TOKENS + " at position " + pos);
  }
  private IllegalArgumentException error(String msg,int pos){return new IllegalArgumentException(msg+" at position "+pos);}
}
