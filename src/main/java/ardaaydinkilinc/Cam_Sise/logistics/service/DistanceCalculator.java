package ardaaydinkilinc.Cam_Sise.logistics.service;

import ardaaydinkilinc.Cam_Sise.shared.domain.vo.GeoCoordinates;
import org.springframework.stereotype.Service;

/**
 * Distance calculator using Haversine formula.
 * Calculates great-circle distance between two points on Earth.
 */
@Service
public class DistanceCalculator {

    private static final double EARTH_RADIUS_KM = 6371.0;

    /**
     * Calculate distance between two geographic coordinates using Haversine formula.
     *
     * @param from Starting coordinates
     * @param to Ending coordinates
     * @return Distance in kilometers
     */
    public double calculateDistance(GeoCoordinates from, GeoCoordinates to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("Coordinates cannot be null");
        }

        double lat1Rad = Math.toRadians(from.latitude());
        double lat2Rad = Math.toRadians(to.latitude());
        double deltaLatRad = Math.toRadians(to.latitude() - from.latitude());
        double deltaLonRad = Math.toRadians(to.longitude() - from.longitude());

        // Haversine formula
        double a = Math.sin(deltaLatRad / 2) * Math.sin(deltaLatRad / 2) +
                   Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                   Math.sin(deltaLonRad / 2) * Math.sin(deltaLonRad / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c;
    }

    /**
     * Calculate total distance for a sequence of coordinates.
     *
     * @param coordinates List of coordinates in order
     * @return Total distance in kilometers
     */
    public double calculateTotalDistance(java.util.List<GeoCoordinates> coordinates) {
        if (coordinates == null || coordinates.size() < 2) {
            return 0.0;
        }

        double totalDistance = 0.0;
        for (int i = 0; i < coordinates.size() - 1; i++) {
            totalDistance += calculateDistance(coordinates.get(i), coordinates.get(i + 1));
        }

        return totalDistance;
    }

    /**
     * Estimate duration based on distance.
     * Assumes average speed of 50 km/h.
     *
     * @param distanceKm Distance in kilometers
     * @return Estimated duration in minutes
     */
    public int estimateDuration(double distanceKm) {
        final double AVERAGE_SPEED_KMH = 50.0;
        double hours = distanceKm / AVERAGE_SPEED_KMH;
        return (int) Math.ceil(hours * 60);
    }
}
