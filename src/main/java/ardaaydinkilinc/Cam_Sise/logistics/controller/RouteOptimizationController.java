package ardaaydinkilinc.Cam_Sise.logistics.controller;

import ardaaydinkilinc.Cam_Sise.logistics.domain.CollectionPlan;
import ardaaydinkilinc.Cam_Sise.logistics.domain.CollectionRequest;
import ardaaydinkilinc.Cam_Sise.logistics.domain.vo.Capacity;
import ardaaydinkilinc.Cam_Sise.logistics.domain.vo.RequestStatus;
import ardaaydinkilinc.Cam_Sise.logistics.repository.CollectionRequestRepository;
import ardaaydinkilinc.Cam_Sise.logistics.service.RouteOptimizationService;
import ardaaydinkilinc.Cam_Sise.logistics.service.fleet.FleetComposition;
import ardaaydinkilinc.Cam_Sise.logistics.service.fleet.VehicleAssignmentService;
import ardaaydinkilinc.Cam_Sise.shared.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import io.swagger.v3.oas.annotations.Operation;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * REST API for route optimization.
 * Triggers CVRP algorithm to generate optimized collection plans.
 */
@RestController
@RequestMapping("/api/logistics/optimize")
@RequiredArgsConstructor
@Tag(name = "Logistics - Route Optimization", description = "CVRP tabanlı rota optimizasyonu API'leri")
@SecurityRequirement(name = "bearerAuth")
public class RouteOptimizationController {

    private final RouteOptimizationService routeOptimizationService;
    private final VehicleAssignmentService vehicleAssignmentService;
    private final CollectionRequestRepository collectionRequestRepository;
    private final JwtUtil jwtUtil;

    /**
     * Generate optimized collection plan(s) for all approved requests.
     * Automatically uses multiple vehicles when total demand exceeds single-vehicle capacity.
     */
    @Operation(
            summary = "Onaylı talepler için otomatik optimize rota oluştur",
            description = "Tüm onaylanmış collection requestleri için CVRP algoritması ile optimize edilmiş toplama planı oluşturur. " +
                    "Toplam talep tek araç kapasitesini aşıyorsa otomatik olarak çoklu araç kullanılır."
    )
    @ApiResponse(responseCode = "201", description = "Rota planı/planları başarıyla oluşturuldu")
    @ApiResponse(responseCode = "400", description = "Onaylı talep bulunamadı veya optimizasyon başarısız")
    @PostMapping
    @PreAuthorize("hasRole('COMPANY_STAFF')")
    public ResponseEntity<MultiVehicleOptimizeResponse> generateOptimizedPlan(
            @RequestBody OptimizeRouteRequest request
    ) {
        List<CollectionPlan> plans = routeOptimizationService.generateOptimizedPlan(
                request.depotId,
                request.plannedDate != null ? request.plannedDate : LocalDate.now().plusDays(1)
        );

        double totalDistance = plans.stream()
                .mapToDouble(p -> p.getTotalDistance().kilometers())
                .sum();
        int totalPallets = plans.stream().mapToInt(CollectionPlan::getTotalCapacityPallets).sum();
        int totalSeparators = plans.stream().mapToInt(CollectionPlan::getTotalCapacitySeparators).sum();

        return ResponseEntity.status(HttpStatus.CREATED).body(
                new MultiVehicleOptimizeResponse(plans, plans.size(), totalDistance, totalPallets, totalSeparators)
        );
    }

