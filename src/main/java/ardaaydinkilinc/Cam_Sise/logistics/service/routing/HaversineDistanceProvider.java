package ardaaydinkilinc.Cam_Sise.logistics.service.routing;

import ardaaydinkilinc.Cam_Sise.shared.domain.vo.GeoCoordinates;
import org.springframework.stereotype.Component;

/**
 * Great-circle distance via Haversine formula.
 *
 * <p>Always registered as a Spring bean under the name
 * {@code haversineDistanceProvider}. Whether it is the active primary
 * provider is decided by {@link DistanceProviderConfig} based on
 * {@code app.routing.distance-provider}.
 */
@Component("haversineDistanceProvider")
public class HaversineDistanceProvider implements DistanceProvider {

    private static final double EARTH_RADIUS_KM = 6371.0;
    private static final double AVERAGE_SPEED_KMH = 50.0;

    @Override
    public RouteSegment route(GeoCoordinates from, GeoCoordinates to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("Coordinates cannot be null");
        }
        double lat1Rad = Math.toRadians(from.latitude());
        double lat2Rad = Math.toRadians(to.latitude());
        double deltaLatRad = Math.toRadians(to.latitude() - from.latitude());
        double deltaLonRad = Math.toRadians(to.longitude() - from.longitude());

        double a = Math.sin(deltaLatRad / 2) * Math.sin(deltaLatRad / 2)
                + Math.cos(lat1Rad) * Math.cos(lat2Rad)
                * Math.sin(deltaLonRad / 2) * Math.sin(deltaLonRad / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distanceKm = EARTH_RADIUS_KM * c;
        double durationMinutes = (distanceKm / AVERAGE_SPEED_KMH) * 60.0;

        return RouteSegment.of(distanceKm, durationMinutes);
    }
}
