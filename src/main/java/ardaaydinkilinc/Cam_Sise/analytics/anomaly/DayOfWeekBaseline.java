package ardaaydinkilinc.Cam_Sise.analytics.anomaly;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.Map;
import java.util.SortedMap;

/**
 * Day-of-week aware seasonal baseline.
 *
 * <p>Given a daily series ({@code LocalDate → value}), the baseline computes the
 * mean and standard deviation <em>per day-of-week</em>. This way, a Wednesday's
 * activity is judged against historical Wednesdays — not against an averaged
 * weekly figure that would obscure weekend vs weekday differences (a concrete
 * piece of the advisor's "mevsimsellik etkilerini de dikkate alarak" feedback).
 *
 * <p>If a particular weekday has fewer than {@link #MIN_OBSERVATIONS} samples the
 * baseline for that weekday falls back to the global mean/stddev so we still
 * provide a usable estimate.
 */
public class DayOfWeekBaseline {

    public static final int MIN_OBSERVATIONS = 3;

    private final Map<DayOfWeek, DescriptiveStatistics> perDayStats = new EnumMap<>(DayOfWeek.class);
    private final DescriptiveStatistics globalStats = new DescriptiveStatistics();

    public DayOfWeekBaseline(SortedMap<LocalDate, ? extends Number> dailySeries) {
        for (DayOfWeek dow : DayOfWeek.values()) {
            perDayStats.put(dow, new DescriptiveStatistics());
        }
        for (Map.Entry<LocalDate, ? extends Number> e : dailySeries.entrySet()) {
            double v = e.getValue().doubleValue();
            perDayStats.get(e.getKey().getDayOfWeek()).addValue(v);
            globalStats.addValue(v);
        }
    }

    public double meanFor(DayOfWeek dow) {
        DescriptiveStatistics s = perDayStats.get(dow);
        if (s.getN() >= MIN_OBSERVATIONS) {
            return s.getMean();
        }
        return globalStats.getN() > 0 ? globalStats.getMean() : 0.0;
    }

    public double stdDevFor(DayOfWeek dow) {
        DescriptiveStatistics s = perDayStats.get(dow);
        if (s.getN() >= MIN_OBSERVATIONS) {
            return Math.max(s.getStandardDeviation(), 0.0);
        }
        return globalStats.getN() > 0 ? Math.max(globalStats.getStandardDeviation(), 0.0) : 0.0;
    }

    public int observationsFor(DayOfWeek dow) {
        return (int) perDayStats.get(dow).getN();
    }

    public int totalObservations() {
        return (int) globalStats.getN();
    }
}
