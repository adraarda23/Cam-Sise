package ardaaydinkilinc.Cam_Sise.shared.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.time.DayOfWeek;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for DataSeeder's helper functions (seasonality + weekday curves).
 * Reflective invocation lets us cover the private helpers without firing the
 * full @PostConstruct seeding flow, which would require a live database.
 */
class DataSeederSeasonalityTest {

    @Test
    @DisplayName("Summer months produce higher inflow than winter months on average")
    void summerIsHigherThanWinter() throws Exception {
        DataSeeder seeder = bareSeeder();
        Method m = DataSeeder.class.getDeclaredMethod("seasonalMultiplier", int.class);
        m.setAccessible(true);
        double summerAvg = ((double) m.invoke(seeder, 6) + (double) m.invoke(seeder, 7) + (double) m.invoke(seeder, 8)) / 3.0;
        double winterAvg = ((double) m.invoke(seeder, 12) + (double) m.invoke(seeder, 1) + (double) m.invoke(seeder, 2)) / 3.0;

        assertThat(summerAvg).isGreaterThan(winterAvg * 1.5);
    }

    @Test
    @DisplayName("Weekend multiplier is strictly lower than weekday")
    void weekendIsQuieter() throws Exception {
        DataSeeder seeder = bareSeeder();
        Method m = DataSeeder.class.getDeclaredMethod("weekdayMultiplier", DayOfWeek.class);
        m.setAccessible(true);

        for (DayOfWeek weekday : new DayOfWeek[]{
                DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY}) {
            double wd = (double) m.invoke(seeder, weekday);
            double sat = (double) m.invoke(seeder, DayOfWeek.SATURDAY);
            double sun = (double) m.invoke(seeder, DayOfWeek.SUNDAY);
            assertThat(wd).isGreaterThan(sat);
            assertThat(sat).isGreaterThan(sun);
        }
    }

    /**
     * Constructs a DataSeeder without invoking PostConstruct or wiring repositories.
     * Acceptable here because we are only exercising pure helper functions.
     */
    private DataSeeder bareSeeder() {
        return new DataSeeder(null, null, null, null, null, null, null);
    }
}
