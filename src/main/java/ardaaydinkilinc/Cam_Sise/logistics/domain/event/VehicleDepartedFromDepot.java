package ardaaydinkilinc.Cam_Sise.logistics.domain.event;

import ardaaydinkilinc.Cam_Sise.shared.domain.event.DomainEvent;

import java.time.LocalDateTime;

public record VehicleDepartedFromDepot(
        Long vehicleId,
        Long depotId,
        Long collectionPlanId,
        LocalDateTime occurredAt
) implements DomainEvent {
}
