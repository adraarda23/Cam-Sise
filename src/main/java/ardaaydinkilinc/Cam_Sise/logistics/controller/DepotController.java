package ardaaydinkilinc.Cam_Sise.logistics.controller;

import ardaaydinkilinc.Cam_Sise.logistics.service.DepotService;
import ardaaydinkilinc.Cam_Sise.logistics.domain.Depot;
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
public class DepotController {

    private final DepotService depotService;

    /**
     * Create a new depot
     */
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
    @PostMapping("/{depotId}/vehicles/{vehicleId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_STAFF')")
    public ResponseEntity<Depot> addVehicle(
            @PathVariable Long depotId,
            @PathVariable Long vehicleId
    ) {
        Depot depot = depotService.addVehicle(depotId, vehicleId);
        return ResponseEntity.ok(depot);
    }

    /**
     * Remove vehicle from depot
     */
    @DeleteMapping("/{depotId}/vehicles/{vehicleId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_STAFF')")
    public ResponseEntity<Depot> removeVehicle(
            @PathVariable Long depotId,
            @PathVariable Long vehicleId
    ) {
        Depot depot = depotService.removeVehicle(depotId, vehicleId);
        return ResponseEntity.ok(depot);
    }

    /**
     * Get depot by ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_STAFF')")
    public ResponseEntity<Depot> getDepot(@PathVariable Long id) {
        Depot depot = depotService.findById(id);
        return ResponseEntity.ok(depot);
    }

    /**
     * Get all depots (with optional filters)
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_STAFF')")
    public ResponseEntity<List<Depot>> getAllDepots(
            @RequestParam(required = false) Long poolOperatorId,
            @RequestParam(required = false) Boolean active
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

    public record CreateDepotRequest(
            Long poolOperatorId,
            String name,
            String street,
            String city,
            String province,
            String postalCode,
            String country,
            Double latitude,
            Double longitude
    ) {}
}
