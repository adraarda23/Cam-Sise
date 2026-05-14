package ardaaydinkilinc.Cam_Sise.logistics.service.routing;

import ardaaydinkilinc.Cam_Sise.shared.domain.vo.GeoCoordinates;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Decorator that memoizes {@link DistanceProvider} responses in the database.
 *
 * <p>Not a Spring component on its own — it is wired by
 * {@link DistanceProviderConfig} so the underlying provider (haversine or
 * osrm) and TTL can be selected from properties without conflicting
 * {@code @Primary} declarations.
 *
 * <p>Cache key: (rounded-from-lat, from-lon, to-lat, to-lon). Rounding to 4
 * decimals (~11m) means small numerical jitter doesn't bust the cache.
 * Entries older than {@code ttlDays} are considered stale and recomputed.
 *
 * <p>Tolerant on cache write failures — if a cache write fails (e.g.
 * transient DB issue) we still return the computed segment to the caller.
 */
@Slf4j
public class CachedDistanceProvider implements DistanceProvider {

    private static final double COORD_ROUNDING_FACTOR = 10_000.0;

    private final DistanceProvider delegate;
    private final DistanceCacheRepository cacheRepository;
    private final ObjectMapper objectMapper;
    private final int ttlDays;

    public CachedDistanceProvider(
            DistanceProvider delegate,
            DistanceCacheRepository cacheRepository,
            ObjectMapper objectMapper,
            int ttlDays
    ) {
        this.delegate = delegate;
        this.cacheRepository = cacheRepository;
        this.objectMapper = objectMapper;
        this.ttlDays = ttlDays;
    }

    @Override
    public RouteSegment route(GeoCoordinates from, GeoCoordinates to) {
        double fromLat = round(from.latitude());
        double fromLon = round(from.longitude());
        double toLat = round(to.latitude());
        double toLon = round(to.longitude());

        Optional<DistanceCacheEntry> cached = cacheRepository
                .findByFromLatAndFromLonAndToLatAndToLon(fromLat, fromLon, toLat, toLon);

        if (cached.isPresent() && !isStale(cached.get())) {
            DistanceCacheEntry e = cached.get();
            return new RouteSegment(e.getDistanceKm(), e.getDurationMinutes(), parseGeometry(e.getGeometryJson()));
        }

        RouteSegment fresh = delegate.route(from, to);
        try {
            String geomJson = fresh.geometry() != null && !fresh.geometry().isEmpty()
                    ? objectMapper.writeValueAsString(fresh.geometry())
                    : null;
            DistanceCacheEntry entry = DistanceCacheEntry.of(
                    fromLat, fromLon, toLat, toLon,
                    fresh.distanceKm(), fresh.durationMinutes(), geomJson);
            cacheRepository.save(entry);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize geometry for cache: {}", e.getMessage());
        } catch (Exception e) {
            log.warn("Failed to write distance cache entry: {}", e.getMessage());
        }
        return fresh;
    }

    private boolean isStale(DistanceCacheEntry entry) {
        return entry.getCachedAt().isBefore(LocalDateTime.now().minusDays(ttlDays));
    }

    private double round(double v) {
        return Math.round(v * COORD_ROUNDING_FACTOR) / COORD_ROUNDING_FACTOR;
    }

    private List<GeoCoordinates> parseGeometry(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            List<?> raw = objectMapper.readValue(json, List.class);
            List<GeoCoordinates> coords = new ArrayList<>(raw.size());
            for (Object o : raw) {
                if (o instanceof java.util.Map<?, ?> map) {
                    Number lat = (Number) map.get("latitude");
                    Number lon = (Number) map.get("longitude");
                    if (lat != null && lon != null) {
                        coords.add(new GeoCoordinates(lat.doubleValue(), lon.doubleValue()));
                    }
                }
            }
            return coords;
        } catch (Exception e) {
            log.warn("Failed to parse cached geometry: {}", e.getMessage());
            return null;
        }
    }
}
