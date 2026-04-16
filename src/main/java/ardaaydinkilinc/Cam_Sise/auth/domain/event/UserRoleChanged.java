package ardaaydinkilinc.Cam_Sise.auth.domain.event;

import ardaaydinkilinc.Cam_Sise.auth.domain.Role;
import ardaaydinkilinc.Cam_Sise.shared.domain.event.DomainEvent;

import java.time.LocalDateTime;

public record UserRoleChanged(
        Long userId,
        Long poolOperatorId,
        Role oldRole,
        Role newRole,
        LocalDateTime occurredAt
) implements DomainEvent {
}
