package ardaaydinkilinc.Cam_Sise.logistics.domain.event;

import ardaaydinkilinc.Cam_Sise.logistics.domain.vo.DriverInfo;
import ardaaydinkilinc.Cam_Sise.shared.domain.event.DomainEvent;

import java.time.LocalDateTime;

public record VehicleAssignedToRoute(
        Long vehicleId,
        Long collectionPlanId,
        DriverInfo driver,
        LocalDateTime occurredAt
) implements DomainEvent {
}
