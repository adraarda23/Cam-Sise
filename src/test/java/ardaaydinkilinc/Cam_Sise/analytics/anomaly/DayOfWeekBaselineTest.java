package ardaaydinkilinc.Cam_Sise.analytics.anomaly;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class DayOfWeekBaselineTest {

    @Test
    @DisplayName("Mean for a weekday is computed only from that weekday's observations")
    void meanIsWeekdayLocal() {
        TreeMap<LocalDate, Double> series = new TreeMap<>();
        LocalDate monday = LocalDate.of(2026, 5, 4); // a Monday
        // 4 Mondays with value 100; 4 Saturdays with value 30
        for (int week = 0; week < 4; week++) {
            series.put(monday.plusWeeks(week), 100.0);
            series.put(monday.plusWeeks(week).plusDays(5), 30.0);
        }

        DayOfWeekBaseline baseline = new DayOfWeekBaseline(series);
        assertThat(baseline.meanFor(DayOfWeek.MONDAY)).isCloseTo(100.0, within(0.01));
        assertThat(baseline.meanFor(DayOfWeek.SATURDAY)).isCloseTo(30.0, within(0.01));
    }

    @Test
    @DisplayName("StdDev reflects within-weekday variance")
    void stdDevReflectsWeekdayVariance() {
        TreeMap<LocalDate, Double> series = new TreeMap<>();
        LocalDate monday = LocalDate.of(2026, 5, 4);
        double[] mondayValues = {80, 100, 120, 110};
        for (int i = 0; i < mondayValues.length; i++) {
            series.put(monday.plusWeeks(i), mondayValues[i]);
        }

        DayOfWeekBaseline baseline = new DayOfWeekBaseline(series);
        assertThat(baseline.stdDevFor(DayOfWeek.MONDAY)).isGreaterThan(0.0);
    }

    @Test
    @DisplayName("Weekday with fewer than MIN_OBSERVATIONS uses global fallback")
    void fallbackToGlobal() {
        TreeMap<LocalDate, Double> series = new TreeMap<>();
        LocalDate monday = LocalDate.of(2026, 5, 4);
        // 4 Mondays @ 100, only 1 Saturday @ 200
        for (int i = 0; i < 4; i++) {
            series.put(monday.plusWeeks(i), 100.0);
        }
        series.put(monday.plusDays(5), 200.0);

        DayOfWeekBaseline baseline = new DayOfWeekBaseline(series);
        double saturdayMean = baseline.meanFor(DayOfWeek.SATURDAY);
        // Saturday count = 1 < 3 → falls back to global mean
        double globalMean = (4 * 100.0 + 200.0) / 5.0;
        assertThat(saturdayMean).isCloseTo(globalMean, within(0.01));
    }

    @Test
    @DisplayName("Empty series returns zero for any weekday")
    void emptySeriesReturnsZero() {
        DayOfWeekBaseline baseline = new DayOfWeekBaseline(new TreeMap<>());
        assertThat(baseline.meanFor(DayOfWeek.MONDAY)).isEqualTo(0.0);
        assertThat(baseline.stdDevFor(DayOfWeek.MONDAY)).isEqualTo(0.0);
    }
}
