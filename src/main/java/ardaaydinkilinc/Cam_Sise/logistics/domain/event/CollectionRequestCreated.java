package ardaaydinkilinc.Cam_Sise.logistics.domain.event;

import ardaaydinkilinc.Cam_Sise.inventory.domain.vo.AssetType;
import ardaaydinkilinc.Cam_Sise.logistics.domain.vo.RequestSource;
import ardaaydinkilinc.Cam_Sise.shared.domain.event.DomainEvent;

import java.time.LocalDateTime;

public record CollectionRequestCreated(
        Long fillerId,
        AssetType assetType,
        int estimatedQuantity,
        RequestSource source,
        LocalDateTime occurredAt
) implements DomainEvent {
}
