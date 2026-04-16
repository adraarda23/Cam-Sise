package ardaaydinkilinc.Cam_Sise.logistics.domain.event;

import ardaaydinkilinc.Cam_Sise.logistics.domain.vo.Capacity;
import ardaaydinkilinc.Cam_Sise.shared.domain.event.DomainEvent;

import java.time.LocalDateTime;

public record VehicleTypeCreated(
        Long poolOperatorId,
        String name,
        Capacity capacity,
        LocalDateTime occurredAt
) implements DomainEvent {
}
