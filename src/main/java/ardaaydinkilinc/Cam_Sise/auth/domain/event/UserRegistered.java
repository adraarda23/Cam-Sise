package ardaaydinkilinc.Cam_Sise.auth.domain.event;

import ardaaydinkilinc.Cam_Sise.auth.domain.Role;
import ardaaydinkilinc.Cam_Sise.shared.domain.event.DomainEvent;

import java.time.LocalDateTime;

public record UserRegistered(
        Long poolOperatorId,
        String username,
        Role role,
        Long fillerId,
        LocalDateTime occurredAt
) implements DomainEvent {
}
