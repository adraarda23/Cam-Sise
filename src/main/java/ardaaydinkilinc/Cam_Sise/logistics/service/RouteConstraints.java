package ardaaydinkilinc.Cam_Sise.logistics.service;

import lombok.Getter;
import org.springframework.stereotype.Component;

/**
 * Route optimization constraints for realistic planning.
 * These constraints ensure routes are physically feasible for daily operations.
 */
@Component
@Getter
public class RouteConstraints {

    /**
     * Maximum route distance in kilometers.
     * Based on realistic daily driving limits (8-10 hours @ 80 km/h avg speed).
     */
    private final double maxRouteDistanceKm = 800.0;

    /**
     * Maximum route duration in minutes (10 hours).
     * Includes driving time + service time at each stop.
     */
    private final int maxRouteDurationMinutes = 600;

    /**
     * Service time per stop in minutes.
     * Time required for loading/unloading at each filler.
     */
    private final int serviceTimePerStopMinutes = 30;

    /**
     * Average vehicle speed in km/h for duration estimation.
     */
    private final double averageSpeedKmh = 50.0;

    /**
     * Check if route distance is within acceptable limits.
     */
    public boolean isDistanceAcceptable(double distanceKm) {
        return distanceKm <= maxRouteDistanceKm;
    }

    /**
     * Check if route duration is within acceptable limits.
     */
    public boolean isDurationAcceptable(int durationMinutes) {
        return durationMinutes <= maxRouteDurationMinutes;
    }

    /**
     * Calculate total route duration including service time.
     */
    public int calculateTotalDuration(double distanceKm, int numberOfStops) {
        int drivingTime = (int) Math.ceil((distanceKm / averageSpeedKmh) * 60);
        int serviceTime = numberOfStops * serviceTimePerStopMinutes;
        return drivingTime + serviceTime;
    }

    /**
     * Check if adding a new stop would violate constraints.
     */
    public boolean canAddStop(
            double currentDistance,
            int currentStops,
            double additionalDistance
    ) {
        double newDistance = currentDistance + additionalDistance;
        int newDuration = calculateTotalDuration(newDistance, currentStops + 1);

        return isDistanceAcceptable(newDistance) && isDurationAcceptable(newDuration);
    }
}
