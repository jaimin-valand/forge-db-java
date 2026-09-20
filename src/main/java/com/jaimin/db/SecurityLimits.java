package com.jaimin.db;

/** Central resource limits used by the storage and SQL boundary layers. */
public final class SecurityLimits {
  public static final int MAX_TABLES = 1_024;
  public static final int MAX_COLUMNS_PER_TABLE = 256;
  public static final int MAX_ROWS_PER_TABLE = 1_000_000;
  public static final int MAX_VALUE_LENGTH = 65_536;
  public static final int MAX_MIGRATION_HISTORY = 10_000;

  private SecurityLimits() {}

  public static void value(String value, String field) {
    if (value == null) return;
    if (value.length() > MAX_VALUE_LENGTH) {
      throw new IllegalArgumentException(field + " exceeds maximum length of " + MAX_VALUE_LENGTH);
    }
  }
}
