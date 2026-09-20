package com.jaimin.db;

import com.jaimin.db.observability.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/** Runtime observability facade: metrics, traces, health and machine-readable diagnostics. */
public final class Observability {
  private final Metrics metrics;
  private final StructuredLogger logger;
  private final LatencyHistogram queryLatency = new LatencyHistogram();
  private final AtomicLong successfulQueries = new AtomicLong();
  private final AtomicLong failedQueries = new AtomicLong();
  public Observability(Metrics metrics){this.metrics=metrics;this.logger=new StructuredLogger("forgedb");}
  public StructuredLogger logger(){return logger;}
  public void recordQuery(String operation,long started,Throwable error){
    QueryTrace t = error==null ? QueryTrace.success(operation,started) : QueryTrace.failure(operation,started,error);
    queryLatency.recordMicros(t.elapsedMicros()); if(t.success())successfulQueries.incrementAndGet();else failedQueries.incrementAndGet();
    if(t.success()) logger.debug("query.completed",t.fields()); else logger.warn("query.failed",t.fields());
  }
  public LatencyHistogram queryLatency(){return queryLatency;}
  public long successfulQueries(){return successfulQueries.get();} public long failedQueries(){return failedQueries.get();}
  public HealthReport health(Database db){
    List<String> c=new ArrayList<>(); String status="UP";
    try { if(db.tables()!=null){c.add("catalog=ok");c.add("tables="+db.tables().size());} else {c.add("catalog=missing");status="DEGRADED";} }
    catch(Exception e){c.add("catalog=error");status="DEGRADED";}
    c.add("transaction_state="+db.transactionState()); c.add("metrics=ok");
    return "UP".equals(status)?HealthReport.healthy(c):HealthReport.degraded(c);
  }
  public Map<String,Object> snapshot(Database db){
    Map<String,Object> root=new LinkedHashMap<>(); root.put("version","0.28.0"); root.put("health",health(db).asMap());
    root.put("metrics",metrics.snapshot()); root.put("query_latency",Map.of("count",queryLatency.count(),"total_us",queryLatency.totalMicros(),"p50_us",queryLatency.percentile(.5),"p95_us",queryLatency.percentile(.95),"p99_us",queryLatency.percentile(.99)));
    root.put("queries",Map.of("successful",successfulQueries(),"failed",failedQueries())); return root;
  }
  public String snapshotJson(Database db){return Json.encode(snapshot(db));}
}
