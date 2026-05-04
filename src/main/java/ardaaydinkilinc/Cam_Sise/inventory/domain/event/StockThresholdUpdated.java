package ardaaydinkilinc.Cam_Sise.inventory.domain.event;

import ardaaydinkilinc.Cam_Sise.inventory.domain.vo.AssetType;
import ardaaydinkilinc.Cam_Sise.shared.domain.event.DomainEvent;

import java.time.LocalDateTime;

public record StockThresholdUpdated(
        Long stockId,
        Long fillerId,
        AssetType assetType,
        int oldThreshold,
        int newThreshold,
        LocalDateTime occurredAt
) implements DomainEvent {
}
