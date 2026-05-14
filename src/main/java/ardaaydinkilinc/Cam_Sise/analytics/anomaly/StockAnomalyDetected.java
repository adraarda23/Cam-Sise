package ardaaydinkilinc.Cam_Sise.analytics.anomaly;

import ardaaydinkilinc.Cam_Sise.inventory.domain.vo.AssetType;
import ardaaydinkilinc.Cam_Sise.shared.domain.event.DomainEvent;

import java.time.LocalDateTime;

/**
 * Domain event published when {@link AnomalyDetectionService} detects an
 * anomalous stock movement or stock level. Subscribed to by the notification
 * module to surface alerts to staff and customers.
 */
public record StockAnomalyDetected(
        Long fillerId,
        AssetType assetType,
        double observedValue,
        double expectedMean,
        double expectedStdDev,
        double zScore,
        AnomalySeverity severity,
        String reason,
        LocalDateTime occurredAt
) implements DomainEvent {

    public static StockAnomalyDetected from(AnomalyResult r) {
        return new StockAnomalyDetected(
                r.fillerId(),
                r.assetType(),
                r.observedValue(),
                r.expectedMean(),
                r.expectedStdDev(),
                r.zScore(),
                r.severity(),
                r.reason(),
                r.evaluatedAt()
        );
    }
}
