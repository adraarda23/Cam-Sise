package ardaaydinkilinc.Cam_Sise.logistics.domain.event;

import ardaaydinkilinc.Cam_Sise.shared.domain.event.DomainEvent;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record RouteAssignedToVehicle(
        Long collectionPlanId,
        Long vehicleId,
        LocalDate plannedDate,
        LocalDateTime occurredAt
) implements DomainEvent {
}
