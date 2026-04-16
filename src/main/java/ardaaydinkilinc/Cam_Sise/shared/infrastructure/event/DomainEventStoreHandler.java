package ardaaydinkilinc.Cam_Sise.shared.infrastructure.event;

import ardaaydinkilinc.Cam_Sise.shared.domain.event.DomainEvent;
import ardaaydinkilinc.Cam_Sise.shared.domain.event.DomainEventStore;
import ardaaydinkilinc.Cam_Sise.shared.domain.event.DomainEventStoreRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Universal handler that stores all domain events to the event store for audit logging.
 * This handler listens to all DomainEvent instances and persists them to the database.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DomainEventStoreHandler {

    private final DomainEventStoreRepository eventStoreRepository;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    /**
     * Store all domain events to the database for audit logging.
     * This method runs asynchronously and in a new transaction to avoid affecting the main transaction.
     */
    @EventListener
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleDomainEvent(DomainEvent event) {
        try {
            log.debug("Storing domain event: {}", event.getClass().getSimpleName());

            DomainEventStore eventStore = DomainEventStore.builder()
                    .eventType(event.getClass().getSimpleName())
                    .eventData(serializeEvent(event))
                    .occurredAt(extractOccurredAt(event))
                    .aggregateId(extractAggregateId(event))
                    .aggregateType(extractAggregateType(event))
                    .build();

            eventStoreRepository.save(eventStore);

            log.info("Domain event stored: {}", event.getClass().getSimpleName());

        } catch (Exception e) {
            log.error("Failed to store domain event: {}", event.getClass().getSimpleName(), e);
            // Don't throw exception - we don't want to break the main flow
        }
    }

    /**
     * Serialize event to JSON
     */
    private String serializeEvent(DomainEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize event: {}", event.getClass().getSimpleName(), e);
            return "{}";
        }
    }

    /**
     * Extract occurredAt timestamp from event using reflection
     */
    private LocalDateTime extractOccurredAt(DomainEvent event) {
        try {
            var method = event.getClass().getMethod("occurredAt");
            return (LocalDateTime) method.invoke(event);
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }

    /**
     * Extract aggregate ID from event using reflection (if exists)
     */
    private String extractAggregateId(DomainEvent event) {
        try {
            // Try common field names
            for (String fieldName : new String[]{"id", "aggregateId", "fillerId", "userId", "depotId"}) {
                try {
                    var method = event.getClass().getMethod(fieldName);
                    Object result = method.invoke(event);
                    if (result != null) {
                        return result.toString();
                    }
                } catch (NoSuchMethodException ignored) {
                }
            }
        } catch (Exception e) {
            log.debug("Could not extract aggregate ID from event: {}", event.getClass().getSimpleName());
        }
        return null;
    }

    /**
     * Extract aggregate type from event class name
     */
    private String extractAggregateType(DomainEvent event) {
        String eventName = event.getClass().getSimpleName();
        // Remove common event suffixes to get aggregate type
        return eventName.replaceAll("(Registered|Created|Updated|Deleted|Activated|Deactivated|Changed|Approved|Rejected)$", "");
    }
}
