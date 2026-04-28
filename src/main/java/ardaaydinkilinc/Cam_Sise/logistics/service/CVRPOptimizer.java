package ardaaydinkilinc.Cam_Sise.logistics.service;

import ardaaydinkilinc.Cam_Sise.logistics.domain.vo.Capacity;
import ardaaydinkilinc.Cam_Sise.shared.domain.vo.GeoCoordinates;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Capacitated Vehicle Routing Problem (CVRP) Optimizer.
 * Uses Greedy Nearest Neighbor algorithm with 2-opt improvement.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CVRPOptimizer {

    private final DistanceCalculator distanceCalculator;
    private final RouteConstraints routeConstraints;

    /**
     * Collection request node for routing
     */
    public record CollectionNode(
            Long fillerId,
            GeoCoordinates location,
            Capacity demand
    ) {}

    /**
     * Optimized route solution
     */
    public record RouteSolution(
            List<CollectionNode> route,
            double totalDistance,
            Capacity totalLoad
    ) {}

    /**
     * Optimize collection routes using CVRP.
     *
     * @param depotLocation Starting depot location
     * @param requests Collection requests to optimize
     * @param vehicleCapacity Maximum vehicle capacity
     * @return Optimized route solution
     */
    public RouteSolution optimizeRoute(
            GeoCoordinates depotLocation,
            List<CollectionNode> requests,
            Capacity vehicleCapacity
    ) {
        if (requests == null || requests.isEmpty()) {
            log.warn("No requests to optimize");
            return new RouteSolution(Collections.emptyList(), 0.0, new Capacity(0, 0));
        }

        log.info("Starting CVRP optimization: requests={}, vehicleCapacity={}",
                requests.size(), vehicleCapacity.formatted());

        // Filter requests that fit within vehicle capacity (0 in a dimension = unconstrained)
        List<CollectionNode> feasibleRequests = requests.stream()
                .filter(req -> vehicleCapacity.canRouteWith(req.demand()))
                .collect(Collectors.toList());

        if (feasibleRequests.isEmpty()) {
            log.warn("No feasible requests within vehicle capacity: capacity={}", vehicleCapacity.formatted());
            return new RouteSolution(Collections.emptyList(), 0.0, new Capacity(0, 0));
        }

        // Build distance matrix
        double[][] distanceMatrix = buildDistanceMatrix(depotLocation, feasibleRequests);

        // Apply greedy nearest neighbor heuristic
        List<CollectionNode> initialRoute = greedyNearestNeighbor(
                depotLocation,
                feasibleRequests,
                vehicleCapacity,
                distanceMatrix
        );

        // Improve with 2-opt
        List<CollectionNode> optimizedRoute = twoOptImprovement(
                depotLocation,
                initialRoute,
                distanceMatrix
        );

        // Calculate total distance and load
        double totalDistance = calculateRouteDistance(depotLocation, optimizedRoute);
        Capacity totalLoad = optimizedRoute.stream()
                .map(CollectionNode::demand)
                .reduce(new Capacity(0, 0), Capacity::add);

        log.info("CVRP optimization completed: stops={}, totalDistance={} km, totalLoad={}",
                optimizedRoute.size(), String.format("%.2f", totalDistance), totalLoad.formatted());

        return new RouteSolution(optimizedRoute, totalDistance, totalLoad);
    }

    /**
     * Build distance matrix between depot and all requests
     */
    private double[][] buildDistanceMatrix(
            GeoCoordinates depot,
            List<CollectionNode> requests
    ) {
        int n = requests.size();
        double[][] matrix = new double[n + 1][n + 1]; // +1 for depot

        // Depot is index 0
        for (int i = 0; i < n; i++) {
            matrix[0][i + 1] = distanceCalculator.calculateDistance(depot, requests.get(i).location());
            matrix[i + 1][0] = matrix[0][i + 1];
        }

        // Distances between requests
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                double dist = distanceCalculator.calculateDistance(
                        requests.get(i).location(),
                        requests.get(j).location()
                );
                matrix[i + 1][j + 1] = dist;
                matrix[j + 1][i + 1] = dist;
            }
        }

        return matrix;
    }

    /**
     * Greedy Nearest Neighbor heuristic for initial solution
     */
    private List<CollectionNode> greedyNearestNeighbor(
            GeoCoordinates depot,
            List<CollectionNode> requests,
            Capacity vehicleCapacity,
            double[][] distanceMatrix
    ) {
        List<CollectionNode> route = new ArrayList<>();
        Set<Integer> visited = new HashSet<>();
        Capacity currentLoad = new Capacity(0, 0);

        int currentIndex = 0; // Start from depot

        while (visited.size() < requests.size()) {
            int nearestIndex = -1;
            double minDistance = Double.MAX_VALUE;

            // Find nearest unvisited node that fits capacity
            for (int i = 0; i < requests.size(); i++) {
                if (!visited.contains(i)) {
                    CollectionNode node = requests.get(i);
                    Capacity potentialLoad = currentLoad.add(node.demand());

                    if (vehicleCapacity.canRouteWith(potentialLoad)) {
                        double distance = distanceMatrix[currentIndex][i + 1];
                        if (distance < minDistance) {
                            minDistance = distance;
                            nearestIndex = i;
                        }
                    }
                }
            }

            // If no feasible node found, stop (vehicle capacity reached)
            if (nearestIndex == -1) {
                break;
            }

            // Add node to route
            CollectionNode selectedNode = requests.get(nearestIndex);
            route.add(selectedNode);
            visited.add(nearestIndex);
            currentLoad = currentLoad.add(selectedNode.demand());
            currentIndex = nearestIndex + 1;
        }

        return route;
    }

    /**
     * 2-opt local search improvement
     */
    private List<CollectionNode> twoOptImprovement(
            GeoCoordinates depot,
            List<CollectionNode> route,
            double[][] distanceMatrix
    ) {
        if (route.size() < 4) {
            return route; // Too small for 2-opt
        }

        List<CollectionNode> bestRoute = new ArrayList<>(route);
        double bestDistance = calculateRouteDistance(depot, bestRoute);
        boolean improved = true;

        int maxIterations = 100;
        int iteration = 0;

        while (improved && iteration < maxIterations) {
            improved = false;
            iteration++;

            for (int i = 1; i < route.size() - 1; i++) {
                for (int j = i + 1; j < route.size(); j++) {
                    // Try reversing segment [i, j]
                    List<CollectionNode> newRoute = twoOptSwap(bestRoute, i, j);
                    double newDistance = calculateRouteDistance(depot, newRoute);

                    if (newDistance < bestDistance) {
                        bestRoute = newRoute;
                        bestDistance = newDistance;
                        improved = true;
                    }
                }
            }
        }

        log.debug("2-opt completed after {} iterations", iteration);
        return bestRoute;
    }

    /**
     * Perform 2-opt swap
     */
    private List<CollectionNode> twoOptSwap(
            List<CollectionNode> route,
            int i,
            int j
    ) {
        List<CollectionNode> newRoute = new ArrayList<>();

        // Add nodes before i
        newRoute.addAll(route.subList(0, i));

        // Reverse segment [i, j]
        List<CollectionNode> reversed = new ArrayList<>(route.subList(i, j + 1));
        Collections.reverse(reversed);
        newRoute.addAll(reversed);

        // Add nodes after j
        if (j + 1 < route.size()) {
            newRoute.addAll(route.subList(j + 1, route.size()));
        }

        return newRoute;
    }

    /**
     * Calculate total route distance including depot returns
     */
    private double calculateRouteDistance(
            GeoCoordinates depot,
            List<CollectionNode> route
    ) {
        if (route.isEmpty()) {
            return 0.0;
        }

        double total = 0.0;

        // Depot to first stop
        total += distanceCalculator.calculateDistance(depot, route.get(0).location());

        // Between stops
        for (int i = 0; i < route.size() - 1; i++) {
            total += distanceCalculator.calculateDistance(
                    route.get(i).location(),
                    route.get(i + 1).location()
            );
        }

        // Last stop back to depot
        total += distanceCalculator.calculateDistance(
                route.get(route.size() - 1).location(),
                depot
        );

        return total;
    }

    // ========== MULTI-VEHICLE CVRP ==========

    /**
     * Multi-vehicle route solution
     */
    public record MultiVehicleSolution(
            List<RouteSolution> vehicleRoutes,
            int totalVehiclesUsed,
            double totalDistance,
            Capacity totalLoad,
            int unassignedRequests
    ) {}

    /**
     * Optimize routes for multiple vehicles using Clarke-Wright Savings Algorithm.
     * Handles large number of fillers by creating multiple realistic routes.
     *
     * @param depotLocation Starting depot
     * @param requests All collection requests
     * @param vehicleCapacity Capacity per vehicle
     * @param maxVehicles Maximum number of vehicles available
     * @return Multi-vehicle solution
     */
    public MultiVehicleSolution optimizeMultiVehicleRoutes(
            GeoCoordinates depotLocation,
            List<CollectionNode> requests,
            Capacity vehicleCapacity,
            int maxVehicles
    ) {
        log.info("Starting multi-vehicle CVRP: requests={}, vehicles={}, capacity={}",
                requests.size(), maxVehicles, vehicleCapacity.formatted());

        if (requests.isEmpty()) {
            return new MultiVehicleSolution(Collections.emptyList(), 0, 0.0, new Capacity(0, 0), 0);
        }

        // Build distance matrix
        double[][] distanceMatrix = buildDistanceMatrix(depotLocation, requests);

        // Clarke-Wright Savings Algorithm
        List<RouteSolution> vehicleRoutes = clarkeWrightSavings(
                depotLocation,
                requests,
                vehicleCapacity,
                maxVehicles,
                distanceMatrix
        );

        // Calculate totals
        double totalDistance = vehicleRoutes.stream()
                .mapToDouble(RouteSolution::totalDistance)
                .sum();

        Capacity totalLoad = vehicleRoutes.stream()
                .map(RouteSolution::totalLoad)
                .reduce(new Capacity(0, 0), Capacity::add);

        int assignedRequests = vehicleRoutes.stream()
                .mapToInt(r -> r.route().size())
                .sum();

        int unassigned = requests.size() - assignedRequests;

        log.info("Multi-vehicle optimization completed: vehicles={}, distance={} km, coverage={}/{}, unassigned={}",
                vehicleRoutes.size(),
                String.format("%.2f", totalDistance),
                assignedRequests,
                requests.size(),
                unassigned);

        return new MultiVehicleSolution(
                vehicleRoutes,
                vehicleRoutes.size(),
                totalDistance,
                totalLoad,
                unassigned
        );
    }

    /**
     * Clarke-Wright Savings Algorithm for multi-vehicle routing.
     */
    private List<RouteSolution> clarkeWrightSavings(
            GeoCoordinates depot,
            List<CollectionNode> requests,
            Capacity vehicleCapacity,
            int maxVehicles,
            double[][] distanceMatrix
    ) {
        // Step 1: Create individual routes for each request (depot → i → depot)
        List<Route> routes = new ArrayList<>();
        for (int i = 0; i < requests.size(); i++) {
            Route route = new Route(depot);
            route.addNode(requests.get(i), distanceMatrix[0][i + 1] * 2); // Round trip
            routes.add(route);
        }

        // Step 2: Calculate savings for all pairs
        List<Saving> savings = calculateSavings(requests, distanceMatrix);

        // Step 3: Sort savings in descending order
        savings.sort((a, b) -> Double.compare(b.saving, a.saving));

        // Step 4: Merge routes based on savings — keep merging until we hit the vehicle target
        for (Saving saving : savings) {
            if (routes.size() <= maxVehicles) {
                break; // Already at or under the vehicle limit, stop merging
            }

            Route routeI = findRouteContaining(routes, requests.get(saving.i));
            Route routeJ = findRouteContaining(routes, requests.get(saving.j));

            if (routeI != null && routeJ != null && routeI != routeJ) {
                if (canMergeRoutes(routeI, routeJ, vehicleCapacity, saving.i, saving.j, requests, saving.saving)) {
                    Route merged = mergeRoutes(routeI, routeJ, saving.i, saving.j, requests, distanceMatrix, depot);
                    routes.remove(routeI);
                    routes.remove(routeJ);
                    routes.add(merged);
                }
            }
        }

        // Step 5: Convert to RouteSolution and apply 2-opt
        // Constraint violations are logged as warnings but do NOT discard the route —
        // the caller decides whether the route is acceptable for their operation.
        List<RouteSolution> solutions = new ArrayList<>();
        for (Route route : routes) {
            List<CollectionNode> optimizedRoute = twoOptImprovement(depot, route.nodes, distanceMatrix);
            double distance = calculateRouteDistance(depot, optimizedRoute);
            Capacity load = optimizedRoute.stream()
                    .map(CollectionNode::demand)
                    .reduce(new Capacity(0, 0), Capacity::add);

            int duration = routeConstraints.calculateTotalDuration(distance, optimizedRoute.size());
            if (!routeConstraints.isDistanceAcceptable(distance) || !routeConstraints.isDurationAcceptable(duration)) {
                log.warn("Route exceeds soft constraints (distance={} km, duration={} min) — included anyway",
                        String.format("%.2f", distance), duration);
            }
            solutions.add(new RouteSolution(optimizedRoute, distance, load));
        }

        return solutions;
    }

    /**
     * Calculate savings for all request pairs
     */
    private List<Saving> calculateSavings(List<CollectionNode> requests, double[][] distanceMatrix) {
        List<Saving> savings = new ArrayList<>();

        for (int i = 0; i < requests.size(); i++) {
            for (int j = i + 1; j < requests.size(); j++) {
                // Saving = dist(depot,i) + dist(depot,j) - dist(i,j)
                double saving = distanceMatrix[0][i + 1] +
                        distanceMatrix[0][j + 1] -
                        distanceMatrix[i + 1][j + 1];

                savings.add(new Saving(i, j, saving));
            }
        }

        return savings;
    }

    /**
     * Find route containing a specific node
     */
    private Route findRouteContaining(List<Route> routes, CollectionNode node) {
        return routes.stream()
                .filter(r -> r.contains(node))
                .findFirst()
                .orElse(null);
    }

    /**
     * Check if two routes can be merged
     */
    private boolean canMergeRoutes(
            Route r1,
            Route r2,
            Capacity vehicleCapacity,
            int i,
            int j,
            List<CollectionNode> requests,
            double savingAmount
    ) {
        // Check capacity (0 in a dimension = unconstrained)
        Capacity combinedLoad = r1.load.add(r2.load);
        if (!vehicleCapacity.canRouteWith(combinedLoad)) {
            return false;
        }

        // Check if i and j are at route ends
        CollectionNode nodeI = requests.get(i);
        CollectionNode nodeJ = requests.get(j);

        if (!r1.isAtEnd(nodeI) || !r2.isAtEnd(nodeJ)) {
            return false;
        }

        // Clarke-Wright: merged_distance = r1 + r2 - saving
        // (saving = d(depot,i) + d(depot,j) - d(i,j), so merged avoids two depot returns)
        double estimatedDistance = r1.distance + r2.distance - savingAmount;
        int estimatedStops = r1.nodes.size() + r2.nodes.size();
        int estimatedDuration = routeConstraints.calculateTotalDuration(estimatedDistance, estimatedStops);

        return routeConstraints.isDistanceAcceptable(estimatedDistance) &&
                routeConstraints.isDurationAcceptable(estimatedDuration);
    }

    /**
     * Merge two routes
     */
    private Route mergeRoutes(
            Route r1,
            Route r2,
            int i,
            int j,
            List<CollectionNode> requests,
            double[][] distanceMatrix,
            GeoCoordinates depot
    ) {
        Route merged = new Route(depot);
        CollectionNode nodeI = requests.get(i);
        CollectionNode nodeJ = requests.get(j);

        // Determine merge order
        List<CollectionNode> mergedNodes = new ArrayList<>();

        if (r1.nodes.get(r1.nodes.size() - 1).equals(nodeI) &&
                r2.nodes.get(0).equals(nodeJ)) {
            mergedNodes.addAll(r1.nodes);
            mergedNodes.addAll(r2.nodes);
        } else if (r1.nodes.get(0).equals(nodeI) &&
                r2.nodes.get(r2.nodes.size() - 1).equals(nodeJ)) {
            mergedNodes.addAll(r2.nodes);
            mergedNodes.addAll(r1.nodes);
        } else {
            // Reverse if needed
            mergedNodes.addAll(r1.nodes);
            mergedNodes.addAll(r2.nodes);
        }

        merged.nodes = mergedNodes;
        merged.load = r1.load.add(r2.load);
        merged.distance = calculateRouteDistance(depot, mergedNodes);

        return merged;
    }

    /**
     * Internal route representation
     */
    private static class Route {
        List<CollectionNode> nodes;
        Capacity load;
        double distance;
        GeoCoordinates depot;

        Route(GeoCoordinates depot) {
            this.depot = depot;
            this.nodes = new ArrayList<>();
            this.load = new Capacity(0, 0);
            this.distance = 0.0;
        }

        void addNode(CollectionNode node, double dist) {
            nodes.add(node);
            load = load.add(node.demand());
            distance += dist;
        }

        boolean contains(CollectionNode node) {
            return nodes.contains(node);
        }

        boolean isAtEnd(CollectionNode node) {
            return !nodes.isEmpty() &&
                    (nodes.get(0).equals(node) || nodes.get(nodes.size() - 1).equals(node));
        }
    }

    /**
     * Savings pair
     */
    private record Saving(int i, int j, double saving) {}
}
