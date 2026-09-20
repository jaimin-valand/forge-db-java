package com.jaimin.db.observability;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public record QueryTrace(long id, long startedNanos, long elapsedMicros, String operation, boolean success, String error) {
  private static final AtomicLong IDS = new AtomicLong();
  public static QueryTrace success(String operation,long started){return new QueryTrace(IDS.incrementAndGet(),started,(System.nanoTime()-started)/1_000,operation,true,null);}
  public static QueryTrace failure(String operation,long started,Throwable t){return new QueryTrace(IDS.incrementAndGet(),started,(System.nanoTime()-started)/1_000,operation,false,t.getClass().getSimpleName()+": "+String.valueOf(t.getMessage()));}
  public Map<String,Object> fields(){Map<String,Object> m=new LinkedHashMap<>();m.put("trace_id",id);m.put("operation",operation);m.put("elapsed_us",elapsedMicros);m.put("success",success);if(error!=null)m.put("error",error);return m;}
}
