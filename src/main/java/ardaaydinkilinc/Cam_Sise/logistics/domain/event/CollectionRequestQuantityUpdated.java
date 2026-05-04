package ardaaydinkilinc.Cam_Sise.logistics.domain.event;

import ardaaydinkilinc.Cam_Sise.shared.domain.event.DomainEvent;

import java.time.LocalDateTime;

public record CollectionRequestQuantityUpdated(
        Long requestId,
        Long fillerId,
        int oldQuantity,
        int newQuantity,
        LocalDateTime occurredAt
) implements DomainEvent {
}
