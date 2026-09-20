package com.jaimin.db;

/** Small allocation-free timing helper for hot paths. */
public final class PerformanceTimer {
  private PerformanceTimer() {}
  public static long now() { return System.nanoTime(); }
  public static long elapsedNanos(long start) { return System.nanoTime() - start; }
}
