package ardaaydinkilinc.Cam_Sise.logistics.domain.event;

import ardaaydinkilinc.Cam_Sise.shared.domain.event.DomainEvent;
import ardaaydinkilinc.Cam_Sise.shared.domain.vo.GeoCoordinates;

import java.time.LocalDateTime;

public record DepotCreated(
        Long poolOperatorId,
        String depotName,
        GeoCoordinates location,
        LocalDateTime occurredAt
) implements DomainEvent {
}
