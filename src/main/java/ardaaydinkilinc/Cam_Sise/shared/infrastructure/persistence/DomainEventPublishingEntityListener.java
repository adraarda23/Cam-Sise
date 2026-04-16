package ardaaydinkilinc.Cam_Sise.shared.infrastructure.persistence;

import ardaaydinkilinc.Cam_Sise.shared.domain.base.AggregateRoot;
import ardaaydinkilinc.Cam_Sise.shared.domain.event.DomainEventPublisher;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * JPA Entity Listener that publishes domain events after persistence operations.
 * This listener is automatically triggered by JPA lifecycle events.
 */
@Component
public class DomainEventPublishingEntityListener {

    private static DomainEventPublisher eventPublisher;

    @Autowired
    public void setEventPublisher(DomainEventPublisher eventPublisher) {
        DomainEventPublishingEntityListener.eventPublisher = eventPublisher;
    }

    /**
     * Publish domain events after entity is persisted
     */
    @PostPersist
    public void postPersist(Object entity) {
        publishEventsIfAggregate(entity);
    }

    /**
     * Publish domain events after entity is updated
     */
    @PostUpdate
    public void postUpdate(Object entity) {
        publishEventsIfAggregate(entity);
    }

    /**
     * Publish domain events after entity is removed
     */
    @PostRemove
    public void postRemove(Object entity) {
        publishEventsIfAggregate(entity);
    }

    /**
     * Check if entity is an AggregateRoot and publish its events
     */
    private void publishEventsIfAggregate(Object entity) {
        if (entity instanceof AggregateRoot<?> aggregate) {
            if (!aggregate.getDomainEvents().isEmpty()) {
                if (eventPublisher != null) {
                    eventPublisher.publishAll(aggregate.getDomainEvents());
                    aggregate.clearDomainEvents();
                }
            }
        }
    }
}
