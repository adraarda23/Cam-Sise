package ardaaydinkilinc.Cam_Sise.shared.domain.event;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for domain event audit logging
 */
@Repository
public interface DomainEventStoreRepository extends JpaRepository<DomainEventStore, Long> {

    /**
     * Find all events of a specific type
     */
    List<DomainEventStore> findByEventType(String eventType);

    /**
     * Find all events for a specific aggregate
     */
    List<DomainEventStore> findByAggregateIdAndAggregateType(String aggregateId, String aggregateType);

    /**
     * Find all events within a time range
     */
    List<DomainEventStore> findByOccurredAtBetween(LocalDateTime start, LocalDateTime end);

    List<DomainEventStore> findByAggregateType(String aggregateType);

    int deleteByOccurredAtBefore(LocalDateTime cutoff);
}
