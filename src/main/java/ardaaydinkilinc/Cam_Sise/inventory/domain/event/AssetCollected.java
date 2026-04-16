package ardaaydinkilinc.Cam_Sise.inventory.domain.event;

import ardaaydinkilinc.Cam_Sise.inventory.domain.vo.AssetType;
import ardaaydinkilinc.Cam_Sise.shared.domain.event.DomainEvent;

import java.time.LocalDateTime;

/**
 * Event fired when assets are collected from a filler by pool operator
 */
public record AssetCollected(
        Long fillerStockId,
        Long fillerId,
        AssetType assetType,
        int quantity,
        int remainingQuantity,
        String collectionPlanId,
        LocalDateTime occurredAt
) implements DomainEvent {
}
