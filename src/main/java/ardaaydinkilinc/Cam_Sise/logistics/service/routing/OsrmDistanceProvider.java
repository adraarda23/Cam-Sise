package ardaaydinkilinc.Cam_Sise.logistics.service.routing;

import ardaaydinkilinc.Cam_Sise.shared.domain.vo.GeoCoordinates;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Real road distance + duration via OSRM HTTP API.
 *
 * <p>Default endpoint is the public demo at {@code router.project-osrm.org}.
 * Override with {@code app.routing.osrm.base-url=...} when self-hosting.
 *
 * <p>Implementation note: we use plain Java 11 {@link HttpClient} rather than
 * Spring WebClient/Reactor Netty here. WebClient occasionally hung
 * indefinitely on Java 26 + macOS during DNS / SSL handshake to the OSRM
 * public demo, with no useful exception fired even at 15s timeout. Plain
 * HttpClient is dependency-light and behaves predictably.
 *
 * <p>Failure modes (timeout, 5xx, malformed) fall back silently to
 * {@link HaversineDistanceProvider} so the optimizer keeps running.
 */
@Component("osrmDistanceProvider")
@Slf4j
public class OsrmDistanceProvider implements DistanceProvider {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(20);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final HaversineDistanceProvider fallback;

    @Value("${app.routing.osrm.base-url:https://router.project-osrm.org}")
    private String baseUrl;

    @Value("${app.routing.osrm.profile:driving}")
    private String profile;

    @Value("${app.routing.osrm.include-geometry:true}")
    private boolean includeGeometry;

    public OsrmDistanceProvider() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        this.objectMapper = new ObjectMapper();
        this.fallback = new HaversineDistanceProvider();
    }

    @Override
    public RouteSegment route(GeoCoordinates from, GeoCoordinates to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("Coordinates cannot be null");
        }

        String url = String.format(
                "%s/route/v1/%s/%f,%f;%f,%f?overview=%s&geometries=geojson",
                baseUrl, profile,
                from.longitude(), from.latitude(),
                to.longitude(), to.latitude(),
                includeGeometry ? "full" : "false");

        try {
            OsrmResponse response = httpGet(url);
            if (response == null || !"Ok".equalsIgnoreCase(response.code())
                    || response.routes() == null || response.routes().isEmpty()) {
                log.warn("OSRM returned empty/non-Ok response for {}→{}, falling back to Haversine", from, to);
                return fallbackSegment(from, to);
            }

            OsrmResponse.Route best = response.routes().get(0);
            double distanceKm = best.distance() / 1000.0;
            double durationMinutes = best.duration() / 60.0;
            List<GeoCoordinates> geometry = parseGeometry(best.geometry());
            return new RouteSegment(distanceKm, durationMinutes, geometry);
        } catch (Exception e) {
            log.warn("OSRM call failed ({}: {}), falling back to Haversine for {}→{}",
                    e.getClass().getSimpleName(), e.getMessage(), from, to);
            return fallbackSegment(from, to);
        }
    }

    /**
     * Multi-waypoint route in a single OSRM call. Far more efficient than
     * calling {@link #route} for each segment when we want geometry for an
     * entire collection plan.
     */
    public List<GeoCoordinates> routeGeometry(List<GeoCoordinates> waypoints) {
        if (waypoints == null || waypoints.size() < 2) return null;

        StringBuilder coordPart = new StringBuilder();
        for (int i = 0; i < waypoints.size(); i++) {
            if (i > 0) coordPart.append(";");
            coordPart.append(waypoints.get(i).longitude())
                     .append(",")
                     .append(waypoints.get(i).latitude());
        }
        String url = String.format(
                "%s/route/v1/%s/%s?overview=full&geometries=geojson",
                baseUrl, profile, coordPart);

        try {
            OsrmResponse response = httpGet(url);
            if (response == null || !"Ok".equalsIgnoreCase(response.code())
                    || response.routes() == null || response.routes().isEmpty()) {
                log.warn("OSRM multi-waypoint returned empty/non-Ok for {} waypoints", waypoints.size());
                return null;
            }
            return parseGeometry(response.routes().get(0).geometry());
        } catch (Exception e) {
            log.warn("OSRM multi-waypoint failed ({}: {}) — caller falls back",
                    e.getClass().getSimpleName(), e.getMessage());
            return null;
        }
    }

    /**
     * Diagnostic helper used by {@code /api/admin/osrm-health} so the user can
     * see the actual HTTP status / body when troubleshooting from the UI.
     */
    public String diagnosticPing() {
        String url = baseUrl + "/route/v1/" + profile + "/29.16,40.43;43.05,39.71?overview=false";
        try {
            long startMs = System.currentTimeMillis();
            HttpResponse<String> resp = httpClient.send(
                    HttpRequest.newBuilder(URI.create(url))
                            .timeout(REQUEST_TIMEOUT)
                            .header("Accept", "application/json")
                            .GET()
                            .build(),
                    HttpResponse.BodyHandlers.ofString());
            long elapsed = System.currentTimeMillis() - startMs;
            String body = resp.body();
            String preview = body == null ? "(null)"
                    : (body.length() > 200 ? body.substring(0, 200) + "..." : body);
            return String.format("status=%d time=%dms body=%s", resp.statusCode(), elapsed, preview);
        } catch (Exception e) {
            return String.format("EXCEPTION %s: %s", e.getClass().getSimpleName(), e.getMessage());
        }
    }

    private OsrmResponse httpGet(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "application/json")
                .header("User-Agent", "Cam-Sise/1.0")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            log.warn("OSRM HTTP {}: {}", response.statusCode(),
                    response.body() != null && response.body().length() > 200
                            ? response.body().substring(0, 200) + "..."
                            : response.body());
            return null;
        }
        return objectMapper.readValue(response.body(), OsrmResponse.class);
    }

    private List<GeoCoordinates> parseGeometry(OsrmResponse.Geometry geometry) {
        if (geometry == null || geometry.coordinates() == null) return null;
        List<GeoCoordinates> coords = new ArrayList<>(geometry.coordinates().size());
        for (List<Double> point : geometry.coordinates()) {
            if (point != null && point.size() >= 2) {
                coords.add(new GeoCoordinates(point.get(1), point.get(0)));
            }
        }
        return coords;
    }

    private RouteSegment fallbackSegment(GeoCoordinates from, GeoCoordinates to) {
        return fallback.route(from, to);
    }
}
