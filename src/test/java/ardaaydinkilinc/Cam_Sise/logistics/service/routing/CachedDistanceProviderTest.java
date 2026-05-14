package ardaaydinkilinc.Cam_Sise.logistics.service.routing;

import ardaaydinkilinc.Cam_Sise.shared.domain.vo.GeoCoordinates;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CachedDistanceProviderTest {

    @Mock private DistanceProvider delegate;
    @Mock private DistanceCacheRepository repo;

    private CachedDistanceProvider provider;

    @BeforeEach
    void setUp() {
        provider = new CachedDistanceProvider(delegate, repo, new ObjectMapper(), 30);
    }

    @Test
    @DisplayName("Cache miss → delegate called → result persisted")
    void cacheMissDelegates() {
        GeoCoordinates from = new GeoCoordinates(40.4333, 29.1667);
        GeoCoordinates to = new GeoCoordinates(40.7128, 29.2056);

        when(repo.findByFromLatAndFromLonAndToLatAndToLon(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(Optional.empty());
        when(delegate.route(from, to)).thenReturn(RouteSegment.of(35.4, 42.5));

        RouteSegment result = provider.route(from, to);

        assertThat(result.distanceKm()).isEqualTo(35.4);
        verify(delegate, times(1)).route(from, to);
        verify(repo, times(1)).save(any(DistanceCacheEntry.class));
    }

    @Test
    @DisplayName("Cache hit (fresh) → delegate not called")
    void cacheHitSkipsDelegate() {
        GeoCoordinates from = new GeoCoordinates(40.4333, 29.1667);
        GeoCoordinates to = new GeoCoordinates(40.7128, 29.2056);

        DistanceCacheEntry entry = DistanceCacheEntry.of(40.4333, 29.1667, 40.7128, 29.2056,
                35.4, 42.5, null);
        when(repo.findByFromLatAndFromLonAndToLatAndToLon(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(Optional.of(entry));

        RouteSegment result = provider.route(from, to);

        assertThat(result.distanceKm()).isCloseTo(35.4, within(0.01));
        verify(delegate, never()).route(any(), any());
    }

    @Test
    @DisplayName("Stale cache entry → delegate called again")
    void staleEntryRefreshes() {
        GeoCoordinates from = new GeoCoordinates(40.4333, 29.1667);
        GeoCoordinates to = new GeoCoordinates(40.7128, 29.2056);

        DistanceCacheEntry stale = DistanceCacheEntry.of(40.4333, 29.1667, 40.7128, 29.2056,
                35.4, 42.5, null);
        ReflectionTestUtils.setField(stale, "cachedAt", LocalDateTime.now().minusDays(60));

        when(repo.findByFromLatAndFromLonAndToLatAndToLon(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(Optional.of(stale));
        when(delegate.route(from, to)).thenReturn(RouteSegment.of(36.0, 43.0));

        RouteSegment result = provider.route(from, to);

        assertThat(result.distanceKm()).isEqualTo(36.0);
        verify(delegate, times(1)).route(from, to);
    }

    @Test
    @DisplayName("Coordinate rounding lets nearby calls hit the same cache key")
    void coordinatesAreRounded() {
        GeoCoordinates p1 = new GeoCoordinates(40.43330001, 29.16669999);
        GeoCoordinates p2 = new GeoCoordinates(40.71280005, 29.20559994);

        DistanceCacheEntry entry = DistanceCacheEntry.of(40.4333, 29.1667, 40.7128, 29.2056,
                35.4, 42.5, null);
        when(repo.findByFromLatAndFromLonAndToLatAndToLon(40.4333, 29.1667, 40.7128, 29.2056))
                .thenReturn(Optional.of(entry));

        RouteSegment result = provider.route(p1, p2);

        assertThat(result.distanceKm()).isCloseTo(35.4, within(0.01));
        verify(delegate, never()).route(any(), any());
    }

    @Test
    @DisplayName("Cache write failure is swallowed and segment still returned")
    void cacheWriteFailureSwallowed() {
        GeoCoordinates from = new GeoCoordinates(40.4333, 29.1667);
        GeoCoordinates to = new GeoCoordinates(40.7128, 29.2056);

        when(repo.findByFromLatAndFromLonAndToLatAndToLon(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(Optional.empty());
        when(delegate.route(from, to)).thenReturn(RouteSegment.of(35.4, 42.5));
        when(repo.save(any())).thenThrow(new RuntimeException("DB down"));

        RouteSegment result = provider.route(from, to);

        assertThat(result.distanceKm()).isEqualTo(35.4);
    }
}
