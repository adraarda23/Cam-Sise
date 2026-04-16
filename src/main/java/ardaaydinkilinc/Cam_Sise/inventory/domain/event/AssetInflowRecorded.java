package ardaaydinkilinc.Cam_Sise.inventory.domain.event;

import ardaaydinkilinc.Cam_Sise.inventory.domain.vo.AssetType;
import ardaaydinkilinc.Cam_Sise.shared.domain.event.DomainEvent;

import java.time.LocalDateTime;

/**
 * Event fired when assets flow into a filler from glass manufacturer
 */
public record AssetInflowRecorded(
        Long fillerStockId,
        Long fillerId,
        AssetType assetType,
        int quantity,
        int newTotalQuantity,
        String referenceId,
        LocalDateTime occurredAt
) implements DomainEvent {
}
