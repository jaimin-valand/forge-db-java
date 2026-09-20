package com.jaimin.db.reliability;

/** Deterministic, test-only fault injection hooks. Disabled by default. */
public final class FaultInjector {
  private FaultInjector() {}

  public enum Point { AFTER_BEGIN, AFTER_MUTATION, AFTER_WAL_COMMIT }

  public static void hit(Point point) {
    String configured = System.getProperty("forge.fault.point", "OFF");
    String legacyAction = System.getProperty("forge.fault.afterWalCommit");
    if (point == Point.AFTER_WAL_COMMIT && legacyAction != null) {
      configured = point.name();
      System.setProperty("forge.fault.action", legacyAction);
    }
    if (!point.name().equalsIgnoreCase(configured)) return;
    String action = System.getProperty("forge.fault.action", "HALT");
    if ("THROW".equalsIgnoreCase(action)) {
      throw new SimulatedFaultException(point);
    }
    if ("HALT".equalsIgnoreCase(action)) {
      Runtime.getRuntime().halt(86);
    }
  }

  /** Backward-compatible hook retained for the existing crash scenario. */
  public static void afterWalCommit() { hit(Point.AFTER_WAL_COMMIT); }

  public static final class SimulatedFaultException extends RuntimeException {
    public SimulatedFaultException(Point point) {
      super("simulated fault at " + point);
    }
  }
}
