package ardaaydinkilinc.Cam_Sise.core.domain.event;

import ardaaydinkilinc.Cam_Sise.shared.domain.event.DomainEvent;

import java.time.LocalDateTime;

/**
 * Event fired when a filler is deactivated
 */
public record FillerDeactivated(
        Long fillerId,
        Long poolOperatorId,
        String fillerName,
        LocalDateTime occurredAt
) implements DomainEvent {
}
