package ardaaydinkilinc.Cam_Sise.shared.domain.vo;

import ardaaydinkilinc.Cam_Sise.shared.domain.base.ValueObject;

/**
 * Duration value object (in minutes)
 */
public record Duration(int minutes) implements ValueObject {

    public Duration {
        if (minutes < 0) {
            throw new IllegalArgumentException("Duration cannot be negative");
        }
    }

    public int toHours() {
        return minutes / 60;
    }

    public int getRemainingMinutes() {
        return minutes % 60;
    }

    public Duration add(Duration other) {
        return new Duration(this.minutes + other.minutes);
    }

    public String format() {
        int hours = toHours();
        int mins = getRemainingMinutes();
        if (hours > 0) {
            return String.format("%d hours %d minutes", hours, mins);
        }
        return String.format("%d minutes", mins);
    }
}
