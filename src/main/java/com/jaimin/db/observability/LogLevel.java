package com.jaimin.db.observability;

public enum LogLevel {
  TRACE, DEBUG, INFO, WARN, ERROR;
  public boolean allows(LogLevel event) { return event.ordinal() >= ordinal(); }
}
