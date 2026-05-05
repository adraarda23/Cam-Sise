package ardaaydinkilinc.Cam_Sise.inventory.controller;

import ardaaydinkilinc.Cam_Sise.inventory.service.LossRecordService;
import ardaaydinkilinc.Cam_Sise.inventory.domain.LossRecord;
import ardaaydinkilinc.Cam_Sise.inventory.domain.vo.AssetType;
import ardaaydinkilinc.Cam_Sise.inventory.domain.vo.Period;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * REST API for LossRecord management.
 * Allows tracking and managing loss rates for assets at filler locations.
 */
@RestController
@RequestMapping("/api/inventory/loss-records")
@RequiredArgsConstructor
@Tag(name = "Inventory - Loss Records", description = "Zaiyat kayıtları yönetimi API'leri")
@SecurityRequirement(name = "bearerAuth")
public class LossRecordController {

    private final LossRecordService lossRecordService;

    @Operation(summary = "Zaiyat kaydı oluştur", description = "Dolumcu ve asset tipi için tahmini zaiyat oranıyla yeni kayıt oluşturur. Sadece COMPANY_STAFF.")
    @ApiResponse(responseCode = "201", description = "Zaiyat kaydı oluşturuldu")
    @ApiResponse(responseCode = "400", description = "Geçersiz parametreler")
    @PostMapping
    @PreAuthorize("hasRole('COMPANY_STAFF')")
    public ResponseEntity<LossRecord> createLossRecord(@RequestBody CreateLossRecordRequest request) {
        Period period = new Period(request.periodStartDate, request.periodEndDate);
        LossRecord record = lossRecordService.createWithEstimate(
                request.fillerId,
                request.assetType,
                request.estimatedRatePercentage,
                period
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(record);
    }

    @Operation(summary = "Gerçekleşen zaiyat oranını güncelle", description = "Dolumcunun raporladığı gerçek zaiyat oranını kaydeder.")
    @ApiResponse(responseCode = "200", description = "Zaiyat oranı güncellendi")
    @ApiResponse(responseCode = "404", description = "Zaiyat kaydı bulunamadı")
    @PutMapping("/{fillerId}/{assetType}/actual-rate")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_STAFF', 'CUSTOMER')")
    public ResponseEntity<LossRecord> updateActualRate(
            @Parameter(description = "Dolumcu ID") @PathVariable Long fillerId,
            @Parameter(description = "Asset tipi (PALLET veya SEPARATOR)") @PathVariable AssetType assetType,
            @RequestBody UpdateActualRateRequest request
    ) {
        LossRecord record = lossRecordService.updateActualRate(
                fillerId,
                assetType,
                request.actualRatePercentage
        );
        return ResponseEntity.ok(record);
    }

    @Operation(summary = "Tahmini zaiyat oranını yeniden hesapla", description = "Moving average algoritmasıyla tahmini zaiyat oranını günceller. Sadece COMPANY_STAFF.")
    @ApiResponse(responseCode = "200", description = "Tahmini oran güncellendi")
    @ApiResponse(responseCode = "404", description = "Zaiyat kaydı bulunamadı")
    @PutMapping("/{fillerId}/{assetType}/estimated-rate")
    @PreAuthorize("hasRole('COMPANY_STAFF')")
    public ResponseEntity<LossRecord> recalculateEstimatedRate(
            @Parameter(description = "Dolumcu ID") @PathVariable Long fillerId,
            @Parameter(description = "Asset tipi (PALLET veya SEPARATOR)") @PathVariable AssetType assetType,
            @RequestBody RecalculateEstimatedRateRequest request
    ) {
        Period period = new Period(request.periodStartDate, request.periodEndDate);
        LossRecord record = lossRecordService.recalculateEstimatedRate(
                fillerId,
                assetType,
                request.newEstimatedRatePercentage,
                period
        );
        return ResponseEntity.ok(record);
    }

    @Operation(summary = "Zaiyat kaydını getir", description = "Belirli dolumcu ve asset tipi için zaiyat kaydını döndürür.")
    @ApiResponse(responseCode = "200", description = "Zaiyat kaydı bulundu")
    @ApiResponse(responseCode = "404", description = "Kayıt bulunamadı")
    @GetMapping("/{fillerId}/{assetType}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_STAFF', 'CUSTOMER')")
    public ResponseEntity<LossRecord> getLossRecord(
            @Parameter(description = "Dolumcu ID") @PathVariable Long fillerId,
            @Parameter(description = "Asset tipi (PALLET veya SEPARATOR)") @PathVariable AssetType assetType
    ) {
        LossRecord record = lossRecordService.getLossRecord(fillerId, assetType);
        return ResponseEntity.ok(record);
    }

    @Operation(summary = "Dolumcuya ait tüm zaiyat kayıtları", description = "Bir dolumcunun tüm asset tiplerine ait zaiyat kayıtlarını listeler.")
    @ApiResponse(responseCode = "200", description = "Zaiyat kayıtları listelendi")
    @GetMapping("/filler/{fillerId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_STAFF', 'CUSTOMER')")
    public ResponseEntity<List<LossRecord>> getLossRecordsByFiller(
            @Parameter(description = "Dolumcu ID") @PathVariable Long fillerId) {
        List<LossRecord> records = lossRecordService.getLossRecordsByFiller(fillerId);
        return ResponseEntity.ok(records);
    }

    @Operation(summary = "Asset tipine göre zaiyat kayıtları", description = "Belirli asset tipindeki tüm dolumcuların zaiyat kayıtlarını listeler. Sadece COMPANY_STAFF.")
    @ApiResponse(responseCode = "200", description = "Zaiyat kayıtları listelendi")
    @GetMapping("/asset-type/{assetType}")
    @PreAuthorize("hasRole('COMPANY_STAFF')")
    public ResponseEntity<List<LossRecord>> getLossRecordsByAssetType(
            @Parameter(description = "Asset tipi (PALLET veya SEPARATOR)") @PathVariable AssetType assetType) {
        List<LossRecord> records = lossRecordService.getLossRecordsByAssetType(assetType);
        return ResponseEntity.ok(records);
    }

    // ===== DTOs =====

    public record CreateLossRecordRequest(
            Long fillerId,
            AssetType assetType,
            double estimatedRatePercentage,
            LocalDate periodStartDate,
            LocalDate periodEndDate
    ) {}

    public record UpdateActualRateRequest(
            double actualRatePercentage
    ) {}

    public record RecalculateEstimatedRateRequest(
            double newEstimatedRatePercentage,
            LocalDate periodStartDate,
            LocalDate periodEndDate
    ) {}
}
