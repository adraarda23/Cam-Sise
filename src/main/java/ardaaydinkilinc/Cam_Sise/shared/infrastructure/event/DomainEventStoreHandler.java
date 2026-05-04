package ardaaydinkilinc.Cam_Sise.shared.infrastructure.event;

import ardaaydinkilinc.Cam_Sise.shared.domain.event.DomainEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@Slf4j
public class DomainEventStoreHandler {

    @Autowired(required = false)
    private DomainEventSearchRepository searchRepository;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @EventListener
    @Async
    public void handleDomainEvent(DomainEvent event) {
        if (searchRepository == null) {
            log.debug("Elasticsearch mevcut değil, audit log atlandı: {}", event.getClass().getSimpleName());
            return;
        }

        try {
            DomainEventDocument doc = DomainEventDocument.builder()
                    .id(UUID.randomUUID().toString())
                    .eventType(event.getClass().getSimpleName())
                    .eventData(serializeEvent(event))
                    .occurredAt(extractOccurredAt(event))
                    .storedAt(LocalDateTime.now())
                    .aggregateId(extractAggregateId(event))
                    .aggregateType(extractAggregateType(event))
                    .build();

            searchRepository.save(doc);
            log.debug("Audit log kaydedildi (ES): {}", event.getClass().getSimpleName());

        } catch (Exception e) {
            log.error("Audit log kaydedilemedi: {}", event.getClass().getSimpleName(), e);
        }
    }

    private String serializeEvent(DomainEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            log.error("Event serialize edilemedi: {}", event.getClass().getSimpleName(), e);
            return "{}";
        }
    }

    private LocalDateTime extractOccurredAt(DomainEvent event) {
        try {
            var method = event.getClass().getMethod("occurredAt");
            return (LocalDateTime) method.invoke(event);
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }

    private String extractAggregateId(DomainEvent event) {
        for (String fieldName : new String[]{"id", "aggregateId", "fillerId", "userId", "depotId"}) {
            try {
                var method = event.getClass().getMethod(fieldName);
                Object result = method.invoke(event);
                if (result != null) return result.toString();
            } catch (NoSuchMethodException ignored) {
            } catch (Exception e) {
                log.debug("aggregateId çıkarılamadı: {}", event.getClass().getSimpleName());
            }
        }
        return null;
    }

    private String extractAggregateType(DomainEvent event) {
        return event.getClass().getSimpleName()
                .replaceAll("(Registered|Created|Updated|Deleted|Activated|Deactivated|Changed|Approved|Rejected)$", "");
    }
}
