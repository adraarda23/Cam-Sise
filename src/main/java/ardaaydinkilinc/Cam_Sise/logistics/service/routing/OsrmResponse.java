package ardaaydinkilinc.Cam_Sise.logistics.service.routing;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Minimal projection of the OSRM /route response.
 * We only care about the first route's distance, duration, and (optionally)
 * the geometry (GeoJSON LineString → List of [lon, lat] pairs).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OsrmResponse(String code, List<Route> routes) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Route(double distance, double duration, Geometry geometry) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Geometry(String type, List<List<Double>> coordinates) {}
}
