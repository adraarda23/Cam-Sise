package ardaaydinkilinc.Cam_Sise.shared.domain.event;

import java.time.LocalDateTime;

/**
 * Base interface for all domain events.
 * Domain events represent something that happened in the domain that domain experts care about.
 */
public interface DomainEvent {

    /**
     * When the event occurred
     */
    LocalDateTime occurredAt();

    /**
     * Event type identifier (used for serialization/deserialization)
     */
    default String eventType() {
        return this.getClass().getSimpleName();
    }
}
