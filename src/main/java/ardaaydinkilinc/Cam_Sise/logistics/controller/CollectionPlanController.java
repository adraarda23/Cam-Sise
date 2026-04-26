package ardaaydinkilinc.Cam_Sise.logistics.controller;

import ardaaydinkilinc.Cam_Sise.logistics.service.CollectionPlanService;
import ardaaydinkilinc.Cam_Sise.logistics.domain.CollectionPlan;
import ardaaydinkilinc.Cam_Sise.logistics.domain.vo.PlanStatus;
import ardaaydinkilinc.Cam_Sise.shared.dto.PageResponse;
import ardaaydinkilinc.Cam_Sise.shared.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * REST API for CollectionPlan management.
 */
@RestController
@RequestMapping("/api/logistics/collection-plans")
@RequiredArgsConstructor
@Tag(name = "Logistics - Collection Plans", description = "Toplama planı yönetimi API'leri")
@SecurityRequirement(name = "bearerAuth")
public class CollectionPlanController {

    private final CollectionPlanService collectionPlanService;
    private final JwtUtil jwtUtil;

    /**
     * Generate a new collection plan (typically called by CVRP optimizer)
     */
    @Operation(
            summary = "Yeni toplama planı oluştur",
            description = "CVRP optimizasyonu sonucu oluşturulan rota bilgilerini kullanarak yeni bir toplama planı kaydeder."
    )
    @ApiResponse(responseCode = "201", description = "Toplama planı başarıyla oluşturuldu")
    @ApiResponse(responseCode = "400", description = "Geçersiz request parametreleri")
    @PostMapping
    @PreAuthorize("hasRole('COMPANY_STAFF')")
    public ResponseEntity<CollectionPlan> generatePlan(@RequestBody GeneratePlanRequest request) {
        CollectionPlan plan = collectionPlanService.generatePlan(
                request.depotId,
                request.totalDistanceKm,
                request.estimatedDurationMinutes,
                request.totalCapacityPallets,
                request.totalCapacitySeparators,
                request.plannedDate,
                request.routeStopsJson
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(plan);
    }

    /**
     * Assign vehicle to a collection plan
     */
    @Operation(
            summary = "Plana araç ata",
            description = "Oluşturulan toplama planına bir araç atar."
    )
    @ApiResponse(responseCode = "200", description = "Araç başarıyla plana atandı")
    @ApiResponse(responseCode = "404", description = "Plan veya araç bulunamadı")
    @PostMapping("/{planId}/assign-vehicle")
    @PreAuthorize("hasRole('COMPANY_STAFF')")
    public ResponseEntity<CollectionPlan> assignVehicle(
            @Parameter(description = "Toplama planı ID") @PathVariable Long planId,
            @RequestBody AssignVehicleRequest request
    ) {
        CollectionPlan plan = collectionPlanService.assignVehicle(planId, request.vehicleId);
        return ResponseEntity.ok(plan);
    }

    /**
     * Start collection (vehicle departed)
     */
    @Operation(
            summary = "Toplamayı başlat",
            description = "Toplama planını başlatır (araç depodan ayrıldı)."
    )
    @ApiResponse(responseCode = "200", description = "Toplama başarıyla başlatıldı")
    @ApiResponse(responseCode = "404", description = "Plan bulunamadı")
    @PostMapping("/{planId}/start")
    @PreAuthorize("hasRole('COMPANY_STAFF')")
    public ResponseEntity<CollectionPlan> startCollection(
            @Parameter(description = "Toplama planı ID") @PathVariable Long planId
    ) {
        CollectionPlan plan = collectionPlanService.startCollection(planId);
        return ResponseEntity.ok(plan);
    }

    /**
     * Complete collection
     */
    @Operation(
            summary = "Toplamayı tamamla",
            description = "Toplama planını tamamlar ve gerçek toplanan miktarları kaydeder."
    )
    @ApiResponse(responseCode = "200", description = "Toplama başarıyla tamamlandı")
    @ApiResponse(responseCode = "404", description = "Plan bulunamadı")
    @PostMapping("/{planId}/complete")
    @PreAuthorize("hasRole('COMPANY_STAFF')")
    public ResponseEntity<CollectionPlan> completeCollection(
            @Parameter(description = "Toplama planı ID") @PathVariable Long planId,
            @RequestBody CompleteCollectionRequest request
    ) {
        CollectionPlan plan = collectionPlanService.completeCollection(
                planId,
                request.actualPalletsCollected,
                request.actualSeparatorsCollected
        );
        return ResponseEntity.ok(plan);
    }

    /**
     * Cancel a collection plan
     */
    @Operation(
            summary = "Planı iptal et",
            description = "Bir toplama planını iptal eder."
    )
    @ApiResponse(responseCode = "200", description = "Plan başarıyla iptal edildi")
    @ApiResponse(responseCode = "404", description = "Plan bulunamadı")
    @PostMapping("/{planId}/cancel")
    @PreAuthorize("hasRole('COMPANY_STAFF')")
    public ResponseEntity<CollectionPlan> cancelPlan(
            @Parameter(description = "Toplama planı ID") @PathVariable Long planId
    ) {
        CollectionPlan plan = collectionPlanService.cancelPlan(planId);
        return ResponseEntity.ok(plan);
    }

    /**
     * Get collection plan by ID
     */
    @Operation(
            summary = "Plan bilgisi getir",
            description = "ID'ye göre toplama planı bilgisini getirir."
    )
    @ApiResponse(responseCode = "200", description = "Plan başarıyla getirildi")
    @ApiResponse(responseCode = "404", description = "Plan bulunamadı")
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('COMPANY_STAFF')")
    public ResponseEntity<CollectionPlan> getPlan(
            @Parameter(description = "Toplama planı ID", example = "1") @PathVariable Long id
    ) {
        CollectionPlan plan = collectionPlanService.findById(id);
        return ResponseEntity.ok(plan);
    }

    /**
     * Get all collection plans (with optional filters)
     */
    @Operation(
            summary = "Tüm planları listele",
            description = "Sistemdeki tüm toplama planlarını listeler. Depo, durum, araç ve tarih aralığına göre filtrelenebilir."
    )
    @ApiResponse(responseCode = "200", description = "Plan listesi başarıyla döndürüldü")
    @GetMapping
    @PreAuthorize("hasRole('COMPANY_STAFF')")
    public ResponseEntity<PageResponse<CollectionPlan>> getAllPlans(
            @Parameter(description = "Plan durumuna göre filtrele") @RequestParam(required = false) PlanStatus status,
            @Parameter(description = "Başlangıç tarihi") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "Bitiş tarihi") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest httpRequest
    ) {
        Long poolOperatorId = jwtUtil.extractPoolOperatorId(httpRequest.getHeader("Authorization").substring(7));
        return ResponseEntity.ok(collectionPlanService.findByPoolOperatorIdPaged(poolOperatorId, status, startDate, endDate, page, size));
    }

