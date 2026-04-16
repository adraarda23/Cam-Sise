package ardaaydinkilinc.Cam_Sise.logistics.controller;

import ardaaydinkilinc.Cam_Sise.logistics.service.VehicleService;
import ardaaydinkilinc.Cam_Sise.logistics.domain.Vehicle;
import ardaaydinkilinc.Cam_Sise.logistics.domain.vo.VehicleStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API for Vehicle management.
 */
@RestController
@RequestMapping("/api/logistics/vehicles")
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleService vehicleService;

    /**
     * Register a new vehicle
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_STAFF')")
    public ResponseEntity<Vehicle> registerVehicle(@RequestBody RegisterVehicleRequest request) {
        Vehicle vehicle = vehicleService.registerVehicle(
                request.depotId,
                request.vehicleTypeId,
                request.plateNumber
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(vehicle);
    }

    /**
     * Assign vehicle to collection route
     */
    @PostMapping("/{vehicleId}/assign")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_STAFF')")
    public ResponseEntity<Vehicle> assignToRoute(
            @PathVariable Long vehicleId,
            @RequestBody AssignToRouteRequest request
    ) {
        Vehicle vehicle = vehicleService.assignToRoute(
                vehicleId,
                request.collectionPlanId,
                request.driverName,
                request.licenseNumber,
                request.phone
        );
        return ResponseEntity.ok(vehicle);
    }

    /**
     * Vehicle departs from depot
     */
    @PostMapping("/{vehicleId}/depart")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_STAFF')")
    public ResponseEntity<Vehicle> departFromDepot(@PathVariable Long vehicleId) {
        Vehicle vehicle = vehicleService.departFromDepot(vehicleId);
        return ResponseEntity.ok(vehicle);
    }

    /**
     * Vehicle returns to depot
     */
    @PostMapping("/{vehicleId}/return")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_STAFF')")
    public ResponseEntity<Vehicle> returnToDepot(@PathVariable Long vehicleId) {
        Vehicle vehicle = vehicleService.returnToDepot(vehicleId);
        return ResponseEntity.ok(vehicle);
    }

    /**
     * Change vehicle status
     */
    @PutMapping("/{vehicleId}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_STAFF')")
    public ResponseEntity<Vehicle> changeStatus(
            @PathVariable Long vehicleId,
            @RequestBody ChangeStatusRequest request
    ) {
        Vehicle vehicle = vehicleService.changeStatus(vehicleId, request.newStatus);
        return ResponseEntity.ok(vehicle);
    }

    /**
     * Get vehicle by ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_STAFF')")
    public ResponseEntity<Vehicle> getVehicle(@PathVariable Long id) {
        Vehicle vehicle = vehicleService.findById(id);
        return ResponseEntity.ok(vehicle);
    }

    /**
     * Get vehicle by plate number
     */
    @GetMapping("/plate/{plateNumber}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_STAFF')")
    public ResponseEntity<Vehicle> getVehicleByPlate(@PathVariable String plateNumber) {
        Vehicle vehicle = vehicleService.findByPlateNumber(plateNumber);
        return ResponseEntity.ok(vehicle);
    }

    /**
     * Get all vehicles (with optional filters)
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_STAFF')")
    public ResponseEntity<List<Vehicle>> getAllVehicles(
            @RequestParam(required = false) Long depotId,
            @RequestParam(required = false) VehicleStatus status
    ) {
        List<Vehicle> vehicles;
        if (depotId != null) {
            vehicles = vehicleService.findByDepot(depotId, status);
        } else if (status != null) {
            vehicles = vehicleService.findByStatus(status);
        } else {
            vehicles = vehicleService.findAll();
        }
        return ResponseEntity.ok(vehicles);
    }

    // ===== DTOs =====

    public record RegisterVehicleRequest(
            Long depotId,
            Long vehicleTypeId,
            String plateNumber
    ) {}

    public record AssignToRouteRequest(
            Long collectionPlanId,
            String driverName,
            String licenseNumber,
            String phone
    ) {}

    public record ChangeStatusRequest(
            VehicleStatus newStatus
    ) {}
}
