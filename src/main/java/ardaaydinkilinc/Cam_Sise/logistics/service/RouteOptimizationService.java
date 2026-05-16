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
import ardaaydinkilinc.Cam_Sise.logistics.repository.CollectionPlanRepository;
import ardaaydinkilinc.Cam_Sise.logistics.repository.CollectionRequestRepository;
import ardaaydinkilinc.Cam_Sise.logistics.repository.DepotRepository;
import ardaaydinkilinc.Cam_Sise.logistics.repository.VehicleTypeRepository;
import ardaaydinkilinc.Cam_Sise.logistics.service.routing.OsrmDistanceProvider;
import ardaaydinkilinc.Cam_Sise.logistics.service.routing.RouteSegment;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final CollectionPlanRepository collectionPlanRepository;
    private final CollectionRequestRepository collectionRequestRepository;
    private final DepotRepository depotRepository;
    private final FillerRepository fillerRepository;
    private final VehicleTypeRepository vehicleTypeRepository;
    private final ObjectMapper objectMapper;

    @Autowired(required = false)
    private OsrmDistanceProvider osrmProvider;

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

            persistRouteGeometry(plan, depot.getLocation(), solution.route());

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
        // Use exactly as many vehicles as capacity requires, capped at nodes.size().
        // maxVehicles is an upper-bound hint; minNeeded always takes priority.
        int vehiclesToUse = Math.min(nodes.size(), minNeeded);

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

            persistRouteGeometry(plan, depot.getLocation(), route.route());

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

    /**
     * Walk the route depot → stop1 → … → stopN and concatenate the geometry
     * returned by the active {@link DistanceCalculator}. When the provider does
     * not return geometry (Haversine default), this is effectively a no-op.
     *
     * <p>Dönüş bacağı (son durak → depo) BİLEREK çizilmez: OSRM gidiş/dönüşü
     * tek-yön cadde / otoyol yönü farkından farklı yollardan döndürebiliyor ve
     * harita üzerinde "iki çizgi ayrışıp tekrar birleşiyor" görünümü oluşuyordu.
     * Round-trip mesafesi zaten CVRP tarafında hesaplanıyor; bu yalnızca görsel.
     */
    private void persistRouteGeometry(CollectionPlan plan, GeoCoordinates depotLocation,
                                      List<CVRPOptimizer.CollectionNode> route) {
        if (route == null || route.isEmpty()) return;

        // Gidiş: depo → d1 → … → dN
        List<GeoCoordinates> outboundSeq = new ArrayList<>();
        outboundSeq.add(depotLocation);
        route.forEach(n -> outboundSeq.add(n.location()));

        // Dönüş: dN → depo (kendi en kısa yolu — retrace değil)
        List<GeoCoordinates> returnSeq = List.of(
                route.get(route.size() - 1).location(),
                depotLocation
        );

        List<double[]> outbound = fetchPolyline(outboundSeq);
        List<double[]> returnLeg = fetchPolyline(returnSeq);

        if (outbound == null && returnLeg == null) {
            // Hiç gerçek geometry yok — düz çizgi kaydetmenin anlamı yok.
            return;
        }

        try {
            // Yeni şema: {"outbound":[[lat,lng]...], "return":[[lat,lng]...]}
            // Eski düz-dizi şeması frontend'de geriye dönük desteklenir.
            java.util.Map<String, List<double[]>> geometry = new java.util.LinkedHashMap<>();
            geometry.put("outbound", outbound != null ? outbound : List.of());
            geometry.put("return", returnLeg != null ? returnLeg : List.of());
            plan.setRouteGeometry(objectMapper.writeValueAsString(geometry));
            collectionPlanRepository.save(plan);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize route geometry: {}", e.getMessage());
        }
    }

    /**
     * Verilen waypoint dizisi için yol-takipli polyline döndürür.
     * Önce tek OSRM multi-waypoint çağrısı (hızlı), olmazsa segment-segment fallback.
     * Hiç gerçek geometry üretilemezse {@code null} döner.
     */
    private List<double[]> fetchPolyline(List<GeoCoordinates> sequence) {
        if (sequence == null || sequence.size() < 2) return null;

        if (osrmProvider != null) {
            try {
                List<GeoCoordinates> geometry = osrmProvider.routeGeometry(sequence);
                if (geometry != null && !geometry.isEmpty()) {
                    List<double[]> polyline = new ArrayList<>(geometry.size());
                    for (GeoCoordinates c : geometry) {
                        polyline.add(new double[]{c.latitude(), c.longitude()});
                    }
                    return polyline;
                }
            } catch (Exception e) {
                log.warn("Multi-waypoint OSRM failed: {}", e.getMessage());
            }
        }

        List<double[]> polyline = new ArrayList<>();
        boolean anyGeometry = false;
        for (int i = 0; i < sequence.size() - 1; i++) {
            try {
                RouteSegment seg = distanceCalculator.routeSegment(sequence.get(i), sequence.get(i + 1));
                if (seg.hasGeometry()) {
                    anyGeometry = true;
                    for (GeoCoordinates c : seg.geometry()) {
                        polyline.add(new double[]{c.latitude(), c.longitude()});
                    }
                } else {
                    polyline.add(new double[]{sequence.get(i).latitude(), sequence.get(i).longitude()});
                }
            } catch (Exception e) {
                log.debug("Per-segment geometry fetch failed: {}", e.getMessage());
                polyline.add(new double[]{sequence.get(i).latitude(), sequence.get(i).longitude()});
            }
        }
        GeoCoordinates last = sequence.get(sequence.size() - 1);
        polyline.add(new double[]{last.latitude(), last.longitude()});
        return anyGeometry ? polyline : null;
    }

    /**
     * Re-fetch road geometry for an existing plan. Useful when a plan was
     * created while OSRM was disabled and the frontend now wants real road
     * polylines.
     *
     * <p>Reads the persisted route stops, asks the active DistanceProvider for
     * geometry between consecutive stops, and saves the result back on the plan.
     * Returns the refreshed plan.
     */
    public CollectionPlan refreshRouteGeometry(Long planId) {
        CollectionPlan plan = collectionPlanRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("Plan not found: " + planId));

        Depot depot = depotRepository.findById(plan.getDepotId())
                .orElseThrow(() -> new IllegalArgumentException("Depot not found: " + plan.getDepotId()));

        List<CVRPOptimizer.CollectionNode> route = parseStopsAsNodes(plan.getRouteStopsJson());
        if (route.isEmpty()) {
            log.warn("Plan {} has no parseable route stops — geometry refresh skipped", planId);
            return plan;
        }

        persistRouteGeometry(plan, depot.getLocation(), route);
        return collectionPlanRepository.findById(planId).orElse(plan);
    }

    private List<CVRPOptimizer.CollectionNode> parseStopsAsNodes(String routeStopsJson) {
        List<CVRPOptimizer.CollectionNode> nodes = new ArrayList<>();
        if (routeStopsJson == null || routeStopsJson.isBlank()) return nodes;
        try {
            List<Map<String, Object>> stops = objectMapper.readValue(
                    routeStopsJson,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));
            for (Map<String, Object> stop : stops) {
                Number lat = (Number) stop.get("latitude");
                Number lon = (Number) stop.get("longitude");
                Number fillerId = (Number) stop.get("fillerId");
                if (lat == null || lon == null) continue;
                nodes.add(new CVRPOptimizer.CollectionNode(
                        fillerId != null ? fillerId.longValue() : null,
                        new GeoCoordinates(lat.doubleValue(), lon.doubleValue()),
                        new Capacity(0, 0)
                ));
            }
        } catch (Exception e) {
            log.warn("Failed to parse routeStopsJson: {}", e.getMessage());
        }
        return nodes;
    }
}
