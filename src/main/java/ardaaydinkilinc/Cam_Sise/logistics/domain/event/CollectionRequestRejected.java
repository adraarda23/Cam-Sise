package ardaaydinkilinc.Cam_Sise.logistics.domain.event;

import ardaaydinkilinc.Cam_Sise.shared.domain.event.DomainEvent;

import java.time.LocalDateTime;

public record CollectionRequestRejected(
        Long requestId,
        Long fillerId,
        String reason,
        LocalDateTime occurredAt
) implements DomainEvent {
}
