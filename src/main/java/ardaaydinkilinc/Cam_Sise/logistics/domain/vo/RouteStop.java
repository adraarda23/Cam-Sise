package ardaaydinkilinc.Cam_Sise.logistics.domain.vo;

import ardaaydinkilinc.Cam_Sise.shared.domain.base.ValueObject;
import ardaaydinkilinc.Cam_Sise.shared.domain.vo.Duration;
import ardaaydinkilinc.Cam_Sise.shared.domain.vo.GeoCoordinates;

/**
 * Route stop value object
 * Represents a single stop in a collection route
 */
public record RouteStop(
        Long fillerId,
        GeoCoordinates location,
        int stopOrder,
        int estimatedPallets,
        int estimatedSeparators,
        Duration estimatedServiceTime
) implements ValueObject {

    public RouteStop {
        if (fillerId == null) {
            throw new IllegalArgumentException("Filler ID cannot be null");
        }
        if (location == null) {
            throw new IllegalArgumentException("Location cannot be null");
        }
        if (stopOrder < 0) {
            throw new IllegalArgumentException("Stop order cannot be negative");
        }
        if (estimatedPallets < 0 || estimatedSeparators < 0) {
            throw new IllegalArgumentException("Estimated quantities cannot be negative");
        }
        if (estimatedServiceTime == null) {
            throw new IllegalArgumentException("Service time cannot be null");
        }
    }

    /**
     * Get estimated capacity for this stop
     */
    public Capacity getEstimatedCapacity() {
        return new Capacity(estimatedPallets, estimatedSeparators);
    }
}
