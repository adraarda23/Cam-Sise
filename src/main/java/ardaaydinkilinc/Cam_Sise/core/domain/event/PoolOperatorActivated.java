package ardaaydinkilinc.Cam_Sise.core.domain.event;

import ardaaydinkilinc.Cam_Sise.shared.domain.event.DomainEvent;

import java.time.LocalDateTime;

/**
 * Event fired when a pool operator is activated
 */
public record PoolOperatorActivated(
        Long operatorId,
        String companyName,
        LocalDateTime occurredAt
) implements DomainEvent {
}
