package ardaaydinkilinc.Cam_Sise.core.domain.event;

import ardaaydinkilinc.Cam_Sise.shared.domain.event.DomainEvent;
import ardaaydinkilinc.Cam_Sise.shared.domain.vo.GeoCoordinates;

import java.time.LocalDateTime;

/**
 * Event fired when a new filler is registered
 */
public record FillerRegistered(
        Long poolOperatorId,
        String fillerName,
        GeoCoordinates location,
        LocalDateTime occurredAt
) implements DomainEvent {
}
