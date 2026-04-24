package ardaaydinkilinc.Cam_Sise.logistics.service;

import ardaaydinkilinc.Cam_Sise.logistics.domain.CollectionPlan;
import ardaaydinkilinc.Cam_Sise.logistics.domain.vo.PlanStatus;
import ardaaydinkilinc.Cam_Sise.logistics.domain.vo.VehicleStatus;
import ardaaydinkilinc.Cam_Sise.logistics.repository.CollectionPlanRepository;
import ardaaydinkilinc.Cam_Sise.logistics.repository.CollectionRequestRepository;
import ardaaydinkilinc.Cam_Sise.shared.domain.vo.Distance;
import ardaaydinkilinc.Cam_Sise.shared.domain.vo.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Application service for CollectionPlan aggregate.
 * Manages collection route planning and execution.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CollectionPlanService {

    private final CollectionPlanRepository collectionPlanRepository;
    private final VehicleService vehicleService;
    private final CollectionRequestService collectionRequestService;
    private final CollectionRequestRepository collectionRequestRepository;

    /**
     * Generate a new collection plan (typically called by CVRP optimizer).
     */
    public CollectionPlan generatePlan(
            Long depotId,
            double totalDistanceKm,
            int estimatedDurationMinutes,
            int totalCapacityPallets,
            int totalCapacitySeparators,
            LocalDate plannedDate,
            String routeStopsJson
    ) {
        log.info("Generating collection plan: depotId={}, plannedDate={}, distance={}km",
                depotId, plannedDate, totalDistanceKm);

        Distance distance = new Distance(totalDistanceKm);
        Duration duration = new Duration(estimatedDurationMinutes);

        CollectionPlan plan = CollectionPlan.generate(
                depotId,
                distance,
                duration,
                totalCapacityPallets,
                totalCapacitySeparators,
                plannedDate,
                routeStopsJson
        );

        plan = collectionPlanRepository.save(plan);

        log.info("Collection plan generated: planId={}, depotId={}", plan.getId(), depotId);

        return plan;
    }

    /**
     * Assign vehicle to a collection plan.
     */
    public CollectionPlan assignVehicle(Long planId, Long vehicleId) {
        log.info("Assigning vehicle to plan: planId={}, vehicleId={}", planId, vehicleId);

        CollectionPlan plan = collectionPlanRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("Collection plan not found: " + planId));

        plan.assignVehicle(vehicleId);
        plan = collectionPlanRepository.save(plan);

        // Change vehicle status to ON_ROUTE
        vehicleService.assignToPlan(vehicleId, planId);

        log.info("Vehicle assigned to plan: planId={}, vehicleId={}", planId, vehicleId);

        return plan;
    }

    /**
     * Start collection (vehicle departed).
     */
    public CollectionPlan startCollection(Long planId) {
        log.info("Starting collection: planId={}", planId);

        CollectionPlan plan = collectionPlanRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("Collection plan not found: " + planId));

        plan.start();
        plan = collectionPlanRepository.save(plan);

        log.info("Collection started: planId={}", planId);

        return plan;
    }

    /**
     * Complete collection.
     */
    public CollectionPlan completeCollection(
            Long planId,
            int actualPalletsCollected,
            int actualSeparatorsCollected
    ) {
        log.info("Completing collection: planId={}, pallets={}, separators={}",
                planId, actualPalletsCollected, actualSeparatorsCollected);

        CollectionPlan plan = collectionPlanRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("Collection plan not found: " + planId));

        plan.complete(actualPalletsCollected, actualSeparatorsCollected);
        plan = collectionPlanRepository.save(plan);

        // Mark all associated collection requests as COMPLETED
        var collectionRequests = collectionRequestRepository.findByCollectionPlanId(planId);
        if (!collectionRequests.isEmpty()) {
            log.info("Completing {} collection requests associated with plan {}", collectionRequests.size(), planId);
            for (var request : collectionRequests) {
                try {
                    collectionRequestService.complete(request.getId());
                } catch (Exception e) {
                    log.error("Failed to complete collection request {}: {}", request.getId(), e.getMessage(), e);
                }
            }
        }

        // Return vehicle to depot (change status to AVAILABLE)
        if (plan.getAssignedVehicleId() != null) {
            vehicleService.returnToDepot(plan.getAssignedVehicleId());
        }

        log.info("Collection completed: planId={}", planId);

        return plan;
    }

    /**
     * Cancel a collection plan.
     */
    public CollectionPlan cancelPlan(Long planId) {
        log.info("Cancelling collection plan: planId={}", planId);

        CollectionPlan plan = collectionPlanRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("Collection plan not found: " + planId));

        plan.cancel();
        plan = collectionPlanRepository.save(plan);

        // Cancel all collection requests associated with this plan
        var collectionRequests = collectionRequestRepository.findByCollectionPlanId(planId);
        if (!collectionRequests.isEmpty()) {
            log.info("Cancelling {} collection requests associated with plan {}",
                    collectionRequests.size(), planId);

            for (var request : collectionRequests) {
                try {
                    collectionRequestService.cancel(request.getId());
                    log.info("Cancelled collection request {} due to plan cancellation", request.getId());
                } catch (Exception e) {
                    log.error("Failed to cancel collection request {}: {}",
                            request.getId(), e.getMessage(), e);
                }
            }
        }

        // Return vehicle to depot if it was assigned and still ON_ROUTE
        if (plan.getAssignedVehicleId() != null) {
            try {
                vehicleService.returnToDepot(plan.getAssignedVehicleId());
            } catch (IllegalStateException e) {
                // Vehicle is not ON_ROUTE anymore (e.g., moved to MAINTENANCE)
                // Just clear the vehicle's plan reference
                log.warn("Vehicle {} is not ON_ROUTE, clearing plan reference only: {}",
                        plan.getAssignedVehicleId(), e.getMessage());

                // Fetch vehicle and clear its collection plan reference
                var vehicle = vehicleService.findById(plan.getAssignedVehicleId());
                if (vehicle.getCurrentCollectionPlanId() != null) {
                    // Vehicle needs manual cleanup - we can't call returnToDepot
                    // This is handled by the vehicle's own state management
                    log.info("Vehicle {} plan reference will be cleared by vehicle state management",
                            plan.getAssignedVehicleId());
                }
            }
        }

        log.info("Collection plan cancelled: planId={}", planId);

        return plan;
    }

    /**
     * Find collection plan by ID.
     */
    @Transactional(readOnly = true)
    public CollectionPlan findById(Long planId) {
        return collectionPlanRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("Collection plan not found: " + planId));
    }

    /**
     * Find all plans for a depot.
     */
    @Transactional(readOnly = true)
    public List<CollectionPlan> findByDepot(Long depotId, PlanStatus status) {
        if (status != null) {
            return collectionPlanRepository.findByDepotIdAndStatus(depotId, status);
        }
        return collectionPlanRepository.findByDepotId(depotId);
    }

    /**
     * Find all plans by status.
     */
    @Transactional(readOnly = true)
    public List<CollectionPlan> findByStatus(PlanStatus status) {
        return collectionPlanRepository.findByStatus(status);
    }

    /**
     * Find all plans for a vehicle.
     */
    @Transactional(readOnly = true)
    public List<CollectionPlan> findByVehicle(Long vehicleId) {
        return collectionPlanRepository.findByAssignedVehicleId(vehicleId);
    }

    /**
     * Find plans by date range.
     */
    @Transactional(readOnly = true)
    public List<CollectionPlan> findByDateRange(LocalDate startDate, LocalDate endDate) {
        return collectionPlanRepository.findByPlannedDateBetween(startDate, endDate);
    }

    /**
     * Find all plans.
     */
    @Transactional(readOnly = true)
    public List<CollectionPlan> findAll() {
        return collectionPlanRepository.findAll();
    }
}
