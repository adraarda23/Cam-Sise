package ardaaydinkilinc.Cam_Sise.logistics.service.routing;

import ardaaydinkilinc.Cam_Sise.shared.domain.vo.GeoCoordinates;

import java.util.List;

/**
 * Result of a routing call between two coordinates.
 *
 * <p>{@code distanceKm} and {@code durationMinutes} are required. {@code geometry}
 * is optional and only populated by providers that return full road geometry
 * (e.g. OSRM). Haversine-only providers leave it null.
 */
public record RouteSegment(
        double distanceKm,
        double durationMinutes,
        List<GeoCoordinates> geometry
) {
    public static RouteSegment of(double distanceKm, double durationMinutes) {
        return new RouteSegment(distanceKm, durationMinutes, null);
    }

    public boolean hasGeometry() {
        return geometry != null && !geometry.isEmpty();
    }
}
