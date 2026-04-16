package ardaaydinkilinc.Cam_Sise.inventory.domain.event;

import ardaaydinkilinc.Cam_Sise.inventory.domain.vo.AssetType;
import ardaaydinkilinc.Cam_Sise.inventory.domain.vo.Period;
import ardaaydinkilinc.Cam_Sise.shared.domain.event.DomainEvent;

import java.time.LocalDateTime;

/**
 * Event fired when a filler updates their actual loss rate
 */
public record LossRecordUpdated(
        Long lossRecordId,
        Long fillerId,
        AssetType assetType,
        double actualLossRatePercentage,
        Period calculationPeriod,
        LocalDateTime occurredAt
) implements DomainEvent {
}
