package ardaaydinkilinc.Cam_Sise.shared.domain.event;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity for storing domain events for audit logging
 */
@Entity
@Table(name = "domain_events")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DomainEventStore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Event type (e.g., "AssetCollected", "CollectionPlanGenerated")
     */
    @Column(nullable = false)
    private String eventType;

    /**
     * Serialized event data (JSON)
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String eventData;

    /**
     * When the event occurred in the domain
     */
    @Column(nullable = false)
    private LocalDateTime occurredAt;

    /**
     * When the event was stored in the database
     */
    @Column(nullable = false)
    private LocalDateTime storedAt;

    /**
     * Aggregate ID that emitted the event (optional, for querying)
     */
    @Column(name = "aggregate_id")
    private String aggregateId;

    /**
     * Aggregate type (e.g., "FillerStock", "CollectionPlan")
     */
    @Column(name = "aggregate_type")
    private String aggregateType;

    @PrePersist
    protected void onCreate() {
        storedAt = LocalDateTime.now();
    }
}
