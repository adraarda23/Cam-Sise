package ardaaydinkilinc.Cam_Sise.inventory.domain.event;

import ardaaydinkilinc.Cam_Sise.inventory.domain.vo.AssetType;
import ardaaydinkilinc.Cam_Sise.shared.domain.event.DomainEvent;

import java.time.LocalDateTime;

/**
 * Event fired when a filler's stock exceeds the collection threshold
 * This triggers automatic collection request creation in Logistics module
 */
public record StockThresholdExceeded(
        Long fillerStockId,
        Long fillerId,
        AssetType assetType,
        int currentQuantity,
        int thresholdQuantity,
        LocalDateTime occurredAt
) implements DomainEvent {
}
