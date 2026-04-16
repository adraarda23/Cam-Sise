package ardaaydinkilinc.Cam_Sise.logistics.domain.event;

import ardaaydinkilinc.Cam_Sise.shared.domain.event.DomainEvent;
import ardaaydinkilinc.Cam_Sise.shared.domain.vo.Distance;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record CollectionPlanGenerated(
        Long depotId,
        Distance totalDistance,
        int totalPallets,
        int totalSeparators,
        LocalDate plannedDate,
        LocalDateTime occurredAt
) implements DomainEvent {
}
