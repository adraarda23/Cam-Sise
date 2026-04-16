package ardaaydinkilinc.Cam_Sise.inventory.service;

import ardaaydinkilinc.Cam_Sise.inventory.domain.FillerStock;
import ardaaydinkilinc.Cam_Sise.inventory.domain.vo.AssetType;
import ardaaydinkilinc.Cam_Sise.inventory.domain.vo.LossRate;
import ardaaydinkilinc.Cam_Sise.inventory.repository.FillerStockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Application service for FillerStock aggregate.
 * Manages filler inventory and stock movements.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class FillerStockService {

    private final FillerStockRepository fillerStockRepository;

    /**
     * Initialize stock for a filler (called when filler is registered).
     * Creates stock records for both PALLET and SEPARATOR.
     */
    public void initializeStockForFiller(Long fillerId) {
        log.info("Initializing stock for filler: fillerId={}", fillerId);

        // Create stock for pallets
        FillerStock palletStock = FillerStock.initialize(
                fillerId,
                AssetType.PALLET,
                100, // Default threshold
                new LossRate(5.0) // 5% default loss rate
        );
        fillerStockRepository.save(palletStock);

        // Create stock for separators
        FillerStock separatorStock = FillerStock.initialize(
                fillerId,
                AssetType.SEPARATOR,
                50, // Default threshold
                new LossRate(3.0) // 3% default loss rate
        );
        fillerStockRepository.save(separatorStock);

        log.info("Stock initialized for filler: fillerId={}", fillerId);
    }

    /**
     * Record asset inflow (when filler receives assets from glass manufacturer).
     */
    public FillerStock recordInflow(
            Long fillerId,
            AssetType assetType,
            int quantity,
            String referenceId
    ) {
        log.info("Recording inflow: fillerId={}, assetType={}, quantity={}",
                fillerId, assetType, quantity);

        FillerStock stock = fillerStockRepository.findByFillerIdAndAssetType(fillerId, assetType)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Stock not found for filler: " + fillerId + ", asset: " + assetType));

        stock.recordInflow(quantity, referenceId);
        stock = fillerStockRepository.save(stock);

        log.info("Inflow recorded successfully: stockId={}, newQuantity={}",
                stock.getId(), stock.getCurrentQuantity());

        return stock;
    }

    /**
     * Record asset collection (when pool operator collects assets).
     */
    public FillerStock recordCollection(
            Long fillerId,
            AssetType assetType,
            int quantity,
            String collectionPlanId
    ) {
        log.info("Recording collection: fillerId={}, assetType={}, quantity={}",
                fillerId, assetType, quantity);

        FillerStock stock = fillerStockRepository.findByFillerIdAndAssetType(fillerId, assetType)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Stock not found for filler: " + fillerId + ", asset: " + assetType));

        stock.recordCollection(quantity, collectionPlanId);
        stock = fillerStockRepository.save(stock);

        log.info("Collection recorded successfully: stockId={}, newQuantity={}",
                stock.getId(), stock.getCurrentQuantity());

        return stock;
    }

    /**
     * Update threshold for a stock.
     */
    public FillerStock updateThreshold(Long fillerId, AssetType assetType, int newThreshold) {
        log.info("Updating threshold: fillerId={}, assetType={}, newThreshold={}",
                fillerId, assetType, newThreshold);

        FillerStock stock = fillerStockRepository.findByFillerIdAndAssetType(fillerId, assetType)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Stock not found for filler: " + fillerId + ", asset: " + assetType));

        stock.updateThreshold(newThreshold);
        stock = fillerStockRepository.save(stock);

        log.info("Threshold updated successfully: stockId={}", stock.getId());

        return stock;
    }

    /**
     * Update loss rate for a stock.
     */
    public FillerStock updateLossRate(Long fillerId, AssetType assetType, double lossRatePercentage) {
        log.info("Updating loss rate: fillerId={}, assetType={}, lossRate={}",
                fillerId, assetType, lossRatePercentage);

        FillerStock stock = fillerStockRepository.findByFillerIdAndAssetType(fillerId, assetType)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Stock not found for filler: " + fillerId + ", asset: " + assetType));

        stock.updateEstimatedLossRate(new LossRate(lossRatePercentage));
        stock = fillerStockRepository.save(stock);

        log.info("Loss rate updated successfully: stockId={}", stock.getId());

        return stock;
    }

    /**
     * Get stock for a filler and asset type.
     */
    @Transactional(readOnly = true)
    public FillerStock getStock(Long fillerId, AssetType assetType) {
        return fillerStockRepository.findByFillerIdAndAssetType(fillerId, assetType)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Stock not found for filler: " + fillerId + ", asset: " + assetType));
    }

    /**
     * Get all stocks for a filler.
     */
    @Transactional(readOnly = true)
    public List<FillerStock> getStocksByFiller(Long fillerId) {
        return fillerStockRepository.findByFillerId(fillerId);
    }

    /**
     * Get all stocks for an asset type.
     */
    @Transactional(readOnly = true)
    public List<FillerStock> getStocksByAssetType(AssetType assetType) {
        return fillerStockRepository.findByAssetType(assetType);
    }
}
