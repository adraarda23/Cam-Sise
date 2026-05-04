package ardaaydinkilinc.Cam_Sise.core.domain.event;

import ardaaydinkilinc.Cam_Sise.shared.domain.event.DomainEvent;

import java.time.LocalDateTime;

public record PoolOperatorUpdated(
        Long poolOperatorId,
        String updatedField,
        LocalDateTime occurredAt
) implements DomainEvent {
}
