package ardaaydinkilinc.Cam_Sise.shared.domain.vo;

import ardaaydinkilinc.Cam_Sise.shared.domain.base.ValueObject;

/**
 * Geographic coordinates value object
 */
public record GeoCoordinates(
        double latitude,
        double longitude
) implements ValueObject {

    public GeoCoordinates {
        if (latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException("Latitude must be between -90 and 90");
        }
        if (longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Longitude must be between -180 and 180");
        }
    }

    /**
     * Calculate distance to another coordinate using Haversine formula
     * @param other the other coordinate
     * @return distance in kilometers
     */
    public Distance distanceTo(GeoCoordinates other) {
        final double EARTH_RADIUS_KM = 6371.0;

        double dLat = Math.toRadians(other.latitude - this.latitude);
        double dLon = Math.toRadians(other.longitude - this.longitude);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(this.latitude)) *
                        Math.cos(Math.toRadians(other.latitude)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        double distanceKm = EARTH_RADIUS_KM * c;

        return new Distance(distanceKm);
    }
}
