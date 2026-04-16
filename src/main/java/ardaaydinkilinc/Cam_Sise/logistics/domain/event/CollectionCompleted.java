package ardaaydinkilinc.Cam_Sise.logistics.domain.event;

import ardaaydinkilinc.Cam_Sise.shared.domain.event.DomainEvent;

import java.time.LocalDateTime;

/**
 * Event fired when a collection is completed
 * This triggers stock updates in Inventory module
 */
public record CollectionCompleted(
        Long collectionPlanId,
        Long vehicleId,
        Long depotId,
        int actualPalletsCollected,
        int actualSeparatorsCollected,
        LocalDateTime occurredAt
) implements DomainEvent {
}
