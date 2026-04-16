package ardaaydinkilinc.Cam_Sise.logistics.domain.event;

import ardaaydinkilinc.Cam_Sise.shared.domain.event.DomainEvent;

import java.time.LocalDateTime;

public record VehicleRegistered(
        Long depotId,
        Long vehicleTypeId,
        String plateNumber,
        LocalDateTime occurredAt
) implements DomainEvent {
}
