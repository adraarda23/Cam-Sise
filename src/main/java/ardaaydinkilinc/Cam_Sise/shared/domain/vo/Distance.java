package ardaaydinkilinc.Cam_Sise.shared.domain.vo;

import ardaaydinkilinc.Cam_Sise.shared.domain.base.ValueObject;

/**
 * Distance value object
 */
public record Distance(double kilometers) implements ValueObject {

    public Distance {
        if (kilometers < 0) {
            throw new IllegalArgumentException("Distance cannot be negative");
        }
    }

    public double toMeters() {
        return kilometers * 1000;
    }

    public Distance add(Distance other) {
        return new Distance(this.kilometers + other.kilometers);
    }

    public Distance subtract(Distance other) {
        double result = this.kilometers - other.kilometers;
        if (result < 0) {
            throw new IllegalArgumentException("Resulting distance cannot be negative");
        }
        return new Distance(result);
    }
}
