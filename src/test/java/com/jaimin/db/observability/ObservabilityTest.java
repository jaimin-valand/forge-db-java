package com.jaimin.db.observability;

import com.jaimin.db.*;
import java.nio.file.*;

public final class ObservabilityTest {
  public static void main(String[] args) throws Exception {
    Path dir=Files.createTempDirectory("forge-observe-"); Database db=new Database(dir.resolve("db"));
    long start=System.nanoTime(); db.observability().recordQuery("SELECT",start,null);
    for(int i=0;i<100;i++) db.observability().queryLatency().recordMicros(i*10);
    if(db.observability().queryLatency().count()!=101) throw new AssertionError("histogram count");
    String json=db.observability().snapshotJson(db);
    if(!json.contains("health")||!json.contains("query_latency")||!json.contains("p95_us")) throw new AssertionError("snapshot fields");
    if(!db.observability().health(db).status().equals("UP")) throw new AssertionError("health");
    System.out.println("PASS: observability metrics, tracing, histogram and health");
  }
}
