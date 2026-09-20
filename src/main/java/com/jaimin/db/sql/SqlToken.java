package com.jaimin.db.sql;

public record SqlToken(Type type, String text, int position) {
  public enum Type { KEYWORD, IDENTIFIER, STRING, NUMBER, SYMBOL, EOF }
}
