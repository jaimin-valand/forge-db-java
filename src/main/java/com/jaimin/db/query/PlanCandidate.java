package com.jaimin.db.query;

/** Cost estimate for one physical access strategy. */
public record PlanCandidate(String strategy, double estimatedRows, double estimatedCost, String rationale) {
    @Override public String toString() {
        return strategy + "(rows=" + fmt(estimatedRows) + ", cost=" + fmt(estimatedCost) + ", " + rationale + ")";
    }
    private static String fmt(double v) { return String.format(java.util.Locale.ROOT, "%.2f", v); }
}
