package ardaaydinkilinc.Cam_Sise.logistics.domain.event;

import ardaaydinkilinc.Cam_Sise.shared.domain.event.DomainEvent;

import java.time.LocalDateTime;

public record VehicleTypeDeactivated(
        Long vehicleTypeId,
        Long poolOperatorId,
        String name,
        LocalDateTime occurredAt
) implements DomainEvent {
}
