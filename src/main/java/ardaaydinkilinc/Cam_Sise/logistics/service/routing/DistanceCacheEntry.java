package ardaaydinkilinc.Cam_Sise.logistics.service.routing;

import ardaaydinkilinc.Cam_Sise.shared.domain.base.Entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Cached distance/duration between two coordinate pairs.
 *
 * <p>Coordinates are rounded to 4 decimals (~11m precision) so nearby calls
 * hit the same cache entry. A composite uniqueness constraint enforces one
 * entry per directional pair.
 */
@jakarta.persistence.Entity
@Table(
        name = "distance_cache",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_distance_cache_pair",
                columnNames = {"from_lat", "from_lon", "to_lat", "to_lon"}),
        indexes = {
                @Index(name = "idx_distance_cache_pair",
                        columnList = "from_lat, from_lon, to_lat, to_lon")
        }
)
@Getter
@NoArgsConstructor
public class DistanceCacheEntry extends Entity<Long> {

    @Column(name = "from_lat", nullable = false)
    private double fromLat;

    @Column(name = "from_lon", nullable = false)
    private double fromLon;

    @Column(name = "to_lat", nullable = false)
    private double toLat;

    @Column(name = "to_lon", nullable = false)
    private double toLon;

    @Column(name = "distance_km", nullable = false)
    private double distanceKm;

    @Column(name = "duration_minutes", nullable = false)
    private double durationMinutes;

    @Column(name = "geometry_json", columnDefinition = "TEXT")
    private String geometryJson;

    @Column(name = "cached_at", nullable = false)
    private LocalDateTime cachedAt;

    public static DistanceCacheEntry of(
            double fromLat, double fromLon, double toLat, double toLon,
            double distanceKm, double durationMinutes, String geometryJson
    ) {
        DistanceCacheEntry e = new DistanceCacheEntry();
        e.fromLat = fromLat;
        e.fromLon = fromLon;
        e.toLat = toLat;
        e.toLon = toLon;
        e.distanceKm = distanceKm;
        e.durationMinutes = durationMinutes;
        e.geometryJson = geometryJson;
        e.cachedAt = LocalDateTime.now();
        return e;
    }
}
