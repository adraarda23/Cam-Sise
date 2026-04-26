package ardaaydinkilinc.Cam_Sise.logistics.service;

import ardaaydinkilinc.Cam_Sise.logistics.domain.CollectionPlan;
import ardaaydinkilinc.Cam_Sise.logistics.domain.Vehicle;
import ardaaydinkilinc.Cam_Sise.logistics.domain.vo.DriverInfo;
import ardaaydinkilinc.Cam_Sise.logistics.domain.vo.PlanStatus;
import ardaaydinkilinc.Cam_Sise.logistics.domain.vo.VehicleStatus;
import ardaaydinkilinc.Cam_Sise.logistics.repository.CollectionPlanRepository;
import ardaaydinkilinc.Cam_Sise.logistics.repository.CollectionRequestRepository;
import ardaaydinkilinc.Cam_Sise.logistics.repository.VehicleRepository;
import ardaaydinkilinc.Cam_Sise.shared.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
    private final CollectionPlanRepository collectionPlanRepository;
    private final CollectionRequestRepository collectionRequestRepository;

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

        VehicleStatus oldStatus = vehicle.getStatus();

        vehicle.changeStatus(newStatus);
        vehicle = vehicleRepository.save(vehicle);

        log.info("Vehicle status changed: vehicleId={}, oldStatus={}, newStatus={}", vehicleId, oldStatus, newStatus);

        // CRITICAL: If vehicle is leaving ON_ROUTE status, cancel all active plans
        if (oldStatus == VehicleStatus.ON_ROUTE && newStatus != VehicleStatus.ON_ROUTE) {
            log.warn("⚠️ Vehicle {} leaving ON_ROUTE status ({} -> {}), cancelling active plans",
                    vehicleId, oldStatus, newStatus);

            // Find all active plans (IN_PROGRESS or ASSIGNED) for this vehicle
            List<CollectionPlan> activePlans = collectionPlanRepository.findByAssignedVehicleId(vehicleId)
                    .stream()
                    .filter(plan -> plan.getStatus() == PlanStatus.IN_PROGRESS ||
                                   plan.getStatus() == PlanStatus.ASSIGNED)
                    .toList();

            for (CollectionPlan plan : activePlans) {
                log.warn("🚨 Cancelling plan {} (status: {}) due to vehicle {} going to {}",
                        plan.getId(), plan.getStatus(), vehicleId, newStatus);

                // Cancel the plan
                plan.cancel();
                collectionPlanRepository.save(plan);

                // Cancel all collection requests for this plan
                var requests = collectionRequestRepository.findByCollectionPlanId(plan.getId());
                for (var request : requests) {
                    log.info("Cancelling collection request {} from plan {}", request.getId(), plan.getId());
                    request.cancel();
                    collectionRequestRepository.save(request);
                }

                log.info("✅ Plan {} and its {} requests cancelled", plan.getId(), requests.size());
            }

            log.info("✅ Cancelled {} active plans for vehicle {}", activePlans.size(), vehicleId);
        }

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

    @Transactional(readOnly = true)
    public List<Vehicle> findByPoolOperatorId(Long poolOperatorId) {
        return vehicleRepository.findByPoolOperatorId(poolOperatorId);
    }

    @Transactional(readOnly = true)
    public PageResponse<Vehicle> findByPoolOperatorIdPaged(Long poolOperatorId, VehicleStatus status, String search, int page, int size) {
        String searchParam = (search == null || search.isBlank()) ? "" : search;
        var pageable = PageRequest.of(page, size, Sort.by("id").descending());
        return PageResponse.from(vehicleRepository.findByPoolOperatorIdFiltered(poolOperatorId, status, searchParam, pageable));
    }
}
