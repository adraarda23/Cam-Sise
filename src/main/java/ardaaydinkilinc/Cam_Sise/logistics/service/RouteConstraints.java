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
    private final double maxRouteDistanceKm = 1500.0;

    /**
     * Maximum route duration in minutes.
     * Set to cover the worst-case 1500 km route at 50 km/h with up to 15 stops:
     * 1500/50*60 = 1800 min driving + 15*30 = 450 min service = 2250 → 2400 for margin.
     */
    private final int maxRouteDurationMinutes = 2400;

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
