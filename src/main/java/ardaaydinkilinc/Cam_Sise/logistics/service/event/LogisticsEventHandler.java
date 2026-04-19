package ardaaydinkilinc.Cam_Sise.logistics.service.event;

import ardaaydinkilinc.Cam_Sise.logistics.service.CollectionRequestService;
import ardaaydinkilinc.Cam_Sise.logistics.service.RouteOptimizationService;
import ardaaydinkilinc.Cam_Sise.logistics.service.CollectionPlanService;
import ardaaydinkilinc.Cam_Sise.logistics.service.VehicleService;
import ardaaydinkilinc.Cam_Sise.logistics.domain.event.*;
import ardaaydinkilinc.Cam_Sise.logistics.domain.Vehicle;
import ardaaydinkilinc.Cam_Sise.logistics.domain.vo.VehicleStatus;
import ardaaydinkilinc.Cam_Sise.logistics.domain.vo.PlanStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Event handler for Logistics module domain events.
 * Handles side effects and cross-aggregate coordination.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LogisticsEventHandler {

    private final CollectionRequestService collectionRequestService;
    private final RouteOptimizationService routeOptimizationService;
    private final CollectionPlanService collectionPlanService;
    private final VehicleService vehicleService;

    /**
     * Handle CollectionRequestApproved event
     */
    @EventListener
    @Async
    public void handleCollectionRequestApproved(CollectionRequestApproved event) {
        log.info("📋 Collection request approved: requestId={}, fillerId={}, approvedBy={}",
                event.requestId(),
                event.fillerId(),
                event.approvedByUserId());

        // Note: Automatic route optimization is not triggered here
        // Admin must manually trigger optimization via POST /api/logistics/optimize endpoint
        // This allows batching multiple approved requests into a single optimized route

        // TODO: Notify filler about approval
        // TODO: Update dashboard/analytics
    }

    /**
     * Handle CollectionRequestRejected event
     */
    @EventListener
    @Async
    public void handleCollectionRequestRejected(CollectionRequestRejected event) {
        log.info("❌ Collection request rejected: requestId={}, fillerId={}, reason={}",
                event.requestId(),
                event.fillerId(),
                event.reason());

        // TODO: Notify filler about rejection
    }

    /**
     * Handle CollectionPlanGenerated event
     */
    @EventListener
    @Async
    public void handleCollectionPlanGenerated(CollectionPlanGenerated event) {
        log.info("🗺️ Collection plan generated: depotId={}, plannedDate={}, distance={}km, capacity={} pallets, {} separators",
                event.depotId(),
                event.plannedDate(),
                event.totalDistance().kilometers(),
                event.totalPallets(),
                event.totalSeparators());

        // TODO: Schedule collection requests included in this plan
        // TODO: Notify relevant parties
    }

    /**
     * Handle RouteAssignedToVehicle event
     */
    @EventListener
    @Async
    public void handleRouteAssignedToVehicle(RouteAssignedToVehicle event) {
        log.info("🚛 Route assigned to vehicle: planId={}, vehicleId={}, plannedDate={}",
                event.collectionPlanId(),
                event.vehicleId(),
                event.plannedDate());

        // TODO: Notify driver
        // TODO: Update vehicle scheduling system
    }

    /**
     * Handle CollectionStarted event
     */
    @EventListener
    @Async
    public void handleCollectionStarted(CollectionStarted event) {
        log.info("🚀 Collection started: planId={}, vehicleId={}, depotId={}",
                event.collectionPlanId(),
                event.vehicleId(),
                event.depotId());

        // TODO: Start real-time tracking
        // TODO: Notify fillers on the route
    }

    /**
     * Handle CollectionCompleted event
     */
    @EventListener
    @Async
    public void handleCollectionCompleted(CollectionCompleted event) {
        log.info("✅ Collection completed: planId={}, vehicleId={}, collected={} pallets, {} separators",
                event.collectionPlanId(),
                event.vehicleId(),
                event.actualPalletsCollected(),
                event.actualSeparatorsCollected());

        // TODO: Update stock levels at fillers (deduct collected amounts)
        // TODO: Mark collection requests as completed
        // TODO: Update analytics/reporting
        // TODO: Generate completion report
    }

    /**
     * Handle VehicleRegistered event
     */
    @EventListener
    @Async
    public void handleVehicleRegistered(VehicleRegistered event) {
        log.info("🚛 Vehicle registered: depotId={}, vehicleTypeId={}, plateNumber={}",
                event.depotId(),
                event.vehicleTypeId(),
                event.plateNumber());

        // TODO: Add to depot vehicle pool
        // TODO: Update capacity planning
    }

    /**
     * Handle DepotCreated event
     */
    @EventListener
    @Async
    public void handleDepotCreated(DepotCreated event) {
        log.info("🏭 Depot created: poolOperatorId={}, name={}, location={}",
                event.poolOperatorId(),
                event.depotName(),
                event.location());

        // TODO: Initialize routing system for this depot
        // TODO: Update logistics network
    }

    /**
     * Handle VehicleStatusChanged event
     * Automatically cancel active collection plan when vehicle leaves ON_ROUTE status
     */
    @EventListener
    @Async
    public void handleVehicleStatusChanged(VehicleStatusChanged event) {
        log.info("🚗 Vehicle status changed: vehicleId={}, {} -> {}",
                event.vehicleId(),
                event.oldStatus(),
                event.newStatus());

        // If vehicle is leaving ON_ROUTE status (accident, maintenance, etc.)
        if (event.oldStatus() == VehicleStatus.ON_ROUTE && event.newStatus() != VehicleStatus.ON_ROUTE) {
            try {
                // Find active IN_PROGRESS plans for this vehicle
                var activePlans = collectionPlanService.findByVehicle(event.vehicleId())
                        .stream()
                        .filter(plan -> plan.getStatus() == PlanStatus.IN_PROGRESS)
                        .toList();

                if (!activePlans.isEmpty()) {
                    for (var plan : activePlans) {
                        log.warn("⚠️ Vehicle {} left ON_ROUTE status while on active plan {}. Cancelling plan.",
                                event.vehicleId(),
                                plan.getId());

                        // Cancel the active plan
                        collectionPlanService.cancelPlan(plan.getId());

                        log.info("✅ Collection plan {} automatically cancelled due to vehicle status change",
                                plan.getId());
                    }
                } else {
                    log.info("ℹ️ No active plans found for vehicle {}", event.vehicleId());
                }
            } catch (Exception e) {
                log.error("❌ Failed to cancel collection plan for vehicle {}: {}",
                        event.vehicleId(),
                        e.getMessage(),
                        e);
            }
        }
    }
}
