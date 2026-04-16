package ardaaydinkilinc.Cam_Sise.inventory.domain.event;

import ardaaydinkilinc.Cam_Sise.inventory.domain.vo.AssetType;
import ardaaydinkilinc.Cam_Sise.inventory.domain.vo.Period;
import ardaaydinkilinc.Cam_Sise.shared.domain.event.DomainEvent;

import java.time.LocalDateTime;

/**
 * Event fired when the system calculates an estimated loss rate using moving average
 */
public record EstimatedLossRateCalculated(
        Long fillerId,
        AssetType assetType,
        double estimatedLossRatePercentage,
        Period calculationPeriod,
        LocalDateTime occurredAt
) implements DomainEvent {
}
