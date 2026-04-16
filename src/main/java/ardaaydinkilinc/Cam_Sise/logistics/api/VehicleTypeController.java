package ardaaydinkilinc.Cam_Sise.logistics.api;

import ardaaydinkilinc.Cam_Sise.logistics.application.service.VehicleTypeService;
import ardaaydinkilinc.Cam_Sise.logistics.domain.VehicleType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API for VehicleType management.
 */
@RestController
@RequestMapping("/api/logistics/vehicle-types")
@RequiredArgsConstructor
public class VehicleTypeController {

    private final VehicleTypeService vehicleTypeService;

    /**
     * Create a new vehicle type
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_STAFF')")
    public ResponseEntity<VehicleType> createVehicleType(@RequestBody CreateVehicleTypeRequest request) {
        VehicleType vehicleType = vehicleTypeService.createVehicleType(
                request.poolOperatorId,
                request.name,
                request.description,
                request.palletCapacity,
                request.separatorCapacity
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(vehicleType);
    }

    /**
     * Update vehicle type capacity
     */
    @PutMapping("/{id}/capacity")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_STAFF')")
    public ResponseEntity<VehicleType> updateCapacity(
            @PathVariable Long id,
            @RequestBody UpdateCapacityRequest request
    ) {
        VehicleType vehicleType = vehicleTypeService.updateCapacity(
                id,
                request.palletCapacity,
                request.separatorCapacity
        );
        return ResponseEntity.ok(vehicleType);
    }

    /**
     * Deactivate vehicle type
     */
    @PostMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_STAFF')")
    public ResponseEntity<VehicleType> deactivateVehicleType(@PathVariable Long id) {
        VehicleType vehicleType = vehicleTypeService.deactivateVehicleType(id);
        return ResponseEntity.ok(vehicleType);
    }

    /**
     * Get vehicle type by ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_STAFF')")
    public ResponseEntity<VehicleType> getVehicleType(@PathVariable Long id) {
        VehicleType vehicleType = vehicleTypeService.findById(id);
        return ResponseEntity.ok(vehicleType);
    }

    /**
     * Get all vehicle types (with optional filters)
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_STAFF')")
    public ResponseEntity<List<VehicleType>> getAllVehicleTypes(
            @RequestParam(required = false) Long poolOperatorId,
            @RequestParam(required = false) Boolean active
    ) {
        List<VehicleType> vehicleTypes;
        if (poolOperatorId != null) {
            vehicleTypes = vehicleTypeService.findByPoolOperator(poolOperatorId, active);
        } else if (active != null && active) {
            vehicleTypes = vehicleTypeService.findAllActive();
        } else {
            vehicleTypes = vehicleTypeService.findAll();
        }
        return ResponseEntity.ok(vehicleTypes);
    }

    // ===== DTOs =====

    public record CreateVehicleTypeRequest(
            Long poolOperatorId,
            String name,
            String description,
            int palletCapacity,
            int separatorCapacity
    ) {}

    public record UpdateCapacityRequest(
            int palletCapacity,
            int separatorCapacity
    ) {}
}
