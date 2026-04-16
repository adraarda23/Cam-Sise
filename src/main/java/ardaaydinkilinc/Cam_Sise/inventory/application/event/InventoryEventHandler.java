package ardaaydinkilinc.Cam_Sise.inventory.application.event;

import ardaaydinkilinc.Cam_Sise.core.domain.event.FillerRegistered;
import ardaaydinkilinc.Cam_Sise.inventory.application.service.FillerStockService;
import ardaaydinkilinc.Cam_Sise.inventory.domain.event.AssetCollected;
import ardaaydinkilinc.Cam_Sise.inventory.domain.event.AssetInflowRecorded;
import ardaaydinkilinc.Cam_Sise.inventory.domain.event.StockThresholdExceeded;
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
    private final ardaaydinkilinc.Cam_Sise.logistics.application.service.CollectionRequestService collectionRequestService;

    /**
     * Initialize stock when a new filler is registered
     */
    @EventListener
    @Async
    public void handleFillerRegistered(FillerRegistered event) {
        log.info("📦 Initializing stock for new filler: fillerId={}", event.poolOperatorId());

        try {
            // Initialize stock records for the new filler
            fillerStockService.initializeStockForFiller(event.poolOperatorId());
            log.info("✅ Stock initialized for filler: fillerId={}", event.poolOperatorId());
        } catch (Exception e) {
            log.error("❌ Failed to initialize stock for filler: fillerId={}", event.poolOperatorId(), e);
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

        // TODO: Update analytics/dashboard
        // TODO: Notify filler of successful inflow recording
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
}
