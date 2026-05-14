package ardaaydinkilinc.Cam_Sise.analytics.anomaly;

import ardaaydinkilinc.Cam_Sise.inventory.domain.FillerStock;
import ardaaydinkilinc.Cam_Sise.inventory.domain.StockMovementHistory;
import ardaaydinkilinc.Cam_Sise.inventory.domain.vo.AssetType;
import ardaaydinkilinc.Cam_Sise.inventory.domain.vo.StockMovement.MovementType;
import ardaaydinkilinc.Cam_Sise.inventory.repository.FillerStockRepository;
import ardaaydinkilinc.Cam_Sise.inventory.repository.StockMovementHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.TreeMap;
import java.util.List;

/**
 * Detects stock-movement anomalies using day-of-week baseline + z-score.
 *
 * <p>Two detection modes:
 * <ul>
 *   <li>{@link #checkLatestDay(Long, AssetType)} — used by the proactive
 *       scheduler. Compares the most recent day's net inflow to the seasonal
 *       baseline.</li>
 *   <li>{@link #checkInflow(Long, AssetType, int, LocalDateTime)} — used by the
 *       reactive event handler when a new inflow is recorded. Same statistics,
 *       different trigger.</li>
 * </ul>
 *
 * <p>Algorithm:
 * <ol>
 *   <li>Pull last {@code lookbackDays} of movement history (default 60 days).</li>
 *   <li>Bucket inflows per calendar day → daily series.</li>
 *   <li>Build a {@link DayOfWeekBaseline} grouped by weekday.</li>
 *   <li>Compute z = (observed − μ) / σ for the day under test.</li>
 *   <li>Map |z| to {@link AnomalySeverity}.</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AnomalyDetectionService {

    public static final int DEFAULT_LOOKBACK_DAYS = 60;
    public static final double MIN_STDDEV = 0.5;

    private final StockMovementHistoryRepository movementRepo;
    private final FillerStockRepository stockRepo;

    public AnomalyResult checkLatestDay(Long fillerId, AssetType assetType) {
        LocalDate today = LocalDate.now();
        LocalDateTime since = today.minusDays(DEFAULT_LOOKBACK_DAYS).atStartOfDay();

        List<StockMovementHistory> movements = movementRepo.findRecent(fillerId, assetType, since);

        TreeMap<LocalDate, Double> dailyInflow = aggregateDailyInflow(movements);
        if (dailyInflow.size() < DayOfWeekBaseline.MIN_OBSERVATIONS) {
            return AnomalyResult.normal(fillerId, assetType,
                    "Yetersiz tarihsel veri (" + dailyInflow.size() + " gün)");
        }

        double observed = dailyInflow.getOrDefault(today, 0.0);

        TreeMap<LocalDate, Double> historical = new TreeMap<>(dailyInflow);
        historical.remove(today);

        return evaluate(fillerId, assetType, observed, today, today.atStartOfDay(), historical,
                "Günlük inflow anomalisi");
    }

    public AnomalyResult checkInflow(Long fillerId, AssetType assetType, int newInflow, LocalDateTime occurredAt) {
        LocalDate evalDate = occurredAt.toLocalDate();
        LocalDateTime since = evalDate.minusDays(DEFAULT_LOOKBACK_DAYS).atStartOfDay();

        List<StockMovementHistory> movements = movementRepo.findRecent(fillerId, assetType, since);

        TreeMap<LocalDate, Double> dailyInflow = aggregateDailyInflow(movements);
        // Don't include the just-recorded inflow in baseline: filter it out first
        TreeMap<LocalDate, Double> historical = dailyInflow;
        historical.remove(evalDate);

        if (historical.size() < DayOfWeekBaseline.MIN_OBSERVATIONS) {
            return AnomalyResult.normal(fillerId, assetType,
                    "Yetersiz tarihsel veri (" + historical.size() + " gün)");
        }

        return evaluate(fillerId, assetType, newInflow, evalDate, occurredAt, historical,
                "Yeni inflow tetikli anomali kontrolü");
    }

    /**
     * Checks whether the current stock level itself exceeds historical norms.
     * Useful when the threshold has not been hit yet but accumulation is unusual.
     */
    public AnomalyResult checkStockLevel(Long fillerId, AssetType assetType) {
        FillerStock stock = stockRepo.findByFillerIdAndAssetType(fillerId, assetType)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Stock not found for filler=" + fillerId + ", asset=" + assetType));

        LocalDate today = LocalDate.now();
        LocalDateTime since = today.minusDays(DEFAULT_LOOKBACK_DAYS).atStartOfDay();
        List<StockMovementHistory> movements = movementRepo.findRecent(fillerId, assetType, since);

        TreeMap<LocalDate, Double> dailyEndOfDayLevel = aggregateEndOfDayLevel(movements);
        dailyEndOfDayLevel.remove(today);
        if (dailyEndOfDayLevel.size() < DayOfWeekBaseline.MIN_OBSERVATIONS) {
            return AnomalyResult.normal(fillerId, assetType,
                    "Yetersiz tarihsel veri (" + dailyEndOfDayLevel.size() + " gün)");
        }

        return evaluate(fillerId, assetType, stock.getCurrentQuantity(),
                today, LocalDateTime.now(), dailyEndOfDayLevel,
                "Stok seviyesi anomalisi");
    }

    private AnomalyResult evaluate(
            Long fillerId, AssetType assetType,
            double observed, LocalDate evalDate, LocalDateTime evaluatedAt,
            TreeMap<LocalDate, Double> historical, String reasonPrefix) {

        DayOfWeekBaseline baseline = new DayOfWeekBaseline(historical);
        DayOfWeek dow = evalDate.getDayOfWeek();

        double mean = baseline.meanFor(dow);
        double stdDev = Math.max(baseline.stdDevFor(dow), MIN_STDDEV);
        double z = (observed - mean) / stdDev;

        AnomalySeverity severity = AnomalySeverity.fromZScore(z);

        String reason = String.format(
                "%s — gözlem=%.1f, beklenen=%.1f ± %.1f, z=%.2f, gün=%s",
                reasonPrefix, observed, mean, stdDev, z, dow);

        return new AnomalyResult(fillerId, assetType, evaluatedAt,
                observed, mean, stdDev, z, severity, reason);
    }

    private TreeMap<LocalDate, Double> aggregateDailyInflow(List<StockMovementHistory> movements) {
        TreeMap<LocalDate, Double> daily = new TreeMap<>();
        ZoneId zone = ZoneId.systemDefault();
        for (StockMovementHistory m : movements) {
            if (m.getMovementType() != MovementType.INFLOW) continue;
            LocalDate day = m.getOccurredAt().atZone(zone).toLocalDate();
            daily.merge(day, (double) m.getQuantity(), Double::sum);
        }
        return daily;
    }

    private TreeMap<LocalDate, Double> aggregateEndOfDayLevel(List<StockMovementHistory> movements) {
        TreeMap<LocalDate, Double> daily = new TreeMap<>();
        ZoneId zone = ZoneId.systemDefault();
        for (StockMovementHistory m : movements) {
            LocalDate day = m.getOccurredAt().atZone(zone).toLocalDate();
            daily.put(day, (double) m.getQuantityAfter());
        }
        return daily;
    }
}
