package ardaaydinkilinc.Cam_Sise.logistics.service.routing;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Wires up exactly one {@code @Primary} {@link DistanceProvider} based on
 * configuration. Removes the previous ambiguity where both
 * {@code OsrmDistanceProvider} and {@code CachedDistanceProvider} could
 * simultaneously be primary and Spring failed to start.
 *
 * <p>Selection rules:
 * <ul>
 *   <li>If {@code app.routing.cache.enabled=true}: the primary bean is a
 *       {@link CachedDistanceProvider} wrapping the underlying provider
 *       chosen by {@code app.routing.distance-provider}.</li>
 *   <li>Otherwise: the primary bean is the underlying provider directly
 *       (haversine by default, osrm when configured).</li>
 * </ul>
 */
@Configuration
@Slf4j
public class DistanceProviderConfig {

    @Bean
    @Primary
    public DistanceProvider activeDistanceProvider(
            @Qualifier("haversineDistanceProvider") DistanceProvider haversine,
            @Qualifier("osrmDistanceProvider") DistanceProvider osrm,
            DistanceCacheRepository cacheRepository,
            DistanceCacheWriter cacheWriter,
            ObjectMapper objectMapper,
            @Value("${app.routing.distance-provider:haversine}") String preferredProvider,
            @Value("${app.routing.cache.enabled:false}") boolean cacheEnabled,
            @Value("${app.routing.cache.ttl-days:30}") int ttlDays
    ) {
        DistanceProvider underlying = "osrm".equalsIgnoreCase(preferredProvider) ? osrm : haversine;
        log.info("🛣️ Active distance provider: underlying={}, cache={}, ttlDays={}",
                "osrm".equalsIgnoreCase(preferredProvider) ? "OSRM" : "Haversine",
                cacheEnabled ? "enabled" : "disabled",
                ttlDays);

        if (cacheEnabled) {
            return new CachedDistanceProvider(underlying, cacheRepository, cacheWriter, objectMapper, ttlDays);
        }
        return underlying;
    }
}
