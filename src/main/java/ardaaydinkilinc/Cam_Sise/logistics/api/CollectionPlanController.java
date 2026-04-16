package ardaaydinkilinc.Cam_Sise.logistics.api;

import ardaaydinkilinc.Cam_Sise.logistics.application.service.CollectionPlanService;
import ardaaydinkilinc.Cam_Sise.logistics.domain.CollectionPlan;
import ardaaydinkilinc.Cam_Sise.logistics.domain.vo.PlanStatus;
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
public class CollectionPlanController {

    private final CollectionPlanService collectionPlanService;

    /**
     * Generate a new collection plan (typically called by CVRP optimizer)
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_STAFF')")
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
    @PostMapping("/{planId}/assign-vehicle")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_STAFF')")
    public ResponseEntity<CollectionPlan> assignVehicle(
            @PathVariable Long planId,
            @RequestBody AssignVehicleRequest request
    ) {
        CollectionPlan plan = collectionPlanService.assignVehicle(planId, request.vehicleId);
        return ResponseEntity.ok(plan);
    }

    /**
     * Start collection (vehicle departed)
     */
    @PostMapping("/{planId}/start")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_STAFF')")
    public ResponseEntity<CollectionPlan> startCollection(@PathVariable Long planId) {
        CollectionPlan plan = collectionPlanService.startCollection(planId);
        return ResponseEntity.ok(plan);
    }

    /**
     * Complete collection
     */
    @PostMapping("/{planId}/complete")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_STAFF')")
    public ResponseEntity<CollectionPlan> completeCollection(
            @PathVariable Long planId,
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
    @PostMapping("/{planId}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_STAFF')")
    public ResponseEntity<CollectionPlan> cancelPlan(@PathVariable Long planId) {
        CollectionPlan plan = collectionPlanService.cancelPlan(planId);
        return ResponseEntity.ok(plan);
    }

    /**
     * Get collection plan by ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_STAFF')")
    public ResponseEntity<CollectionPlan> getPlan(@PathVariable Long id) {
        CollectionPlan plan = collectionPlanService.findById(id);
        return ResponseEntity.ok(plan);
    }

    /**
     * Get all collection plans (with optional filters)
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_STAFF')")
    public ResponseEntity<List<CollectionPlan>> getAllPlans(
            @RequestParam(required = false) Long depotId,
            @RequestParam(required = false) PlanStatus status,
            @RequestParam(required = false) Long vehicleId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        List<CollectionPlan> plans;
        if (depotId != null) {
            plans = collectionPlanService.findByDepot(depotId, status);
        } else if (status != null) {
            plans = collectionPlanService.findByStatus(status);
        } else if (vehicleId != null) {
            plans = collectionPlanService.findByVehicle(vehicleId);
        } else if (startDate != null && endDate != null) {
            plans = collectionPlanService.findByDateRange(startDate, endDate);
        } else {
            plans = collectionPlanService.findAll();
        }
        return ResponseEntity.ok(plans);
    }

    /**
     * Get plans for a specific depot
     */
    @GetMapping("/depot/{depotId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_STAFF')")
    public ResponseEntity<List<CollectionPlan>> getPlansByDepot(
            @PathVariable Long depotId,
            @RequestParam(required = false) PlanStatus status
    ) {
        List<CollectionPlan> plans = collectionPlanService.findByDepot(depotId, status);
        return ResponseEntity.ok(plans);
    }

    /**
     * Get plans for a specific vehicle
     */
    @GetMapping("/vehicle/{vehicleId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_STAFF')")
    public ResponseEntity<List<CollectionPlan>> getPlansByVehicle(@PathVariable Long vehicleId) {
        List<CollectionPlan> plans = collectionPlanService.findByVehicle(vehicleId);
        return ResponseEntity.ok(plans);
    }

    // ===== DTOs =====

    public record GeneratePlanRequest(
            Long depotId,
            double totalDistanceKm,
            int estimatedDurationMinutes,
            int totalCapacityPallets,
            int totalCapacitySeparators,
            LocalDate plannedDate,
            String routeStopsJson
    ) {}

    public record AssignVehicleRequest(
            Long vehicleId
    ) {}

    public record CompleteCollectionRequest(
            int actualPalletsCollected,
            int actualSeparatorsCollected
    ) {}
}
