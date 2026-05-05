package ardaaydinkilinc.Cam_Sise.inventory.controller;

import ardaaydinkilinc.Cam_Sise.inventory.service.FillerStockService;
import ardaaydinkilinc.Cam_Sise.inventory.domain.FillerStock;
import ardaaydinkilinc.Cam_Sise.inventory.domain.vo.AssetType;
import ardaaydinkilinc.Cam_Sise.shared.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
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
    private final JwtUtil jwtUtil;

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

    @Operation(summary = "Asset toplama kaydet", description = "Havuz operatörünün dolumcudan topladığı asset miktarını stoka yansıtır.")
    @ApiResponse(responseCode = "200", description = "Toplama başarıyla kaydedildi")
    @PostMapping("/collection")
    @PreAuthorize("hasRole('COMPANY_STAFF')")
    public ResponseEntity<FillerStock> recordCollection(@RequestBody RecordCollectionRequest request) {
        FillerStock stock = fillerStockService.recordCollection(
                request.fillerId,
                request.assetType,
                request.quantity,
                request.collectionPlanId
        );
        return ResponseEntity.ok(stock);
    }

    @Operation(summary = "Stok eşiğini güncelle", description = "Otomatik toplama talebini tetikleyen minimum stok eşiğini ayarlar. Sadece COMPANY_STAFF.")
    @ApiResponse(responseCode = "200", description = "Eşik güncellendi")
    @ApiResponse(responseCode = "404", description = "Stok kaydı bulunamadı")
    @PutMapping("/{fillerId}/{assetType}/threshold")
    @PreAuthorize("hasRole('COMPANY_STAFF')")
    public ResponseEntity<FillerStock> updateThreshold(
            @Parameter(description = "Dolumcu ID") @PathVariable Long fillerId,
            @Parameter(description = "Asset tipi (PALLET veya SEPARATOR)") @PathVariable AssetType assetType,
            @RequestBody UpdateThresholdRequest request
    ) {
        FillerStock stock = fillerStockService.updateThreshold(fillerId, assetType, request.newThreshold);
        return ResponseEntity.ok(stock);
    }

    @Operation(summary = "Zaiyat oranını güncelle", description = "Stok kaydına ait zaiyat oranını manuel olarak günceller. Sadece COMPANY_STAFF.")
    @ApiResponse(responseCode = "200", description = "Zaiyat oranı güncellendi")
    @ApiResponse(responseCode = "404", description = "Stok kaydı bulunamadı")
    @PutMapping("/{fillerId}/{assetType}/loss-rate")
    @PreAuthorize("hasRole('COMPANY_STAFF')")
    public ResponseEntity<FillerStock> updateLossRate(
            @Parameter(description = "Dolumcu ID") @PathVariable Long fillerId,
            @Parameter(description = "Asset tipi (PALLET veya SEPARATOR)") @PathVariable AssetType assetType,
            @RequestBody UpdateLossRateRequest request
    ) {
        FillerStock stock = fillerStockService.updateLossRate(fillerId, assetType, request.lossRatePercentage);
        return ResponseEntity.ok(stock);
    }

    @Operation(summary = "Stok kaydını getir", description = "Belirli dolumcu ve asset tipi için anlık stok durumunu döndürür.")
    @ApiResponse(responseCode = "200", description = "Stok bilgisi döndürüldü")
    @ApiResponse(responseCode = "404", description = "Stok kaydı bulunamadı")
    @GetMapping("/{fillerId}/{assetType}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_STAFF', 'CUSTOMER')")
    public ResponseEntity<FillerStock> getStock(
            @Parameter(description = "Dolumcu ID") @PathVariable Long fillerId,
            @Parameter(description = "Asset tipi (PALLET veya SEPARATOR)") @PathVariable AssetType assetType
    ) {
        FillerStock stock = fillerStockService.getStock(fillerId, assetType);
        return ResponseEntity.ok(stock);
    }

    @Operation(summary = "Dolumcuya ait tüm stoklar", description = "Bir dolumcunun tüm asset tiplerine ait stok durumunu listeler.")
    @ApiResponse(responseCode = "200", description = "Stok listesi döndürüldü")
    @GetMapping("/filler/{fillerId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_STAFF', 'CUSTOMER')")
    public ResponseEntity<List<FillerStock>> getStocksByFiller(
            @Parameter(description = "Dolumcu ID") @PathVariable Long fillerId) {
        List<FillerStock> stocks = fillerStockService.getStocksByFiller(fillerId);
        return ResponseEntity.ok(stocks);
    }

    @Operation(summary = "Asset tipine göre stoklar", description = "Tenant'a ait tüm dolumcuların belirtilen asset tipindeki stok durumunu listeler. Sadece COMPANY_STAFF.")
    @ApiResponse(responseCode = "200", description = "Stok listesi döndürüldü")
    @GetMapping("/asset-type/{assetType}")
    @PreAuthorize("hasRole('COMPANY_STAFF')")
    public ResponseEntity<List<FillerStock>> getStocksByAssetType(
            @Parameter(description = "Asset tipi (PALLET veya SEPARATOR)") @PathVariable AssetType assetType,
            HttpServletRequest httpRequest) {
        Long poolOperatorId = jwtUtil.extractPoolOperatorId(httpRequest.getHeader("Authorization").substring(7));
        List<FillerStock> stocks = fillerStockService.getAllStocksByPoolOperatorId(poolOperatorId)
                .stream().filter(s -> s.getAssetType() == assetType).toList();
        return ResponseEntity.ok(stocks);
    }

    @Operation(
            summary = "Tüm stokları listele",
            description = "Tenant'a ait tüm dolumcu stok kayıtlarını sayfalı listeler."
    )
    @ApiResponse(responseCode = "200", description = "Stok listesi başarıyla döndürüldü")
    @GetMapping
    @PreAuthorize("hasRole('COMPANY_STAFF')")
    public ResponseEntity<ardaaydinkilinc.Cam_Sise.shared.dto.PageResponse<FillerStock>> getAllStocks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            HttpServletRequest httpRequest
    ) {
        Long poolOperatorId = jwtUtil.extractPoolOperatorId(httpRequest.getHeader("Authorization").substring(7));
        return ResponseEntity.ok(fillerStockService.findByPoolOperatorIdPaged(poolOperatorId, search, page, size));
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
