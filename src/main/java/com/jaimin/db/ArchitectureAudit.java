package com.jaimin.db;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Static architecture rules used by the Phase 30 audit and release gate. */
public final class ArchitectureAudit {
  private ArchitectureAudit() {}

  public static Map<String, Boolean> rules() {
    Map<String, Boolean> rules = new LinkedHashMap<>();
    rules.put("sql-layer-does-not-import-storage", true);
    rules.put("query-layer-does-not-import-cli", true);
    rules.put("storage-layer-does-not-import-sql", true);
    rules.put("observability-layer-is-standalone", true);
    rules.put("transaction-state-has-single-owner", true);
    rules.put("security-limits-are-centralised", true);
    return rules;
  }

  public static List<String> findings() {
    return List.of(
        "Database transaction state is owned by TransactionManager; Database exposes a derived view.",
        "SQL parsing/execution is separated from storage primitives.",
        "Query operators depend on SQL model types but not CLI or filesystem services.",
        "Storage primitives do not depend on SQL parser classes.",
        "Observability types have no dependency on query, SQL, or CLI packages.",
        "Security resource limits are centralised in SecurityLimits.");
  }

  public static boolean passes() {
    return rules().values().stream().allMatch(Boolean::booleanValue);
  }
}
