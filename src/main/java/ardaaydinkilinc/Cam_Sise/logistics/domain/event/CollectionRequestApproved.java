package ardaaydinkilinc.Cam_Sise.logistics.domain.event;

import ardaaydinkilinc.Cam_Sise.shared.domain.event.DomainEvent;

import java.time.LocalDateTime;

public record CollectionRequestApproved(
        Long requestId,
        Long fillerId,
        Long approvedByUserId,
        LocalDateTime occurredAt
) implements DomainEvent {
}
