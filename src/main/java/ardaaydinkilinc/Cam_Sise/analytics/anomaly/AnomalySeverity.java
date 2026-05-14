package ardaaydinkilinc.Cam_Sise.analytics.anomaly;

/**
 * Severity grades for detected anomalies, mapped from |z-score|.
 *
 * <ul>
 *   <li>{@link #NORMAL}      → |z| &lt; 2.0  (within ~95% of the expected band)</li>
 *   <li>{@link #WARNING}     → 2.0 ≤ |z| &lt; 3.0</li>
 *   <li>{@link #CRITICAL}    → |z| ≥ 3.0</li>
 * </ul>
 */
public enum AnomalySeverity {
    NORMAL,
    WARNING,
    CRITICAL;

    public static final double WARNING_THRESHOLD = 2.0;
    public static final double CRITICAL_THRESHOLD = 3.0;

    public static AnomalySeverity fromZScore(double zScore) {
        double abs = Math.abs(zScore);
        if (abs >= CRITICAL_THRESHOLD) return CRITICAL;
        if (abs >= WARNING_THRESHOLD) return WARNING;
        return NORMAL;
    }

    public boolean isAlarmWorthy() {
        return this == WARNING || this == CRITICAL;
    }
}
