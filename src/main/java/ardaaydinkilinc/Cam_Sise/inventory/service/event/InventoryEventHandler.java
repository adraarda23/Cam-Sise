package ardaaydinkilinc.Cam_Sise.inventory.service.event;

import ardaaydinkilinc.Cam_Sise.analytics.anomaly.AnomalyDetectionService;
import ardaaydinkilinc.Cam_Sise.analytics.anomaly.AnomalyResult;
import ardaaydinkilinc.Cam_Sise.analytics.anomaly.StockAnomalyDetected;
import ardaaydinkilinc.Cam_Sise.core.domain.event.FillerRegistered;
import ardaaydinkilinc.Cam_Sise.inventory.service.FillerStockService;
import ardaaydinkilinc.Cam_Sise.inventory.domain.event.AssetCollected;
import ardaaydinkilinc.Cam_Sise.inventory.domain.event.AssetInflowRecorded;
import ardaaydinkilinc.Cam_Sise.inventory.domain.event.StockThresholdExceeded;
import ardaaydinkilinc.Cam_Sise.logistics.domain.event.CollectionRequestCompleted;
import ardaaydinkilinc.Cam_Sise.shared.domain.event.DomainEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Event handler for Inventory module domain events.
 * Handles side effects like auto-creating collection requests when threshold is exceeded.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryEventHandler {

    private final FillerStockService fillerStockService;
    private final ardaaydinkilinc.Cam_Sise.logistics.service.CollectionRequestService collectionRequestService;
    private final AnomalyDetectionService anomalyDetectionService;
    private final DomainEventPublisher domainEventPublisher;

    /**
     * Initialize stock when a new filler is registered
     */
    @EventListener
    @Async
    public void handleFillerRegistered(FillerRegistered event) {
        log.info("📦 Initializing stock for new filler: fillerId={}", event.fillerId());

        try {
            fillerStockService.initializeStockForFiller(event.fillerId());
            log.info("✅ Stock initialized for filler: fillerId={}", event.fillerId());
        } catch (Exception e) {
            log.error("❌ Failed to initialize stock for filler: fillerId={}", event.fillerId(), e);
        }
    }

    /**
     * Handle StockThresholdExceeded event
     * This is the KEY event that triggers automatic collection request creation
     */
    @EventListener
    @Async
    public void handleStockThresholdExceeded(StockThresholdExceeded event) {
        log.warn("⚠️ Stock threshold exceeded: fillerId={}, assetType={}, current={}, threshold={}",
                event.fillerId(),
                event.assetType(),
                event.currentQuantity(),
                event.thresholdQuantity());

        try {
            // Auto-create CollectionRequest when threshold is exceeded
            collectionRequestService.createAutomatic(
                    event.fillerId(),
                    event.assetType(),
                    event.currentQuantity()
            );
            log.info("✅ Automatic collection request created for fillerId={}, assetType={}",
                    event.fillerId(), event.assetType());
        } catch (Exception e) {
            log.error("❌ Failed to create automatic collection request: fillerId={}, assetType={}",
                    event.fillerId(), event.assetType(), e);
        }

        // TODO: Send notification to pool operator
        // TODO: Send notification to filler
    }

    /**
     * Handle AssetInflowRecorded event
     */
    @EventListener
    @Async
    public void handleAssetInflowRecorded(AssetInflowRecorded event) {
        log.info("📥 Asset inflow recorded: fillerId={}, assetType={}, quantity={}, newTotal={}",
                event.fillerId(),
                event.assetType(),
                event.quantity(),
                event.newTotalQuantity());

        try {
            AnomalyResult result = anomalyDetectionService.checkInflow(
                    event.fillerId(),
                    event.assetType(),
                    event.quantity(),
                    event.occurredAt());
            if (result.isAnomaly()) {
                log.warn("⚠️ Reactive anomaly: fillerId={}, asset={}, severity={}, z={}",
                        result.fillerId(), result.assetType(), result.severity(), result.zScore());
                domainEventPublisher.publish(StockAnomalyDetected.from(result));
            }
        } catch (Exception e) {
            log.error("Reactive anomaly check failed for fillerId={}: {}",
                    event.fillerId(), e.getMessage(), e);
        }
    }

    /**
     * Handle AssetCollected event
     */
    @EventListener
    @Async
    public void handleAssetCollected(AssetCollected event) {
        log.info("📤 Asset collected: fillerId={}, assetType={}, quantity={}, remaining={}",
                event.fillerId(),
                event.assetType(),
                event.quantity(),
                event.remainingQuantity());

        // TODO: Update collection plan status
        // TODO: Notify filler of successful collection
        // TODO: Update analytics/dashboard
    }

    /**
     * Toplama tamamlanınca ilgili dolumcunun stoğunu düşer.
     * Bir CollectionPlan tamamlandığında plana bağlı her CollectionRequest COMPLETED olur ve
     * {@link CollectionRequestCompleted} olayını yayımlar; bu olay için ilgili dolumcunun
     * stoğundan toplanan miktar düşülür (kapanış adımı). Böylece "toplama tamamlandı →
     * stok düşümü" zinciri tamamlanmış olur.
     */
    @EventListener
    @Async
    public void handleCollectionRequestCompleted(CollectionRequestCompleted event) {
        if (event.quantity() <= 0) {
            return;
        }
        log.info("📤 Toplama tamamlandı, stok düşülüyor: requestId={}, fillerId={}, assetType={}, miktar={}",
                event.requestId(), event.fillerId(), event.assetType(), event.quantity());
        try {
            fillerStockService.recordCollection(
                    event.fillerId(),
                    event.assetType(),
                    event.quantity(),
                    "request#" + event.requestId());
        } catch (Exception e) {
            log.error("❌ Toplama sonrası stok düşülemedi: requestId={}, fillerId={}",
                    event.requestId(), event.fillerId(), e);
        }
    }
}
