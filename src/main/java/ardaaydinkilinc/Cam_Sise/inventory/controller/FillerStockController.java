package ardaaydinkilinc.Cam_Sise.inventory.controller;

import ardaaydinkilinc.Cam_Sise.inventory.service.FillerStockService;
import ardaaydinkilinc.Cam_Sise.inventory.domain.FillerStock;
import ardaaydinkilinc.Cam_Sise.inventory.domain.vo.AssetType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API for FillerStock management.
 * Allows recording inflows, collections, and viewing stock levels.
 */
@RestController
@RequestMapping("/api/inventory/stocks")
@RequiredArgsConstructor
@Tag(name = "Inventory - Stock Management", description = "Dolumcu stok yönetimi API'leri")
@SecurityRequirement(name = "bearerAuth")
public class FillerStockController {

    private final FillerStockService fillerStockService;

    @Operation(
            summary = "Asset girişi kaydet",
            description = "Dolumcuya gelen palet/separatör girişini kaydet (cam üreticisinden)"
    )
    @ApiResponse(responseCode = "200", description = "Giriş başarıyla kaydedildi")
    @PostMapping("/inflow")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_STAFF', 'CUSTOMER')")
    public ResponseEntity<FillerStock> recordInflow(@RequestBody RecordInflowRequest request) {
        FillerStock stock = fillerStockService.recordInflow(
                request.fillerId,
                request.assetType,
                request.quantity,
                request.referenceId
        );
        return ResponseEntity.ok(stock);
    }

    /**
     * Record asset collection (when pool operator collects assets)
     */
    @PostMapping("/collection")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_STAFF')")
    public ResponseEntity<FillerStock> recordCollection(@RequestBody RecordCollectionRequest request) {
        FillerStock stock = fillerStockService.recordCollection(
                request.fillerId,
                request.assetType,
                request.quantity,
                request.collectionPlanId
        );
        return ResponseEntity.ok(stock);
    }

    /**
     * Update threshold for a stock
     */
    @PutMapping("/{fillerId}/{assetType}/threshold")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_STAFF')")
    public ResponseEntity<FillerStock> updateThreshold(
            @PathVariable Long fillerId,
            @PathVariable AssetType assetType,
            @RequestBody UpdateThresholdRequest request
    ) {
        FillerStock stock = fillerStockService.updateThreshold(fillerId, assetType, request.newThreshold);
        return ResponseEntity.ok(stock);
    }

    /**
     * Update loss rate for a stock
     */
    @PutMapping("/{fillerId}/{assetType}/loss-rate")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_STAFF')")
    public ResponseEntity<FillerStock> updateLossRate(
            @PathVariable Long fillerId,
            @PathVariable AssetType assetType,
            @RequestBody UpdateLossRateRequest request
    ) {
        FillerStock stock = fillerStockService.updateLossRate(fillerId, assetType, request.lossRatePercentage);
        return ResponseEntity.ok(stock);
    }

    /**
     * Get stock for a specific filler and asset type
     */
    @GetMapping("/{fillerId}/{assetType}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_STAFF', 'CUSTOMER')")
    public ResponseEntity<FillerStock> getStock(
            @PathVariable Long fillerId,
            @PathVariable AssetType assetType
    ) {
        FillerStock stock = fillerStockService.getStock(fillerId, assetType);
        return ResponseEntity.ok(stock);
    }

    /**
     * Get all stocks for a filler
     */
    @GetMapping("/filler/{fillerId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_STAFF', 'CUSTOMER')")
    public ResponseEntity<List<FillerStock>> getStocksByFiller(@PathVariable Long fillerId) {
        List<FillerStock> stocks = fillerStockService.getStocksByFiller(fillerId);
        return ResponseEntity.ok(stocks);
    }

    /**
     * Get all stocks for a specific asset type
     */
    @GetMapping("/asset-type/{assetType}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_STAFF')")
    public ResponseEntity<List<FillerStock>> getStocksByAssetType(@PathVariable AssetType assetType) {
        List<FillerStock> stocks = fillerStockService.getStocksByAssetType(assetType);
        return ResponseEntity.ok(stocks);
    }

    /**
     * Get all stocks in the system
     */
    @Operation(
            summary = "Tüm stokları listele",
            description = "Sistemdeki tüm dolumcu stok kayıtlarını listeler. ADMIN ve COMPANY_STAFF tarafından kullanılabilir."
    )
    @ApiResponse(responseCode = "200", description = "Stok listesi başarıyla döndürüldü")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_STAFF')")
    public ResponseEntity<List<FillerStock>> getAllStocks() {
        List<FillerStock> stocks = fillerStockService.getAllStocks();
        return ResponseEntity.ok(stocks);
    }

    // ===== DTOs =====

    public record RecordInflowRequest(
            Long fillerId,
            AssetType assetType,
            int quantity,
            String referenceId
    ) {}

    public record RecordCollectionRequest(
            Long fillerId,
            AssetType assetType,
            int quantity,
            String collectionPlanId
    ) {}

    public record UpdateThresholdRequest(
            int newThreshold
    ) {}

    public record UpdateLossRateRequest(
            double lossRatePercentage
    ) {}
}
