package ardaaydinkilinc.Cam_Sise.inventory.service;

import ardaaydinkilinc.Cam_Sise.core.domain.Filler;
import ardaaydinkilinc.Cam_Sise.core.repository.FillerRepository;
import ardaaydinkilinc.Cam_Sise.inventory.domain.FillerStock;
import ardaaydinkilinc.Cam_Sise.inventory.domain.StockMovementHistory;
import ardaaydinkilinc.Cam_Sise.inventory.domain.vo.AssetType;
import ardaaydinkilinc.Cam_Sise.inventory.domain.vo.LossRate;
import ardaaydinkilinc.Cam_Sise.inventory.repository.FillerStockRepository;
import ardaaydinkilinc.Cam_Sise.inventory.repository.StockMovementHistoryRepository;
import ardaaydinkilinc.Cam_Sise.shared.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
    private final StockMovementHistoryRepository movementHistoryRepository;
    private final FillerRepository fillerRepository;

    /**
     * Initialize stock for a filler (called when filler is registered).
     * Creates stock records for both PALLET and SEPARATOR. Idempotent —
     * skips assets that already have a stock record.
     */
    public void initializeStockForFiller(Long fillerId) {
        log.info("Initializing stock for filler: fillerId={}", fillerId);

        if (fillerStockRepository.findByFillerIdAndAssetType(fillerId, AssetType.PALLET).isEmpty()) {
            FillerStock palletStock = FillerStock.initialize(
                    fillerId,
                    AssetType.PALLET,
                    100,
                    new LossRate(5.0)
            );
            fillerStockRepository.save(palletStock);
        }

        if (fillerStockRepository.findByFillerIdAndAssetType(fillerId, AssetType.SEPARATOR).isEmpty()) {
            FillerStock separatorStock = FillerStock.initialize(
                    fillerId,
                    AssetType.SEPARATOR,
                    50,
                    new LossRate(3.0)
            );
            fillerStockRepository.save(separatorStock);
        }

        log.info("Stock initialized for filler: fillerId={}", fillerId);
    }

    /**
     * Backfill: stok kaydı olmayan dolumcular için varsayılan stokları oluşturur.
     * FillerRegistered event'inin daha önce hatalı id ile çalıştığı veya event'in hiç
     * yayınlanmadığı durumlarda eski dolumcuların stok listesinde görünmesini sağlar.
     */
    public void ensureStocksForOperator(Long poolOperatorId) {
        List<Filler> fillers = fillerRepository.findByPoolOperatorId(poolOperatorId);
        for (Filler filler : fillers) {
            boolean palletMissing = fillerStockRepository
                    .findByFillerIdAndAssetType(filler.getId(), AssetType.PALLET).isEmpty();
            boolean separatorMissing = fillerStockRepository
                    .findByFillerIdAndAssetType(filler.getId(), AssetType.SEPARATOR).isEmpty();
            if (palletMissing || separatorMissing) {
                log.info("Backfilling stocks for filler: id={}, name={}", filler.getId(), filler.getName());
                initializeStockForFiller(filler.getId());
            }
        }
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

        movementHistoryRepository.save(StockMovementHistory.inflow(
                fillerId,
                assetType,
                quantity,
                stock.getCurrentQuantity(),
                referenceId,
                LocalDateTime.now()
        ));

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

        movementHistoryRepository.save(StockMovementHistory.collection(
                fillerId,
                assetType,
                quantity,
                stock.getCurrentQuantity(),
                collectionPlanId,
                LocalDateTime.now()
        ));

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

    /**
     * Get all stocks in the system.
     */
    @Transactional(readOnly = true)
    public List<FillerStock> getAllStocks() {
        return fillerStockRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<FillerStock> getAllStocksByPoolOperatorId(Long poolOperatorId) {
        return fillerStockRepository.findByPoolOperatorId(poolOperatorId);
    }

    public PageResponse<FillerStock> findByPoolOperatorIdPaged(Long poolOperatorId, String search, int page, int size) {
        // Lazy backfill: stoğu olmayan dolumcuları (örn. event'in fillerId hatasından
        // önce kayıt olanlar) listede görünür hale getir.
        ensureStocksForOperator(poolOperatorId);

        String searchParam = (search == null || search.isBlank()) ? "" : search;
        var pageable = PageRequest.of(page, size, Sort.by("id").descending());
        return PageResponse.from(fillerStockRepository.findByPoolOperatorIdFiltered(poolOperatorId, searchParam, pageable));
    }
}
