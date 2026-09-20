package com.jaimin.db.observability;

import java.util.*;

public record HealthReport(String status, List<String> checks) {
  public static HealthReport healthy(List<String> checks){return new HealthReport("UP",List.copyOf(checks));}
  public static HealthReport degraded(List<String> checks){return new HealthReport("DEGRADED",List.copyOf(checks));}
  public Map<String,Object> asMap(){Map<String,Object> m=new LinkedHashMap<>();m.put("status",status);m.put("checks",checks);return m;}
}
