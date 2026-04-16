package ardaaydinkilinc.Cam_Sise.core.domain.event;

import ardaaydinkilinc.Cam_Sise.shared.domain.event.DomainEvent;

import java.time.LocalDateTime;

/**
 * Event fired when a new pool operator registers to the system
 */
public record PoolOperatorRegistered(
        String companyName,
        String taxId,
        LocalDateTime occurredAt
) implements DomainEvent {
}
