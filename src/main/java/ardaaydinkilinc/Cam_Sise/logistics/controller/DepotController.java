package ardaaydinkilinc.Cam_Sise.logistics.controller;

import ardaaydinkilinc.Cam_Sise.logistics.service.DepotService;
import ardaaydinkilinc.Cam_Sise.logistics.domain.Depot;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API for Depot management.
 */
@RestController
@RequestMapping("/api/logistics/depots")
@RequiredArgsConstructor
@Tag(name = "Logistics - Depots", description = "Depo yönetimi API'leri")
@SecurityRequirement(name = "bearerAuth")
public class DepotController {

    private final DepotService depotService;

    /**
     * Create a new depot
     */
    @Operation(
            summary = "Yeni depo oluştur",
            description = "Sisteme yeni bir depo kaydeder. ADMIN ve COMPANY_STAFF tarafından kullanılabilir."
    )
    @ApiResponse(responseCode = "201", description = "Depo başarıyla oluşturuldu")
    @ApiResponse(responseCode = "400", description = "Geçersiz request parametreleri")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_STAFF')")
    public ResponseEntity<Depot> createDepot(@RequestBody CreateDepotRequest request) {
        Depot depot = depotService.createDepot(
                request.poolOperatorId,
                request.name,
                request.street,
                request.city,
                request.province,
                request.postalCode,
                request.country,
                request.latitude,
                request.longitude
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(depot);
    }

    /**
     * Add vehicle to depot
     */
    @Operation(
            summary = "Depoya araç ekle",
            description = "Mevcut bir aracı depoya atar."
    )
    @ApiResponse(responseCode = "200", description = "Araç başarıyla depoya eklendi")
    @ApiResponse(responseCode = "404", description = "Depo veya araç bulunamadı")
    @PostMapping("/{depotId}/vehicles/{vehicleId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_STAFF')")
    public ResponseEntity<Depot> addVehicle(
            @Parameter(description = "Depo ID") @PathVariable Long depotId,
            @Parameter(description = "Araç ID") @PathVariable Long vehicleId
    ) {
        Depot depot = depotService.addVehicle(depotId, vehicleId);
        return ResponseEntity.ok(depot);
    }

    /**
     * Remove vehicle from depot
     */
    @Operation(
            summary = "Depodan araç çıkar",
            description = "Depoya atanmış bir aracı depodan çıkarır."
    )
    @ApiResponse(responseCode = "200", description = "Araç başarıyla depodan çıkarıldı")
    @ApiResponse(responseCode = "404", description = "Depo veya araç bulunamadı")
    @DeleteMapping("/{depotId}/vehicles/{vehicleId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_STAFF')")
    public ResponseEntity<Depot> removeVehicle(
            @Parameter(description = "Depo ID") @PathVariable Long depotId,
            @Parameter(description = "Araç ID") @PathVariable Long vehicleId
    ) {
        Depot depot = depotService.removeVehicle(depotId, vehicleId);
        return ResponseEntity.ok(depot);
    }

    /**
     * Get depot by ID
     */
    @Operation(
            summary = "Depo bilgisi getir",
            description = "ID'ye göre depo bilgisini getirir."
    )
    @ApiResponse(responseCode = "200", description = "Depo başarıyla getirildi")
    @ApiResponse(responseCode = "404", description = "Depo bulunamadı")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_STAFF')")
    public ResponseEntity<Depot> getDepot(
            @Parameter(description = "Depo ID", example = "1") @PathVariable Long id
    ) {
        Depot depot = depotService.findById(id);
        return ResponseEntity.ok(depot);
    }

    /**
     * Get all depots (with optional filters)
     */
    @Operation(
            summary = "Tüm depoları listele",
            description = "Sistemdeki tüm depoları listeler. Pool operator ve aktiflik durumuna göre filtrelenebilir."
    )
    @ApiResponse(responseCode = "200", description = "Depo listesi başarıyla döndürüldü")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_STAFF')")
    public ResponseEntity<List<Depot>> getAllDepots(
            @Parameter(description = "Havuz operatörü ID'ye göre filtrele") @RequestParam(required = false) Long poolOperatorId,
            @Parameter(description = "Aktiflik durumuna göre filtrele") @RequestParam(required = false) Boolean active
    ) {
        List<Depot> depots;
        if (poolOperatorId != null) {
            depots = depotService.findByPoolOperator(poolOperatorId, active);
        } else if (active != null && active) {
            depots = depotService.findAllActive();
        } else {
            depots = depotService.findAll();
        }
        return ResponseEntity.ok(depots);
    }

    // ===== DTOs =====

    @Schema(description = "Depo oluşturma request DTO")
    public record CreateDepotRequest(
            @Schema(description = "Havuz operatörü ID", example = "1", required = true)
            Long poolOperatorId,

            @Schema(description = "Depo adı", example = "İstanbul Merkez Depo", required = true)
            String name,

            @Schema(description = "Sokak adresi", example = "Atatürk Cad. No:123", required = true)
            String street,

            @Schema(description = "İl/Şehir", example = "İstanbul", required = true)
            String city,

            @Schema(description = "İlçe", example = "Kadıköy", required = true)
            String province,

            @Schema(description = "Posta kodu", example = "34710", required = true)
            String postalCode,

            @Schema(description = "Ülke", example = "Türkiye", required = true)
            String country,

            @Schema(description = "Enlem (Latitude)", example = "41.0082", required = true)
            @jakarta.validation.constraints.NotNull(message = "Enlem (latitude) zorunludur")
            @jakarta.validation.constraints.DecimalMin(value = "-90.0", message = "Enlem (latitude) -90 ile 90 arasında olmalıdır")
            @jakarta.validation.constraints.DecimalMax(value = "90.0", message = "Enlem (latitude) -90 ile 90 arasında olmalıdır")
            Double latitude,

            @Schema(description = "Boylam (Longitude)", example = "28.9784", required = true)
            @jakarta.validation.constraints.NotNull(message = "Boylam (longitude) zorunludur")
            @jakarta.validation.constraints.DecimalMin(value = "-180.0", message = "Boylam (longitude) -180 ile 180 arasında olmalıdır")
            @jakarta.validation.constraints.DecimalMax(value = "180.0", message = "Boylam (longitude) -180 ile 180 arasında olmalıdır")
            Double longitude
    ) {}
}
