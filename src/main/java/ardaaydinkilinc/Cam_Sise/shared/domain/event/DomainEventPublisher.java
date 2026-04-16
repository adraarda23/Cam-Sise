package ardaaydinkilinc.Cam_Sise.shared.domain.event;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service for publishing domain events to Spring's event bus
 */
@Service
public class DomainEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    public DomainEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    /**
     * Publish a single domain event
     */
    public void publish(DomainEvent event) {
        applicationEventPublisher.publishEvent(event);
    }

    /**
     * Publish multiple domain events
     */
    public void publishAll(List<DomainEvent> events) {
        events.forEach(this::publish);
    }
}
