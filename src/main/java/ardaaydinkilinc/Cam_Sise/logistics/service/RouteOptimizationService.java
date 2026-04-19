package ardaaydinkilinc.Cam_Sise.logistics.service;

import ardaaydinkilinc.Cam_Sise.core.domain.Filler;
import ardaaydinkilinc.Cam_Sise.core.repository.FillerRepository;
import ardaaydinkilinc.Cam_Sise.logistics.domain.CollectionPlan;
import ardaaydinkilinc.Cam_Sise.logistics.domain.CollectionRequest;
import ardaaydinkilinc.Cam_Sise.logistics.domain.Depot;
import ardaaydinkilinc.Cam_Sise.logistics.domain.VehicleType;
import ardaaydinkilinc.Cam_Sise.logistics.domain.vo.Capacity;
import ardaaydinkilinc.Cam_Sise.logistics.domain.vo.RequestStatus;
import ardaaydinkilinc.Cam_Sise.logistics.repository.CollectionRequestRepository;
import ardaaydinkilinc.Cam_Sise.logistics.repository.DepotRepository;
import ardaaydinkilinc.Cam_Sise.logistics.repository.VehicleTypeRepository;
import ardaaydinkilinc.Cam_Sise.shared.domain.vo.Distance;
import ardaaydinkilinc.Cam_Sise.shared.domain.vo.Duration;
import ardaaydinkilinc.Cam_Sise.shared.domain.vo.GeoCoordinates;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Route optimization orchestration service.
 * Combines CVRP optimizer with domain logic to create optimized collection plans.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class RouteOptimizationService {

    private final CVRPOptimizer cvrpOptimizer;
    private final DistanceCalculator distanceCalculator;
    private final CollectionPlanService collectionPlanService;
    private final CollectionRequestRepository collectionRequestRepository;
    private final DepotRepository depotRepository;
    private final FillerRepository fillerRepository;
    private final VehicleTypeRepository vehicleTypeRepository;
    private final ObjectMapper objectMapper;

    /**
     * Generate optimized collection plan for approved requests.
     *
     * @param depotId Depot to start from
     * @param plannedDate Date for collection
     * @return Created collection plan
     */
    public CollectionPlan generateOptimizedPlan(Long depotId, LocalDate plannedDate) {
        log.info("Generating optimized collection plan: depotId={}, date={}", depotId, plannedDate);

        // 1. Get depot
        Depot depot = depotRepository.findById(depotId)
                .orElseThrow(() -> new IllegalArgumentException("Depot not found: " + depotId));

        // 2. Get approved collection requests
        List<CollectionRequest> approvedRequests = collectionRequestRepository
                .findByStatus(RequestStatus.APPROVED);

        if (approvedRequests.isEmpty()) {
            throw new IllegalStateException("No approved collection requests found");
        }

        log.info("Found {} approved requests", approvedRequests.size());

        // 3. Get default vehicle type (largest capacity)
        VehicleType vehicleType = vehicleTypeRepository
                .findByPoolOperatorIdAndActive(depot.getPoolOperatorId(), true)
                .stream()
                .max((v1, v2) -> Integer.compare(
                        v1.getCapacity().pallets() + v1.getCapacity().separators(),
                        v2.getCapacity().pallets() + v2.getCapacity().separators()
                ))
                .orElseThrow(() -> new IllegalStateException("No active vehicle types found"));

        log.info("Using vehicle type: {} with capacity {}",
                vehicleType.getName(), vehicleType.getCapacity().formatted());

        // 4. Convert requests to CVRP nodes
        List<CVRPOptimizer.CollectionNode> nodes = convertRequestsToNodes(approvedRequests);

        // 5. Optimize route
        CVRPOptimizer.RouteSolution solution = cvrpOptimizer.optimizeRoute(
                depot.getLocation(),
                nodes,
                vehicleType.getCapacity()
        );

        if (solution.route().isEmpty()) {
            throw new IllegalStateException("Optimization failed: no feasible route found");
        }

        // 6. Convert solution to RouteStops JSON
        String routeStopsJson = convertSolutionToJson(solution);

        // 7. Calculate duration
        int estimatedDurationMinutes = distanceCalculator.estimateDuration(solution.totalDistance());

        // 8. Create collection plan
        CollectionPlan plan = collectionPlanService.generatePlan(
                depotId,
                solution.totalDistance(),
                estimatedDurationMinutes,
                solution.totalLoad().pallets(),
                solution.totalLoad().separators(),
                plannedDate,
                routeStopsJson
        );

        // 9. Mark all used requests as SCHEDULED
        for (CollectionRequest request : approvedRequests) {
            if (request.getStatus() == RequestStatus.APPROVED) { // Only schedule if still APPROVED
                request.schedule(plan.getId());
                collectionRequestRepository.save(request);
            }
        }

        log.info("✅ Collection plan created: planId={}, stops={}, distance={} km, duration={} min, requests scheduled={}",
                plan.getId(),
                solution.route().size(),
                String.format("%.2f", solution.totalDistance()),
                estimatedDurationMinutes,
                approvedRequests.size());

        return plan;
    }

    /**
     * Generate plan for specific collection requests.
     *
     * @param depotId Depot ID
     * @param requestIds Collection request IDs to include
     * @param plannedDate Planned collection date
     * @return Created collection plan
     */
    public CollectionPlan generatePlanForRequests(
            Long depotId,
            List<Long> requestIds,
            LocalDate plannedDate
    ) {
        log.info("Generating plan for specific requests: depotId={}, requests={}, date={}",
                depotId, requestIds.size(), plannedDate);

        // Get depot
        Depot depot = depotRepository.findById(depotId)
                .orElseThrow(() -> new IllegalArgumentException("Depot not found: " + depotId));

        // Get specified requests
        List<CollectionRequest> requests = requestIds.stream()
                .map(id -> collectionRequestRepository.findById(id)
                        .orElseThrow(() -> new IllegalArgumentException("Request not found: " + id)))
                .collect(Collectors.toList());

        // Verify all are approved
        boolean allApproved = requests.stream()
                .allMatch(req -> req.getStatus() == RequestStatus.APPROVED);

        if (!allApproved) {
            throw new IllegalStateException("All requests must be approved");
        }

        // Get vehicle type
        VehicleType vehicleType = vehicleTypeRepository
                .findByPoolOperatorIdAndActive(depot.getPoolOperatorId(), true)
                .stream()
                .max((v1, v2) -> Integer.compare(
                        v1.getCapacity().pallets() + v1.getCapacity().separators(),
                        v2.getCapacity().pallets() + v2.getCapacity().separators()
                ))
                .orElseThrow(() -> new IllegalStateException("No active vehicle types found"));

        // Convert and optimize
        List<CVRPOptimizer.CollectionNode> nodes = convertRequestsToNodes(requests);
        CVRPOptimizer.RouteSolution solution = cvrpOptimizer.optimizeRoute(
                depot.getLocation(),
                nodes,
                vehicleType.getCapacity()
        );

        if (solution.route().isEmpty()) {
            throw new IllegalStateException("Optimization failed: no feasible route found");
        }

        // Create plan
        String routeStopsJson = convertSolutionToJson(solution);
        int estimatedDurationMinutes = distanceCalculator.estimateDuration(solution.totalDistance());

        CollectionPlan plan = collectionPlanService.generatePlan(
                depotId,
                solution.totalDistance(),
                estimatedDurationMinutes,
                solution.totalLoad().pallets(),
                solution.totalLoad().separators(),
                plannedDate,
                routeStopsJson
        );

        // Mark all used requests as SCHEDULED
        for (CollectionRequest request : requests) {
            request.schedule(plan.getId());
            collectionRequestRepository.save(request);
        }

        log.info("✅ Custom plan created: planId={}, requests scheduled={}", plan.getId(), requests.size());

        return plan;
    }

    /**
     * Convert collection requests to CVRP nodes
     */
    private List<CVRPOptimizer.CollectionNode> convertRequestsToNodes(
            List<CollectionRequest> requests
    ) {
        List<CVRPOptimizer.CollectionNode> nodes = new ArrayList<>();

        for (CollectionRequest request : requests) {
            Filler filler = fillerRepository.findById(request.getFillerId())
                    .orElseThrow(() -> new IllegalArgumentException("Filler not found: " + request.getFillerId()));

            // Determine demand based on asset type
            Capacity demand = switch (request.getAssetType()) {
                case PALLET -> new Capacity(request.getEstimatedQuantity(), 0);
                case SEPARATOR -> new Capacity(0, request.getEstimatedQuantity());
            };

            nodes.add(new CVRPOptimizer.CollectionNode(
                    filler.getId(),
                    filler.getLocation(),
                    demand
            ));
        }

        return nodes;
    }

    /**
     * Convert optimization solution to JSON format
     */
    private String convertSolutionToJson(CVRPOptimizer.RouteSolution solution) {
        List<Map<String, Object>> stops = new ArrayList<>();

        for (int i = 0; i < solution.route().size(); i++) {
            CVRPOptimizer.CollectionNode node = solution.route().get(i);
            Map<String, Object> stop = new HashMap<>();
            stop.put("fillerId", node.fillerId());
            stop.put("sequence", i + 1);
            stop.put("pallets", node.demand().pallets());
            stop.put("separators", node.demand().separators());
            stop.put("latitude", node.location().latitude());
            stop.put("longitude", node.location().longitude());
            stops.add(stop);
        }

        try {
            return objectMapper.writeValueAsString(stops);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize route stops", e);
            return "[]";
        }
    }

    /**
     * Generate optimized multi-vehicle collection plans.
     * Creates multiple CollectionPlans for large number of fillers.
     *
     * @param depotId Depot to start from
     * @param plannedDate Date for collection
     * @param maxVehicles Maximum number of vehicles to use
     * @return List of created collection plans (one per vehicle)
     */
    public List<CollectionPlan> generateMultiVehiclePlan(
            Long depotId,
            LocalDate plannedDate,
            int maxVehicles
    ) {
        log.info("Generating multi-vehicle collection plan: depotId={}, date={}, maxVehicles={}",
                depotId, plannedDate, maxVehicles);

        // 1. Get depot
        Depot depot = depotRepository.findById(depotId)
                .orElseThrow(() -> new IllegalArgumentException("Depot not found: " + depotId));

        // 2. Get approved collection requests
        List<CollectionRequest> approvedRequests = collectionRequestRepository
                .findByStatus(RequestStatus.APPROVED);

        if (approvedRequests.isEmpty()) {
            throw new IllegalStateException("No approved collection requests found");
        }

        log.info("Found {} approved requests for multi-vehicle optimization", approvedRequests.size());

        // 3. Get vehicle type
        VehicleType vehicleType = vehicleTypeRepository
                .findByPoolOperatorIdAndActive(depot.getPoolOperatorId(), true)
                .stream()
                .max((v1, v2) -> Integer.compare(
                        v1.getCapacity().pallets() + v1.getCapacity().separators(),
                        v2.getCapacity().pallets() + v2.getCapacity().separators()
                ))
                .orElseThrow(() -> new IllegalStateException("No active vehicle types found"));

        // 4. Convert requests to nodes
        List<CVRPOptimizer.CollectionNode> nodes = convertRequestsToNodes(approvedRequests);

        // 5. Run multi-vehicle optimization
        CVRPOptimizer.MultiVehicleSolution solution = cvrpOptimizer.optimizeMultiVehicleRoutes(
                depot.getLocation(),
                nodes,
                vehicleType.getCapacity(),
                maxVehicles
        );

        if (solution.vehicleRoutes().isEmpty()) {
            throw new IllegalStateException("Multi-vehicle optimization failed: no feasible routes found");
        }

        // 6. Create CollectionPlan for each vehicle route
        List<CollectionPlan> plans = new ArrayList<>();
        for (int i = 0; i < solution.vehicleRoutes().size(); i++) {
            CVRPOptimizer.RouteSolution route = solution.vehicleRoutes().get(i);

            String routeStopsJson = convertSolutionToJson(route);
            int estimatedDurationMinutes = distanceCalculator.estimateDuration(route.totalDistance());

            CollectionPlan plan = collectionPlanService.generatePlan(
                    depotId,
                    route.totalDistance(),
                    estimatedDurationMinutes,
                    route.totalLoad().pallets(),
                    route.totalLoad().separators(),
                    plannedDate,
                    routeStopsJson
            );

            // Mark requests for fillers in this route as SCHEDULED
            List<Long> fillerIdsInRoute = route.route().stream()
                    .map(CVRPOptimizer.CollectionNode::fillerId)
                    .toList();

            List<CollectionRequest> requestsForThisRoute = approvedRequests.stream()
                    .filter(req -> fillerIdsInRoute.contains(req.getFillerId()))
                    .filter(req -> req.getStatus() == RequestStatus.APPROVED) // Only schedule if still APPROVED
                    .toList();

            for (CollectionRequest request : requestsForThisRoute) {
                request.schedule(plan.getId());
                collectionRequestRepository.save(request);
            }

            plans.add(plan);

            log.info("Created plan {}/{}: planId={}, stops={}, distance={} km, duration={} min, requests scheduled={}",
                    i + 1,
                    solution.vehicleRoutes().size(),
                    plan.getId(),
                    route.route().size(),
                    String.format("%.2f", route.totalDistance()),
                    estimatedDurationMinutes,
                    requestsForThisRoute.size());
        }

        log.info("✅ Multi-vehicle optimization completed: {} plans created, {} vehicles used, total {} km",
                plans.size(),
                solution.totalVehiclesUsed(),
                String.format("%.2f", solution.totalDistance()));

        if (solution.unassignedRequests() > 0) {
            log.warn("⚠️ {} requests could not be assigned (exceeds vehicle/capacity limits)",
                    solution.unassignedRequests());
        }

        return plans;
    }
}
