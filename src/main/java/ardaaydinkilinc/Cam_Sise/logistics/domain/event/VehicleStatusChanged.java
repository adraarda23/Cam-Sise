package ardaaydinkilinc.Cam_Sise.logistics.domain.event;

import ardaaydinkilinc.Cam_Sise.logistics.domain.vo.VehicleStatus;
import ardaaydinkilinc.Cam_Sise.shared.domain.event.DomainEvent;

import java.time.LocalDateTime;

public record VehicleStatusChanged(
        Long vehicleId,
        VehicleStatus oldStatus,
        VehicleStatus newStatus,
        LocalDateTime occurredAt
) implements DomainEvent {
}
