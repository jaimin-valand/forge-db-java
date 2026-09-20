package com.jaimin.db.observability;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/** Dependency-free structured logger suitable for CLI/server embedding. */
public final class StructuredLogger {
  private final String component;
  private volatile LogLevel minimum = LogLevel.INFO;
  public StructuredLogger(String component) { this.component = component; }
  public void minimum(LogLevel level) { minimum = level == null ? LogLevel.INFO : level; }
  public LogLevel minimum() { return minimum; }
  public void trace(String message, Map<String, ?> fields) { emit(LogLevel.TRACE, message, fields); }
  public void debug(String message, Map<String, ?> fields) { emit(LogLevel.DEBUG, message, fields); }
  public void info(String message, Map<String, ?> fields) { emit(LogLevel.INFO, message, fields); }
  public void warn(String message, Map<String, ?> fields) { emit(LogLevel.WARN, message, fields); }
  public void error(String message, Map<String, ?> fields) { emit(LogLevel.ERROR, message, fields); }
  private void emit(LogLevel level, String message, Map<String, ?> fields) {
    if (!minimum.allows(level)) return;
    Map<String,Object> event = new LinkedHashMap<>();
    event.put("ts", Instant.now().toString()); event.put("level", level.name());
    event.put("component", component); event.put("message", message);
    if (fields != null) event.putAll(fields);
    System.err.println(Json.encode(event));
  }
}
