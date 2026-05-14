package ardaaydinkilinc.Cam_Sise.logistics.service;

import ardaaydinkilinc.Cam_Sise.logistics.service.routing.DistanceProvider;
import ardaaydinkilinc.Cam_Sise.logistics.service.routing.RouteSegment;
import ardaaydinkilinc.Cam_Sise.shared.domain.vo.GeoCoordinates;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Façade around {@link DistanceProvider} that preserves the original
 * public API used by CVRPOptimizer and tests. The actual computation is
 * delegated to whichever {@code @Primary} {@link DistanceProvider} bean is
 * active (haversine by default; OSRM when configured).
 *
 * <p>Backwards-compatible: existing callers continue to use this service
 * without changes. The no-arg constructor lets unit tests build an instance
 * without wiring a provider (falls back to inline Haversine).
 */
@Service
public class DistanceCalculator {

    private static final double EARTH_RADIUS_KM = 6371.0;
    private static final double AVERAGE_SPEED_KMH = 50.0;

    private final DistanceProvider provider;

    @Autowired
    public DistanceCalculator(DistanceProvider provider) {
        this.provider = provider;
    }

    /**
     * Convenience constructor used by unit tests; uses inline Haversine.
     */
    public DistanceCalculator() {
        this.provider = null;
    }

    public double calculateDistance(GeoCoordinates from, GeoCoordinates to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("Coordinates cannot be null");
        }
        if (provider != null) {
            return provider.distanceKm(from, to);
        }
        return haversine(from, to);
    }

    public double calculateTotalDistance(List<GeoCoordinates> coordinates) {
        if (coordinates == null || coordinates.size() < 2) return 0.0;
        if (provider != null) {
            return provider.totalDistanceKm(coordinates);
        }
        double total = 0.0;
        for (int i = 0; i < coordinates.size() - 1; i++) {
            total += haversine(coordinates.get(i), coordinates.get(i + 1));
        }
        return total;
    }

    public int estimateDuration(double distanceKm) {
        double hours = distanceKm / AVERAGE_SPEED_KMH;
        return (int) Math.ceil(hours * 60);
    }

    /**
     * Returns full {@link RouteSegment} when the provider supports it.
     */
    public RouteSegment routeSegment(GeoCoordinates from, GeoCoordinates to) {
        if (provider != null) {
            return provider.route(from, to);
        }
        double distance = haversine(from, to);
        double duration = (distance / AVERAGE_SPEED_KMH) * 60.0;
        return RouteSegment.of(distance, duration);
    }

    private double haversine(GeoCoordinates from, GeoCoordinates to) {
        double lat1Rad = Math.toRadians(from.latitude());
        double lat2Rad = Math.toRadians(to.latitude());
        double deltaLatRad = Math.toRadians(to.latitude() - from.latitude());
        double deltaLonRad = Math.toRadians(to.longitude() - from.longitude());

        double a = Math.sin(deltaLatRad / 2) * Math.sin(deltaLatRad / 2)
                + Math.cos(lat1Rad) * Math.cos(lat2Rad)
                * Math.sin(deltaLonRad / 2) * Math.sin(deltaLonRad / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }
}
