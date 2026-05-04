package ardaaydinkilinc.Cam_Sise.logistics.domain.event;

import ardaaydinkilinc.Cam_Sise.shared.domain.event.DomainEvent;

import java.time.LocalDateTime;

public record CollectionRequestScheduled(
        Long requestId,
        Long fillerId,
        Long planId,
        LocalDateTime occurredAt
) implements DomainEvent {
}
