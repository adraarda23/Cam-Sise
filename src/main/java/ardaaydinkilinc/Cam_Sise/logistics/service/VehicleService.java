package ardaaydinkilinc.Cam_Sise.logistics.service;

import ardaaydinkilinc.Cam_Sise.logistics.domain.Vehicle;
import ardaaydinkilinc.Cam_Sise.logistics.domain.vo.DriverInfo;
import ardaaydinkilinc.Cam_Sise.logistics.domain.vo.VehicleStatus;
import ardaaydinkilinc.Cam_Sise.logistics.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Application service for Vehicle aggregate.
 * Manages vehicle registration and operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class VehicleService {

    private final VehicleRepository vehicleRepository;

    /**
     * Register a new vehicle.
     */
    public Vehicle registerVehicle(
            Long depotId,
            Long vehicleTypeId,
            String plateNumber
    ) {
        log.info("Registering vehicle: plateNumber={}, depotId={}", plateNumber, depotId);

        // Check if plate number already exists
        if (vehicleRepository.existsByPlateNumber(plateNumber)) {
            throw new IllegalArgumentException("Vehicle with plate number already exists: " + plateNumber);
        }

        Vehicle vehicle = Vehicle.register(depotId, vehicleTypeId, plateNumber);
        vehicle = vehicleRepository.save(vehicle);

        log.info("Vehicle registered successfully: id={}, plateNumber={}", vehicle.getId(), plateNumber);

        return vehicle;
    }

    /**
     * Assign vehicle to a collection plan (without driver info).
     */
    public Vehicle assignToPlan(Long vehicleId, Long collectionPlanId) {
        log.info("Assigning vehicle to plan: vehicleId={}, collectionPlanId={}", vehicleId, collectionPlanId);

        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found: " + vehicleId));

        vehicle.assignToPlan(collectionPlanId);
        vehicle = vehicleRepository.save(vehicle);

        log.info("Vehicle assigned to plan successfully: vehicleId={}, collectionPlanId={}", vehicleId, collectionPlanId);

        return vehicle;
    }

    /**
     * Assign vehicle to a collection route (with driver info).
     */
    public Vehicle assignToRoute(
            Long vehicleId,
            Long collectionPlanId,
            String driverName,
            String licenseNumber,
            String phone
    ) {
        log.info("Assigning vehicle to route: vehicleId={}, collectionPlanId={}", vehicleId, collectionPlanId);

        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found: " + vehicleId));

        DriverInfo driver = new DriverInfo(driverName, licenseNumber, phone);
        vehicle.assignToRoute(collectionPlanId, driver);
        vehicle = vehicleRepository.save(vehicle);

        log.info("Vehicle assigned to route successfully: vehicleId={}, collectionPlanId={}", vehicleId, collectionPlanId);

        return vehicle;
    }

    /**
     * Vehicle departs from depot.
     */
    public Vehicle departFromDepot(Long vehicleId) {
        log.info("Vehicle departing from depot: vehicleId={}", vehicleId);

        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found: " + vehicleId));

        vehicle.departFromDepot();
        vehicle = vehicleRepository.save(vehicle);

        log.info("Vehicle departed from depot: vehicleId={}", vehicleId);

        return vehicle;
    }

    /**
     * Vehicle returns to depot.
     */
    public Vehicle returnToDepot(Long vehicleId) {
        log.info("Vehicle returning to depot: vehicleId={}", vehicleId);

        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found: " + vehicleId));

        vehicle.returnToDepot();
        vehicle = vehicleRepository.save(vehicle);

        log.info("Vehicle returned to depot: vehicleId={}", vehicleId);

        return vehicle;
    }

    /**
     * Change vehicle status.
     */
    public Vehicle changeStatus(Long vehicleId, VehicleStatus newStatus) {
        log.info("Changing vehicle status: vehicleId={}, newStatus={}", vehicleId, newStatus);

        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found: " + vehicleId));

        vehicle.changeStatus(newStatus);
        vehicle = vehicleRepository.save(vehicle);

        log.info("Vehicle status changed: vehicleId={}, newStatus={}", vehicleId, newStatus);

        return vehicle;
    }

    /**
     * Find vehicle by ID.
     */
    @Transactional(readOnly = true)
    public Vehicle findById(Long vehicleId) {
        return vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found: " + vehicleId));
    }

    /**
     * Find vehicle by plate number.
     */
    @Transactional(readOnly = true)
    public Vehicle findByPlateNumber(String plateNumber) {
        return vehicleRepository.findByPlateNumber(plateNumber)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found with plate: " + plateNumber));
    }

    /**
     * Find all vehicles at a depot.
     */
    @Transactional(readOnly = true)
    public List<Vehicle> findByDepot(Long depotId, VehicleStatus status) {
        if (status != null) {
            return vehicleRepository.findByDepotIdAndStatus(depotId, status);
        }
        return vehicleRepository.findByDepotId(depotId);
    }

    /**
     * Find all vehicles by status.
     */
    @Transactional(readOnly = true)
    public List<Vehicle> findByStatus(VehicleStatus status) {
        return vehicleRepository.findByStatus(status);
    }

    /**
     * Find all vehicles.
     */
    @Transactional(readOnly = true)
    public List<Vehicle> findAll() {
        return vehicleRepository.findAll();
    }
}
