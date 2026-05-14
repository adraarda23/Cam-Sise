package ardaaydinkilinc.Cam_Sise.notification.service.event;

import ardaaydinkilinc.Cam_Sise.analytics.anomaly.StockAnomalyDetected;
import ardaaydinkilinc.Cam_Sise.core.domain.Filler;
import ardaaydinkilinc.Cam_Sise.core.repository.FillerRepository;
import ardaaydinkilinc.Cam_Sise.inventory.domain.event.StockThresholdExceeded;
import ardaaydinkilinc.Cam_Sise.logistics.domain.event.CollectionPlanGenerated;
import ardaaydinkilinc.Cam_Sise.logistics.domain.event.CollectionRequestApproved;
import ardaaydinkilinc.Cam_Sise.logistics.domain.event.CollectionRequestCompleted;
import ardaaydinkilinc.Cam_Sise.logistics.domain.event.CollectionRequestCreated;
import ardaaydinkilinc.Cam_Sise.logistics.domain.vo.RequestSource;
import ardaaydinkilinc.Cam_Sise.notification.domain.vo.NotificationSeverity;
import ardaaydinkilinc.Cam_Sise.notification.domain.vo.NotificationType;
import ardaaydinkilinc.Cam_Sise.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Translates domain events into persistent notifications.
 *
 * <p>Replaces the 30+ {@code // TODO: Send notification} markers that were
 * scattered across InventoryEventHandler / LogisticsEventHandler / CoreEventHandler.
 * Each listener is {@code @Async} so persistence happens on a background thread
 * and does not block the originating transaction.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventHandler {

    private final NotificationService notificationService;
    private final FillerRepository fillerRepository;

    @Async
    @EventListener
    public void onAnomaly(StockAnomalyDetected event) {
        Optional<Filler> filler = fillerRepository.findById(event.fillerId());
        Long poolOperatorId = filler.map(Filler::getPoolOperatorId).orElse(null);
        String fillerName = filler.map(Filler::getName).orElse("Dolumcu #" + event.fillerId());

        NotificationSeverity severity = switch (event.severity()) {
            case CRITICAL -> NotificationSeverity.CRITICAL;
            case WARNING -> NotificationSeverity.WARNING;
            default -> NotificationSeverity.INFO;
        };

        String title = String.format("Anomali tespit edildi — %s (%s)", fillerName, event.assetType());
        String body = event.reason()
                + String.format("%nGözlem: %.1f, Beklenen ortalama: %.1f ± %.1f, z-score: %.2f",
                event.observedValue(), event.expectedMean(), event.expectedStdDev(), event.zScore());
        String actionUrl = "/fillers/" + event.fillerId();

        if (poolOperatorId != null) {
            notificationService.notifyAllStaff(
                    poolOperatorId, event.fillerId(),
                    NotificationType.STOCK_ANOMALY, severity,
                    title, body, actionUrl);
        }
        notificationService.notifyFillerCustomer(
                event.fillerId(), poolOperatorId,
                NotificationType.STOCK_ANOMALY, severity,
                title, body, actionUrl);
    }

    @Async
    @EventListener
    public void onThresholdExceeded(StockThresholdExceeded event) {
        Optional<Filler> filler = fillerRepository.findById(event.fillerId());
        Long poolOperatorId = filler.map(Filler::getPoolOperatorId).orElse(null);
        String fillerName = filler.map(Filler::getName).orElse("Dolumcu #" + event.fillerId());

        String title = String.format("Stok eşiği aşıldı — %s (%s)", fillerName, event.assetType());
        String body = String.format("Mevcut: %d / Eşik: %d. Otomatik toplama talebi oluşturuldu.",
                event.currentQuantity(), event.thresholdQuantity());
        String actionUrl = "/requests";

        if (poolOperatorId != null) {
            notificationService.notifyAllStaff(
                    poolOperatorId, event.fillerId(),
                    NotificationType.STOCK_THRESHOLD_EXCEEDED, NotificationSeverity.WARNING,
                    title, body, actionUrl);
        }
    }

    @Async
    @EventListener
    public void onRequestCreated(CollectionRequestCreated event) {
        // Only manual customer requests trigger a staff notification — automatic
        // threshold-driven requests already have their own StockThresholdExceeded notice.
        if (event.source() != RequestSource.MANUAL_CUSTOMER) return;

        Optional<Filler> filler = fillerRepository.findById(event.fillerId());
        Long poolOperatorId = filler.map(Filler::getPoolOperatorId).orElse(null);
        String fillerName = filler.map(Filler::getName).orElse("Dolumcu #" + event.fillerId());
        if (poolOperatorId == null) return;

        String assetLabel = event.assetType() == ardaaydinkilinc.Cam_Sise.inventory.domain.vo.AssetType.PALLET
                ? "palet" : "ayırıcı";
        String title = String.format("Yeni toplama talebi — %s", fillerName);
        String body = String.format("%s tarafından %d adet %s için yeni toplama talebi oluşturuldu. Onayınızı bekliyor.",
                fillerName, event.estimatedQuantity(), assetLabel);

        notificationService.notifyAllStaff(
                poolOperatorId, event.fillerId(),
                NotificationType.COLLECTION_REQUEST_CREATED, NotificationSeverity.INFO,
                title, body, "/requests");
    }

    @Async
    @EventListener
    public void onRequestApproved(CollectionRequestApproved event) {
        notificationService.notifyFillerCustomer(
                event.fillerId(), null,
                NotificationType.COLLECTION_REQUEST_APPROVED, NotificationSeverity.INFO,
                "Toplama talebiniz onaylandı",
                String.format("Talep #%d onaylandı.", event.requestId()),
                "/my-requests");
    }

    @Async
    @EventListener
    public void onRequestCompleted(CollectionRequestCompleted event) {
        notificationService.notifyFillerCustomer(
                event.fillerId(), null,
                NotificationType.COLLECTION_REQUEST_COMPLETED,
                NotificationSeverity.INFO,
                "Toplama tamamlandı",
                String.format("Talep #%d için toplama tamamlandı.", event.requestId()),
                "/my-requests");
    }

    @Async
    @EventListener
    public void onPlanGenerated(CollectionPlanGenerated event) {
        // Resolve pool operator via depot lookup is heavier than needed —
        // depots all belong to one operator in the current tenant model, so we
        // simply broadcast to anyone whose depot matches. For now we skip if
        // we cannot identify the operator without extra queries; the demo
        // notification can be added later via depot lookup.
        log.debug("Plan generated for depot {} — staff notification suppressed (no pool operator on event)",
                event.depotId());
    }
}
