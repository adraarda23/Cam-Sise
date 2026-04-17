package ardaaydinkilinc.Cam_Sise.logistics.controller;

import ardaaydinkilinc.Cam_Sise.logistics.domain.CollectionPlan;
import ardaaydinkilinc.Cam_Sise.logistics.service.RouteOptimizationService;
import io.swagger.v3.oas.annotations.Operation;
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

    /**
     * Generate optimized collection plan for all approved requests.
     * This endpoint triggers the CVRP optimizer to create an optimal route.
     */
    @Operation(
            summary = "Onaylı talepler için optimize rota oluştur",
            description = "Tüm onaylanmış collection requestleri için CVRP algoritması ile optimize edilmiş toplama planı oluşturur"
    )
    @ApiResponse(responseCode = "201", description = "Rota planı başarıyla oluşturuldu")
    @ApiResponse(responseCode = "400", description = "Onaylı talep bulunamadı veya optimizasyon başarısız")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_STAFF')")
    public ResponseEntity<CollectionPlan> generateOptimizedPlan(
            @RequestBody OptimizeRouteRequest request
    ) {
        CollectionPlan plan = routeOptimizationService.generateOptimizedPlan(
                request.depotId,
                request.plannedDate != null ? request.plannedDate : LocalDate.now().plusDays(1)
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(plan);
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
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_STAFF')")
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

    public record OptimizeRouteRequest(
            Long depotId,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate plannedDate
    ) {}

    public record CustomOptimizeRequest(
            Long depotId,
            List<Long> requestIds,
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
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_STAFF')")
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

    public record MultiVehicleOptimizeRequest(
            Long depotId,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate plannedDate,
            Integer maxVehicles
    ) {}

    public record MultiVehicleOptimizeResponse(
            List<CollectionPlan> plans,
            int vehiclesUsed,
            double totalDistanceKm,
            int totalPallets,
            int totalSeparators
    ) {}
}