    /**
     * Get plans for a specific depot
     */
    @Operation(
            summary = "Depoya göre planları getir",
            description = "Belirli bir depoya ait tüm toplama planlarını getirir."
    )
    @ApiResponse(responseCode = "200", description = "Plan listesi başarıyla döndürüldü")
    @GetMapping("/depot/{depotId}")
    @PreAuthorize("hasRole('COMPANY_STAFF')")
    public ResponseEntity<List<CollectionPlan>> getPlansByDepot(
            @Parameter(description = "Depo ID") @PathVariable Long depotId,
            @Parameter(description = "Plan durumuna göre filtrele") @RequestParam(required = false) PlanStatus status
    ) {
        List<CollectionPlan> plans = collectionPlanService.findByDepot(depotId, status);
        return ResponseEntity.ok(plans);
    }

    /**
     * Get plans for a specific vehicle
     */
    @Operation(
            summary = "Araca göre planları getir",
            description = "Belirli bir araca ait tüm toplama planlarını getirir."
    )
    @ApiResponse(responseCode = "200", description = "Plan listesi başarıyla döndürüldü")
    @GetMapping("/vehicle/{vehicleId}")
    @PreAuthorize("hasRole('COMPANY_STAFF')")
    public ResponseEntity<List<CollectionPlan>> getPlansByVehicle(
            @Parameter(description = "Araç ID") @PathVariable Long vehicleId
    ) {
        List<CollectionPlan> plans = collectionPlanService.findByVehicle(vehicleId);
        return ResponseEntity.ok(plans);
    }

    // ===== DTOs =====

    @Schema(description = "Plan oluşturma request DTO")
    public record GeneratePlanRequest(
            @Schema(description = "Depo ID", example = "1", required = true)
            Long depotId,

            @Schema(description = "Toplam rota mesafesi (km)", example = "125.5", required = true)
            double totalDistanceKm,

            @Schema(description = "Tahmini süre (dakika)", example = "240", required = true)
            int estimatedDurationMinutes,

            @Schema(description = "Toplam palet kapasitesi", example = "120", required = true)
            int totalCapacityPallets,

            @Schema(description = "Toplam seperatör kapasitesi", example = "80", required = true)
            int totalCapacitySeparators,

            @Schema(description = "Planlanan tarih", example = "2026-04-20", required = true)
            LocalDate plannedDate,

            @Schema(description = "Rota durağı JSON verisi", example = "[{\"fillerId\":1,\"sequence\":1,...}]", required = true)
            String routeStopsJson
    ) {}

    @Schema(description = "Araç atama request DTO")
    public record AssignVehicleRequest(
            @Schema(description = "Araç ID", example = "1", required = true)
            Long vehicleId
    ) {}

    @Schema(description = "Toplama tamamlama request DTO")
    public record CompleteCollectionRequest(
            @Schema(description = "Gerçek toplanan palet sayısı", example = "115", required = true)
            int actualPalletsCollected,

            @Schema(description = "Gerçek toplanan seperatör sayısı", example = "78", required = true)
            int actualSeparatorsCollected
    ) {}
}
