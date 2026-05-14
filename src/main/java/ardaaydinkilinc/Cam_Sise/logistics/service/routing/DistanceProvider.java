package ardaaydinkilinc.Cam_Sise.logistics.service.routing;

import ardaaydinkilinc.Cam_Sise.shared.domain.vo.GeoCoordinates;

import java.util.List;

/**
 * Strategy for computing road / great-circle distance between two coordinates.
 *
 * <p>Implementations:
 * <ul>
 *   <li>{@link HaversineDistanceProvider} — great-circle distance, no external dep.</li>
 *   <li>{@code OsrmDistanceProvider} — real road distance via OSRM HTTP API.</li>
 *   <li>{@code CachedDistanceProvider} — decorator that memoizes results in DB.</li>
 * </ul>
 *
 * <p>The Spring bean selection is driven by {@code app.routing.distance-provider}
 * (haversine | osrm). The {@code @Primary} bean is the one chosen at startup.
 */
public interface DistanceProvider {

    /**
     * Single-segment query.
     */
    RouteSegment route(GeoCoordinates from, GeoCoordinates to);

    /**
     * Sum of consecutive segment distances. Default implementation walks the
     * list; providers with batch APIs may override.
     */
    default double totalDistanceKm(List<GeoCoordinates> coordinates) {
        if (coordinates == null || coordinates.size() < 2) return 0.0;
        double total = 0.0;
        for (int i = 0; i < coordinates.size() - 1; i++) {
            total += route(coordinates.get(i), coordinates.get(i + 1)).distanceKm();
        }
        return total;
    }

    /**
     * Quick distance-only access. Equivalent to {@code route().distanceKm()}.
     */
    default double distanceKm(GeoCoordinates from, GeoCoordinates to) {
        return route(from, to).distanceKm();
    }
}
