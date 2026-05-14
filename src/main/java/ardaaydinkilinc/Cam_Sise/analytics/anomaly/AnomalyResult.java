package ardaaydinkilinc.Cam_Sise.analytics.anomaly;

import ardaaydinkilinc.Cam_Sise.inventory.domain.vo.AssetType;

import java.time.LocalDateTime;

/**
 * Output of an anomaly check on a single (filler, assetType, day) triple.
 *
 * <p><strong>Fields</strong>:
 * <ul>
 *   <li>{@code expectedMean / expectedStdDev}: baseline computed from history
 *       (same day-of-week, mean &amp; stddev).</li>
 *   <li>{@code observedValue}: the actual quantity that triggered evaluation
 *       (e.g. today's net inflow or current stock level).</li>
 *   <li>{@code zScore}: standardised deviation; sign tells you which direction
 *       the anomaly leans (positive = unexpectedly high).</li>
 *   <li>{@code severity}: bucketed verdict from |z|.</li>
 *   <li>{@code reason}: human-readable diagnostic for the UI/notification.</li>
 * </ul>
 */
public record AnomalyResult(
        Long fillerId,
        AssetType assetType,
        LocalDateTime evaluatedAt,
        double observedValue,
        double expectedMean,
        double expectedStdDev,
        double zScore,
        AnomalySeverity severity,
        String reason
) {

    public boolean isAnomaly() {
        return severity.isAlarmWorthy();
    }

    public double lowerExpected() {
        return expectedMean - AnomalySeverity.WARNING_THRESHOLD * expectedStdDev;
    }

    public double upperExpected() {
        return expectedMean + AnomalySeverity.WARNING_THRESHOLD * expectedStdDev;
    }

    public static AnomalyResult normal(Long fillerId, AssetType assetType, String reason) {
        return new AnomalyResult(
                fillerId, assetType, LocalDateTime.now(),
                0, 0, 0, 0, AnomalySeverity.NORMAL, reason);
    }
}
