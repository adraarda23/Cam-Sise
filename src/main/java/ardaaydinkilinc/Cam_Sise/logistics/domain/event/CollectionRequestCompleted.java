package ardaaydinkilinc.Cam_Sise.logistics.domain.event;

import ardaaydinkilinc.Cam_Sise.shared.domain.event.DomainEvent;

import java.time.LocalDateTime;

public record CollectionRequestCompleted(
        Long requestId,
        Long fillerId,
        LocalDateTime occurredAt
) implements DomainEvent {
}