    /**
     * Generate optimized plan for specific collection requests.
     * Allows cherry-picking which requests to include in the route.
     */
    @Operation(
            summary = "Belirli talepler için optimize rota oluştur",
            description = "Seçili collection requestler için optimize edilmiş toplama planı oluşturur"
    )
    @ApiResponse(responseCode = "201", description = "Rota planı başarıyla oluşturuldu")
    @ApiResponse(responseCode = "400", description = "Geçersiz talepler veya optimizasyon başarısız")
    @PostMapping("/custom")
    @PreAuthorize("hasRole('COMPANY_STAFF')")
    public ResponseEntity<CollectionPlan> generatePlanForRequests(
            @RequestBody CustomOptimizeRequest request
    ) {
        CollectionPlan plan = routeOptimizationService.generatePlanForRequests(
                request.depotId,
                request.requestIds,
                request.plannedDate != null ? request.plannedDate : LocalDate.now().plusDays(1)
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(plan);
    }

    // ===== DTOs =====

    @Schema(description = "Rota optimizasyonu request DTO")
    public record OptimizeRouteRequest(
            @Schema(description = "Depo ID", example = "1", required = true)
            Long depotId,

            @Schema(description = "Planlanan toplama tarihi (varsayılan: yarın)", example = "2026-04-20")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate plannedDate
    ) {}

    @Schema(description = "Özel talep listesi için rota optimizasyonu request DTO")
    public record CustomOptimizeRequest(
            @Schema(description = "Depo ID", example = "1", required = true)
            Long depotId,

            @Schema(description = "Optimize edilecek collection request ID listesi", example = "[1, 2, 3, 4, 5]", required = true)
            List<Long> requestIds,

            @Schema(description = "Planlanan toplama tarihi (varsayılan: yarın)", example = "2026-04-21")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate plannedDate
    ) {}

    /**
     * Generate multi-vehicle optimized routes.
     * Handles large number of fillers by creating multiple vehicle routes.
     */
    @Operation(
            summary = "Çok araçlı optimize rota oluştur",
            description = "Büyük sayıdaki talepler için çoklu araç ile optimize edilmiş rotalar oluşturur (Clarke-Wright algoritması)"
    )
    @ApiResponse(responseCode = "201", description = "Rota planları başarıyla oluşturuldu")
    @ApiResponse(responseCode = "400", description = "Optimizasyon başarısız")
    @PostMapping("/multi-vehicle")
    @PreAuthorize("hasRole('COMPANY_STAFF')")
    public ResponseEntity<MultiVehicleOptimizeResponse> generateMultiVehiclePlan(
            @RequestBody MultiVehicleOptimizeRequest request
    ) {
        List<CollectionPlan> plans = routeOptimizationService.generateMultiVehiclePlan(
                request.depotId,
                request.plannedDate != null ? request.plannedDate : LocalDate.now().plusDays(1),
                request.maxVehicles != null ? request.maxVehicles : 10
        );

        // Calculate summary
        double totalDistance = plans.stream()
                .mapToDouble(p -> p.getTotalDistance().kilometers())
                .sum();

        int totalPallets = plans.stream()
                .mapToInt(CollectionPlan::getTotalCapacityPallets)
                .sum();

        int totalSeparators = plans.stream()
                .mapToInt(CollectionPlan::getTotalCapacitySeparators)
                .sum();

        MultiVehicleOptimizeResponse response = new MultiVehicleOptimizeResponse(
                plans,
                plans.size(),
                totalDistance,
                totalPallets,
                totalSeparators
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Schema(description = "Çoklu araç rota optimizasyonu request DTO")
    public record MultiVehicleOptimizeRequest(
            @Schema(description = "Depo ID", example = "1", required = true)
            Long depotId,

            @Schema(description = "Planlanan toplama tarihi (varsayılan: yarın)", example = "2026-04-20")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate plannedDate,

            @Schema(description = "Maksimum kullanılacak araç sayısı (varsayılan: 10)", example = "5")
            Integer maxVehicles
    ) {}

    /**
     * Generate optimized routes using a fleet composition the user selected
     * from the suggestion cards. Vehicles are auto-assigned, so plans come
     * back in ASSIGNED state instead of GENERATED.
     */
    @Operation(
            summary = "Seçilen filo kompozisyonu ile optimize rota oluştur",
            description = "Filo önerisi kartından seçilen kompozisyon (araç tipi + adet) ile rotaları optimize eder ve " +
                    "müsait araçları otomatik atar. Planlar GENERATED yerine doğrudan ASSIGNED (Araç Atandı) durumunda döner."
    )
    @ApiResponse(responseCode = "201", description = "Rota planları oluşturuldu ve araçlar atandı")
    @ApiResponse(responseCode = "400", description = "Yetersiz müsait araç, geçersiz kompozisyon veya optimizasyon başarısız")
    @PostMapping("/with-fleet")
    @PreAuthorize("hasRole('COMPANY_STAFF')")
    public ResponseEntity<MultiVehicleOptimizeResponse> generatePlanWithFleet(
            @RequestBody OptimizeWithFleetRequest request
    ) {
        Map<Long, Integer> fleetCounts = new LinkedHashMap<>();
        if (request.assignments != null) {
            request.assignments.forEach(a -> fleetCounts.merge(a.vehicleTypeId(), a.count(), Integer::sum));
        }

        List<CollectionPlan> plans = routeOptimizationService.generatePlanWithFleet(
                request.depotId,
                request.plannedDate != null ? request.plannedDate : LocalDate.now().plusDays(1),
                fleetCounts
        );

        double totalDistance = plans.stream()
                .mapToDouble(p -> p.getTotalDistance().kilometers())
                .sum();
        int totalPallets = plans.stream().mapToInt(CollectionPlan::getTotalCapacityPallets).sum();
        int totalSeparators = plans.stream().mapToInt(CollectionPlan::getTotalCapacitySeparators).sum();

        return ResponseEntity.status(HttpStatus.CREATED).body(
                new MultiVehicleOptimizeResponse(plans, plans.size(), totalDistance, totalPallets, totalSeparators)
        );
    }

    @Schema(description = "Seçilen filo kompozisyonu ile rota optimizasyonu request DTO")
    public record OptimizeWithFleetRequest(
            @Schema(description = "Depo ID", example = "1", required = true)
            Long depotId,

            @Schema(description = "Planlanan toplama tarihi (varsayılan: yarın)", example = "2026-06-15")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate plannedDate,

            @Schema(description = "Seçilen kompozisyondaki araç tipi ve adetleri", required = true)
            List<FleetTypeCount> assignments
    ) {}

    @Schema(description = "Araç tipi + adet çifti")
    public record FleetTypeCount(
            @Schema(description = "Araç tipi ID", example = "2", required = true)
            Long vehicleTypeId,

            @Schema(description = "Bu tipten kaç araç kullanılacak", example = "3", required = true)
            int count
    ) {}

    @Operation(
            summary = "Filo öneri kartları",
            description = "Onaylı talepleri toplamak için sistem tarafından önerilen 2-3 alternatif filo kompozisyonu döndürür (en ucuz / en az araç / dengeli). " +
                    "Kullanıcı UI'da bu kartlardan birini seçerek devam eder."
    )
    @ApiResponse(responseCode = "200", description = "Önerilen filo kompozisyonları")
    @PostMapping("/suggest-fleet")
    @PreAuthorize("hasRole('COMPANY_STAFF')")
    public ResponseEntity<List<FleetComposition>> suggestFleet(
            @RequestBody SuggestFleetRequest request,
            HttpServletRequest httpRequest
    ) {
        Long poolOperatorId = jwtUtil.extractPoolOperatorId(
                httpRequest.getHeader("Authorization").substring(7));

        List<CollectionRequest> approved = collectionRequestRepository
                .findByStatus(RequestStatus.APPROVED);
        int totalPallets = approved.stream()
                .filter(r -> r.getAssetType() == ardaaydinkilinc.Cam_Sise.inventory.domain.vo.AssetType.PALLET)
                .mapToInt(CollectionRequest::getEstimatedQuantity).sum();
        int totalSeparators = approved.stream()
                .filter(r -> r.getAssetType() == ardaaydinkilinc.Cam_Sise.inventory.domain.vo.AssetType.SEPARATOR)
                .mapToInt(CollectionRequest::getEstimatedQuantity).sum();
        Capacity demand = new Capacity(totalPallets, totalSeparators);

        double estimatedKm = request.estimatedRouteKm != null ? request.estimatedRouteKm : 0.0;

        List<FleetComposition> compositions = vehicleAssignmentService.suggest(
                poolOperatorId, demand, estimatedKm);
        return ResponseEntity.ok(compositions);
    }

    @Schema(description = "Filo öneri request DTO")
    public record SuggestFleetRequest(
            @Schema(description = "Depo ID", example = "1")
            Long depotId,
            @Schema(description = "Tahmini toplam rota uzunluğu (km) — maliyet hesabı için", example = "350")
            Double estimatedRouteKm
    ) {}

    @Schema(description = "Çoklu araç rota optimizasyonu response DTO")
    public record MultiVehicleOptimizeResponse(
            @Schema(description = "Oluşturulan collection plan listesi (her biri bir araç rotası)")
            List<CollectionPlan> plans,

            @Schema(description = "Kullanılan araç sayısı", example = "3")
            int vehiclesUsed,

            @Schema(description = "Toplam mesafe (km)", example = "789.68")
            double totalDistanceKm,

            @Schema(description = "Toplam palet sayısı", example = "78")
            int totalPallets,

            @Schema(description = "Toplam ayırıcı sayısı", example = "27")
            int totalSeparators
    ) {}
}
