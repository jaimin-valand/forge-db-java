package com.jaimin.db.observability;

import java.util.concurrent.atomic.AtomicLongArray;

/** Fixed logarithmic latency histogram. Values are microseconds. */
public final class LatencyHistogram {
  private static final long[] UPPER = {1,2,5,10,25,50,100,250,500,1_000,2_500,5_000,10_000,25_000,50_000,100_000,250_000,500_000,1_000_000,Long.MAX_VALUE};
  private final AtomicLongArray buckets = new AtomicLongArray(UPPER.length);
  private final java.util.concurrent.atomic.AtomicLong total = new java.util.concurrent.atomic.AtomicLong();
  private final java.util.concurrent.atomic.AtomicLong count = new java.util.concurrent.atomic.AtomicLong();
  public void recordMicros(long micros) { long v=Math.max(0,micros); total.addAndGet(v); count.incrementAndGet(); for(int i=0;i<UPPER.length;i++) if(v<=UPPER[i]){buckets.incrementAndGet(i);break;} }
  public long count(){return count.get();} public long totalMicros(){return total.get();}
  public long percentile(double p){ long n=count.get(); if(n==0)return 0; long target=Math.max(1,(long)Math.ceil(n*p)); long seen=0; for(int i=0;i<UPPER.length;i++){seen+=buckets.get(i);if(seen>=target)return UPPER[i];}return UPPER[UPPER.length-1]; }
  public String summary(){return "count="+count()+" avg_us="+(count()==0?0:totalMicros()/count())+" p50_us="+percentile(.50)+" p95_us="+percentile(.95)+" p99_us="+percentile(.99);}
}
