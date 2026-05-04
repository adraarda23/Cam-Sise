package ardaaydinkilinc.Cam_Sise.shared.infrastructure.event;

import ardaaydinkilinc.Cam_Sise.shared.domain.event.DomainEventStoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class DomainEventCleanupJob {

    private static final int RETENTION_DAYS = 15;

    private final DomainEventStoreRepository eventStoreRepository;

    // Her Pazar 03:00'da çalışır
    @Scheduled(cron = "0 0 3 * * SUN")
    @Transactional
    public void deleteOldEvents() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(RETENTION_DAYS);
        int deleted = eventStoreRepository.deleteByOccurredAtBefore(cutoff);
        log.info("Audit log cleanup: {} kayıt silindi (cutoff: {})", deleted, cutoff);
    }
}
