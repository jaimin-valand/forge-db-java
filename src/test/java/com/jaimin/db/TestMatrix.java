package com.jaimin.db;

import java.util.*;

/**
 * Dependency-free test orchestrator for CI environments where JUnit is unavailable.
 * The same release gate can therefore validate the critical database path with only JDK 21.
 */
public final class TestMatrix {
    private record Case(String name, String className) {}

    private static final List<Case> CASES = List.of(
        new Case("transaction state machine", "com.jaimin.db.TransactionStateMachineTest"),
        new Case("MVCC CRUD", "com.jaimin.db.MvccCrudTest"),
        new Case("aggregation", "com.jaimin.db.AggregationQueryTest"),
        new Case("advanced optimizer", "com.jaimin.db.query.AdvancedQueryOptimizerTest"),
        new Case("crash recovery", "com.jaimin.db.reliability.CrashRecoveryIntegration"),
        new Case("integrity hardening", "com.jaimin.db.reliability.IntegrityHardeningTest"),
        new Case("security hardening", "com.jaimin.db.sql.SecurityHardeningTest"),
        new Case("security fuzz regression", "com.jaimin.db.sql.SecurityFuzzRegressionTest"),
        new Case("storage engine v2", "com.jaimin.db.storage.StorageEngineV2Test"),
        new Case("end-to-end integration", "com.jaimin.db.integration.ForgeDbIntegrationSuite"),
        new Case("storage/query invariant properties", "com.jaimin.db.PropertyInvariantTest")
    );

    public static void main(String[] args) throws Exception {
        long started = System.nanoTime();
        int passed = 0;
        for (Case test : CASES) {
            long t0 = System.nanoTime();
            try {
                Class<?> type = Class.forName(test.className());
                type.getMethod("main", String[].class).invoke(null, (Object) new String[0]);
                passed++;
                System.out.printf(Locale.ROOT, "TEST PASS  %-32s %8.2f ms%n", test.name(), elapsedMs(t0));
            } catch (Throwable failure) {
                Throwable cause = failure instanceof java.lang.reflect.InvocationTargetException ite && ite.getCause() != null
                    ? ite.getCause() : failure;
                System.err.printf(Locale.ROOT, "TEST FAIL  %-32s %8.2f ms%n", test.name(), elapsedMs(t0));
                cause.printStackTrace(System.err);
                throw new AssertionError("release gate stopped at: " + test.name(), cause);
            }
        }
        System.out.printf(Locale.ROOT, "%nPASS: dependency-free test matrix (%d/%d), %.2f ms total%n",
            passed, CASES.size(), elapsedMs(started));
    }

    private static double elapsedMs(long started) {
        return (System.nanoTime() - started) / 1_000_000.0;
    }
}
