package ardaaydinkilinc.Cam_Sise.logistics.service;

import ardaaydinkilinc.Cam_Sise.core.domain.Filler;
import ardaaydinkilinc.Cam_Sise.core.repository.FillerRepository;
import ardaaydinkilinc.Cam_Sise.inventory.domain.vo.AssetType;
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
    private final RouteConstraints routeConstraints;
    private final CollectionPlanService collectionPlanService;
    private final CollectionRequestRepository collectionRequestRepository;
    private final DepotRepository depotRepository;
    private final FillerRepository fillerRepository;
    private final VehicleTypeRepository vehicleTypeRepository;
    private final ObjectMapper objectMapper;

    /**
     * Generate optimized collection plan(s) for approved requests.
     * Automatically uses multiple vehicles when total demand exceeds single vehicle capacity.
     *
     * @param depotId    Depot to start from
     * @param plannedDate Date for collection
     * @return List of created collection plans (1 per vehicle used)
     */
    public List<CollectionPlan> generateOptimizedPlan(Long depotId, LocalDate plannedDate) {
        log.info("Generating auto-optimized collection plan: depotId={}, date={}", depotId, plannedDate);

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

        // 3. Get vehicle type with largest capacity
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

        // 4. Convert requests to CVRP nodes (merged per filler)
        List<CVRPOptimizer.CollectionNode> nodes = convertRequestsToNodes(approvedRequests);

        // 5. Calculate total demand and determine minimum vehicles needed
        Capacity totalDemand = nodes.stream()
                .map(CVRPOptimizer.CollectionNode::demand)
                .reduce(new Capacity(0, 0), Capacity::add);

        int vehiclesNeeded = calculateNeededVehicles(totalDemand, vehicleType.getCapacity());
        log.info("Total demand: {}, vehicles needed: {}", totalDemand.formatted(), vehiclesNeeded);

        if (vehiclesNeeded <= 1) {
            // Single vehicle trial — greedy nearest-neighbor + 2-opt
            CVRPOptimizer.RouteSolution solution = cvrpOptimizer.optimizeRoute(
                    depot.getLocation(), nodes, vehicleType.getCapacity());

            if (solution.route().isEmpty()) {
                throw new IllegalStateException("Optimization failed: no feasible route found");
            }

            // Check distance / duration constraints — if exceeded, escalate to multi-vehicle
            int trialDuration = routeConstraints.calculateTotalDuration(
                    solution.totalDistance(), solution.route().size());
            if (!routeConstraints.isDistanceAcceptable(solution.totalDistance()) ||
                    !routeConstraints.isDurationAcceptable(trialDuration)) {

                int byDistance = (int) Math.ceil(
                        solution.totalDistance() / routeConstraints.getMaxRouteDistanceKm());
                vehiclesNeeded = Math.max(vehiclesNeeded, byDistance);
                log.info("Single-vehicle route ({} km, {} min) exceeds constraints → escalating to {} vehicles",
                        String.format("%.1f", solution.totalDistance()), trialDuration, vehiclesNeeded);
                return buildMultiVehiclePlans(depotId, depot, approvedRequests, nodes, vehicleType, plannedDate, vehiclesNeeded);
            }

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

            for (CollectionRequest request : approvedRequests) {
                if (request.getStatus() == RequestStatus.APPROVED) {
                    request.schedule(plan.getId());
                    collectionRequestRepository.save(request);
                }
            }

            log.info("✅ Single-vehicle plan created: planId={}, stops={}, distance={} km, duration={} min",
                    plan.getId(), solution.route().size(),
                    String.format("%.2f", solution.totalDistance()), estimatedDurationMinutes);

            return List.of(plan);
        } else {
            // Multiple vehicles needed — Clarke-Wright savings
            log.info("Total demand exceeds single-vehicle capacity → using {} vehicles", vehiclesNeeded);
            return buildMultiVehiclePlans(depotId, depot, approvedRequests, nodes, vehicleType, plannedDate, vehiclesNeeded);
        }
    }

    /**
     * Calculate the minimum number of vehicles needed to carry the total demand.
     */
    private int calculateNeededVehicles(Capacity totalDemand, Capacity vehicleCapacity) {
        // 0 capacity in a dimension means unconstrained — treat as 1 vehicle needed for that dimension
        int byPallets = vehicleCapacity.pallets() > 0
                ? (int) Math.ceil((double) totalDemand.pallets() / vehicleCapacity.pallets()) : 1;
        int bySeparators = vehicleCapacity.separators() > 0
                ? (int) Math.ceil((double) totalDemand.separators() / vehicleCapacity.separators()) : 1;
        return Math.max(1, Math.max(byPallets, bySeparators));
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
        // Group requests by fillerId to merge multiple asset types from same filler
        Map<Long, List<CollectionRequest>> requestsByFiller = requests.stream()
                .collect(Collectors.groupingBy(CollectionRequest::getFillerId));

        List<CVRPOptimizer.CollectionNode> nodes = new ArrayList<>();
        List<String> invalidLocationFillers = new ArrayList<>();

        // Create ONE node per filler with combined demand (pallets + separators)
        for (Map.Entry<Long, List<CollectionRequest>> entry : requestsByFiller.entrySet()) {
            Long fillerId = entry.getKey();
            List<CollectionRequest> fillerRequests = entry.getValue();

            Filler filler = fillerRepository.findById(fillerId)
                    .orElseThrow(() -> new IllegalArgumentException("Filler not found: " + fillerId));

            // Validate location — null or (0,0) means the filler was never geocoded
            var loc = filler.getLocation();
            if (loc == null || (loc.latitude() == 0.0 && loc.longitude() == 0.0)) {
                log.warn("Filler '{}' (ID: {}) has no valid location — skipping from route optimization", filler.getName(), fillerId);
                invalidLocationFillers.add(filler.getName() + " (ID: " + fillerId + ")");
                continue;
            }

            // Sum all pallets and separators for this filler
            int totalPallets = fillerRequests.stream()
                    .filter(r -> r.getAssetType() == AssetType.PALLET)
                    .mapToInt(CollectionRequest::getEstimatedQuantity)
                    .sum();

            int totalSeparators = fillerRequests.stream()
                    .filter(r -> r.getAssetType() == AssetType.SEPARATOR)
                    .mapToInt(CollectionRequest::getEstimatedQuantity)
                    .sum();

            Capacity combinedDemand = new Capacity(totalPallets, totalSeparators);

            nodes.add(new CVRPOptimizer.CollectionNode(
                    filler.getId(),
                    loc,
                    combinedDemand
            ));

            log.debug("Merged {} requests from filler {} into single node: {} pallets, {} separators",
                    fillerRequests.size(), fillerId, totalPallets, totalSeparators);
        }

        if (!invalidLocationFillers.isEmpty()) {
            if (nodes.isEmpty()) {
                throw new IllegalStateException(
                    "Rota optimizasyonu başlatılamadı: Aşağıdaki dolumcularda geçerli konum bilgisi bulunmuyor. " +
                    "Lütfen dolumcu kayıtlarını güncelleyin: " + String.join(", ", invalidLocationFillers)
                );
            }
            log.warn("Geçersiz konumlu {} dolumcu optimizasyon dışı bırakıldı: {}", invalidLocationFillers.size(), invalidLocationFillers);
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
     * maxVehicles is treated as an upper-bound; if total demand requires more vehicles,
     * the minimum needed count takes priority so no requests are silently dropped.
     *
     * @param depotId     Depot to start from
     * @param plannedDate Date for collection
     * @param maxVehicles Preferred maximum number of vehicles (overridden if capacity demands more)
     * @return List of created collection plans (one per vehicle used)
     */
    public List<CollectionPlan> generateMultiVehiclePlan(
            Long depotId,
            LocalDate plannedDate,
            int maxVehicles
    ) {
        log.info("Generating multi-vehicle collection plan: depotId={}, date={}, maxVehicles={}",
                depotId, plannedDate, maxVehicles);

        Depot depot = depotRepository.findById(depotId)
                .orElseThrow(() -> new IllegalArgumentException("Depot not found: " + depotId));

        List<CollectionRequest> approvedRequests = collectionRequestRepository
                .findByStatus(RequestStatus.APPROVED);

        if (approvedRequests.isEmpty()) {
            throw new IllegalStateException("No approved collection requests found");
        }

        log.info("Found {} approved requests for multi-vehicle optimization", approvedRequests.size());

        VehicleType vehicleType = vehicleTypeRepository
                .findByPoolOperatorIdAndActive(depot.getPoolOperatorId(), true)
                .stream()
                .max((v1, v2) -> Integer.compare(
                        v1.getCapacity().pallets() + v1.getCapacity().separators(),
                        v2.getCapacity().pallets() + v2.getCapacity().separators()
                ))
                .orElseThrow(() -> new IllegalStateException("No active vehicle types found"));

        List<CVRPOptimizer.CollectionNode> nodes = convertRequestsToNodes(approvedRequests);

        // Ensure we use at least as many vehicles as capacity demands
        Capacity totalDemand = nodes.stream()
                .map(CVRPOptimizer.CollectionNode::demand)
                .reduce(new Capacity(0, 0), Capacity::add);

        int minNeeded = calculateNeededVehicles(totalDemand, vehicleType.getCapacity());
        // Cap at number of nodes (can't have more vehicles than stops), take the larger of
        // what the user requested and what capacity requires
        int vehiclesToUse = Math.max(minNeeded, Math.min(maxVehicles, nodes.size()));

        if (vehiclesToUse != maxVehicles) {
            log.info("Adjusted vehicle count from {} to {} (min needed by capacity: {})",
                    maxVehicles, vehiclesToUse, minNeeded);
        }

        return buildMultiVehiclePlans(depotId, depot, approvedRequests, nodes, vehicleType, plannedDate, vehiclesToUse);
    }

    /**
     * Internal: run Clarke-Wright optimization and persist one CollectionPlan per route.
     */
    private List<CollectionPlan> buildMultiVehiclePlans(
            Long depotId,
            Depot depot,
            List<CollectionRequest> approvedRequests,
            List<CVRPOptimizer.CollectionNode> nodes,
            VehicleType vehicleType,
            LocalDate plannedDate,
            int vehiclesNeeded
    ) {
        CVRPOptimizer.MultiVehicleSolution solution = cvrpOptimizer.optimizeMultiVehicleRoutes(
                depot.getLocation(),
                nodes,
                vehicleType.getCapacity(),
                vehiclesNeeded
        );

        if (solution.vehicleRoutes().isEmpty()) {
            throw new IllegalStateException("Multi-vehicle optimization failed: no feasible routes found");
        }

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

            List<Long> fillerIdsInRoute = route.route().stream()
                    .map(CVRPOptimizer.CollectionNode::fillerId)
                    .toList();

            List<CollectionRequest> requestsForThisRoute = approvedRequests.stream()
                    .filter(req -> fillerIdsInRoute.contains(req.getFillerId()))
                    .filter(req -> req.getStatus() == RequestStatus.APPROVED)
                    .toList();

            for (CollectionRequest request : requestsForThisRoute) {
                request.schedule(plan.getId());
                collectionRequestRepository.save(request);
            }

            plans.add(plan);

            log.info("Created plan {}/{}: planId={}, stops={}, distance={} km, duration={} min, requests scheduled={}",
                    i + 1, solution.vehicleRoutes().size(), plan.getId(), route.route().size(),
                    String.format("%.2f", route.totalDistance()), estimatedDurationMinutes,
                    requestsForThisRoute.size());
        }

        log.info("✅ Multi-vehicle optimization completed: {} plans created, {} vehicles used, total {} km",
                plans.size(), solution.totalVehiclesUsed(),
                String.format("%.2f", solution.totalDistance()));

        if (solution.unassignedRequests() > 0) {
            log.warn("⚠️ {} requests could not be assigned (exceeds vehicle/capacity limits)",
                    solution.unassignedRequests());
        }

        return plans;
    }
}
