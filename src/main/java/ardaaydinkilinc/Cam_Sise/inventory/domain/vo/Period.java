package ardaaydinkilinc.Cam_Sise.inventory.domain.vo;

import ardaaydinkilinc.Cam_Sise.shared.domain.base.ValueObject;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Time period value object
 */
public record Period(
        LocalDate startDate,
        LocalDate endDate
) implements ValueObject {

    public Period {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Dates cannot be null");
        }
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date must be before or equal to end date");
        }
    }

    public long getDays() {
        return ChronoUnit.DAYS.between(startDate, endDate) + 1;
    }

    public long getMonths() {
        return ChronoUnit.MONTHS.between(startDate, endDate);
    }

    public boolean contains(LocalDate date) {
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }

    public String formatted() {
        return String.format("%s to %s (%d days)", startDate, endDate, getDays());
    }
}
