package com.jaimin.db.observability;

import java.util.*;

/** Tiny JSON encoder; intentionally dependency-free for the embedded engine. */
public final class Json {
  private Json() {}
  public static String encode(Object value) {
    if (value == null) return "null";
    if (value instanceof Number || value instanceof Boolean) return String.valueOf(value);
    if (value instanceof Map<?,?> m) {
      StringJoiner j=new StringJoiner(",","{","}");
      for (var e:m.entrySet()) j.add(quote(String.valueOf(e.getKey()))+":"+encode(e.getValue())); return j.toString();
    }
    if (value instanceof Iterable<?> it) { StringJoiner j=new StringJoiner(",","[","]"); for(var x:it)j.add(encode(x)); return j.toString(); }
    return quote(String.valueOf(value));
  }
  public static String quote(String s) { return "\""+s.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n").replace("\r","\\r")+"\""; }
}
