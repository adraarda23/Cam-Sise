package ardaaydinkilinc.Cam_Sise.shared.domain.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Listens to all domain events and stores them for audit logging
 */
@Component
public class DomainEventListener {

    private static final Logger logger = LoggerFactory.getLogger(DomainEventListener.class);

    private final DomainEventStoreRepository eventStoreRepository;
    private final ObjectMapper objectMapper;

    public DomainEventListener(DomainEventStoreRepository eventStoreRepository) {
        this.eventStoreRepository = eventStoreRepository;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * Listen to all domain events and store them
     */
    @Async
    @EventListener
    public void handleDomainEvent(DomainEvent event) {
        try {
            String eventData = objectMapper.writeValueAsString(event);

            DomainEventStore eventStore = DomainEventStore.builder()
                    .eventType(event.eventType())
                    .eventData(eventData)
                    .occurredAt(event.occurredAt())
                    .build();

            eventStoreRepository.save(eventStore);

            logger.info("Domain event stored: {}", event.eventType());

        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize domain event: {}", event.eventType(), e);
        } catch (Exception e) {
            logger.error("Failed to store domain event: {}", event.eventType(), e);
        }
    }
}
