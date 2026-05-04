package ardaaydinkilinc.Cam_Sise.auth.domain.event;

import ardaaydinkilinc.Cam_Sise.shared.domain.event.DomainEvent;

import java.time.LocalDateTime;

public record UserDeactivated(
        Long userId,
        Long poolOperatorId,
        String username,
        LocalDateTime occurredAt
) implements DomainEvent {
}
