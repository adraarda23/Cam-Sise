package ardaaydinkilinc.Cam_Sise.auth.domain.event;

import ardaaydinkilinc.Cam_Sise.shared.domain.event.DomainEvent;

import java.time.LocalDateTime;

public record UserLoggedIn(
        Long userId,
        Long poolOperatorId,
        String username,
        String role,
        LocalDateTime occurredAt
) implements DomainEvent {
}
