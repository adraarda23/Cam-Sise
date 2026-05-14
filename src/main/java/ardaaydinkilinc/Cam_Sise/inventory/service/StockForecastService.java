package ardaaydinkilinc.Cam_Sise.inventory.service;

import ardaaydinkilinc.Cam_Sise.inventory.domain.FillerStock;
import ardaaydinkilinc.Cam_Sise.inventory.domain.StockMovementHistory;
import ardaaydinkilinc.Cam_Sise.inventory.domain.vo.AssetType;
import ardaaydinkilinc.Cam_Sise.inventory.domain.vo.StockMovement.MovementType;
import ardaaydinkilinc.Cam_Sise.inventory.repository.FillerStockRepository;
import ardaaydinkilinc.Cam_Sise.inventory.repository.StockMovementHistoryRepository;
import ardaaydinkilinc.Cam_Sise.shared.domain.vo.ConfidenceInterval;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Forecasts future stock quantity for a (filler, assetType) pair.
 *
 * <p>Approach (intentionally simple and explainable for the thesis):
 * <ol>
 *   <li>Read the last <code>lookbackDays</code> of {@link StockMovementHistory}.</li>
 *   <li>Aggregate into a daily net-flow series (inflows − collections per calendar day).</li>
 *   <li>Compute mean μ and standard deviation σ of daily net flow with
 *       {@link DescriptiveStatistics}.</li>
 *   <li>Project forward: forecasted_quantity = current + μ × horizonDays.</li>
 *   <li>Variance propagation: Var(sum of n daily flows) = n × σ². Hence the
 *       forecast standard deviation = σ × √horizonDays (random-walk style).</li>
 * </ol>
 *
 * <p>This is not a sophisticated model, but it gives a defensible point estimate
 * plus a 95% confidence band that widens with horizon — which is exactly the
 * "45 ± 3.4" presentation the advisor asked for.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class StockForecastService {

    public static final int DEFAULT_LOOKBACK_DAYS = 30;
    public static final int MIN_DAYS_FOR_STATISTICS = 3;

    private final StockMovementHistoryRepository movementRepo;
    private final FillerStockRepository stockRepo;

    public ConfidenceInterval forecast(Long fillerId, AssetType assetType, int horizonDays) {
        return forecast(fillerId, assetType, horizonDays, DEFAULT_LOOKBACK_DAYS);
    }

    public ConfidenceInterval forecast(Long fillerId, AssetType assetType, int horizonDays, int lookbackDays) {
        if (horizonDays <= 0) {
            throw new IllegalArgumentException("horizonDays must be positive");
        }
        if (lookbackDays <= 0) {
            throw new IllegalArgumentException("lookbackDays must be positive");
        }

        FillerStock stock = stockRepo.findByFillerIdAndAssetType(fillerId, assetType)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Stock not found for filler=" + fillerId + ", asset=" + assetType));

        int currentQuantity = stock.getCurrentQuantity();
        LocalDateTime since = LocalDateTime.now().minusDays(lookbackDays);

        List<StockMovementHistory> movements = movementRepo.findRecent(fillerId, assetType, since);

        if (movements.isEmpty()) {
            log.debug("No movements for forecast — returning point estimate. filler={} asset={}",
                    fillerId, assetType);
            return ConfidenceInterval.pointEstimate(currentQuantity);
        }

        Map<LocalDate, Integer> dailyNetFlow = aggregateDailyNetFlow(movements);

        if (dailyNetFlow.size() < MIN_DAYS_FOR_STATISTICS) {
            log.debug("Only {} day(s) of data — returning point estimate", dailyNetFlow.size());
            double avgNet = dailyNetFlow.values().stream().mapToInt(Integer::intValue).average().orElse(0.0);
            double projected = currentQuantity + avgNet * horizonDays;
            return ConfidenceInterval.pointEstimate(projected);
        }

        DescriptiveStatistics stats = new DescriptiveStatistics();
        dailyNetFlow.values().forEach(v -> stats.addValue(v));

        double meanDaily = stats.getMean();
        double stdDevDaily = stats.getStandardDeviation();
        int sampleDays = (int) stats.getN();

        double forecastMean = currentQuantity + meanDaily * horizonDays;
        double forecastStdDev = stdDevDaily * Math.sqrt(horizonDays);

        return ConfidenceInterval.of(forecastMean, forecastStdDev, sampleDays);
    }

    private Map<LocalDate, Integer> aggregateDailyNetFlow(List<StockMovementHistory> movements) {
        Map<LocalDate, Integer> netFlow = new HashMap<>();
        ZoneId zone = ZoneId.systemDefault();
        for (StockMovementHistory m : movements) {
            LocalDate day = m.getOccurredAt().atZone(zone).toLocalDate();
            int delta = (m.getMovementType() == MovementType.INFLOW)
                    ? m.getQuantity()
                    : -m.getQuantity();
            netFlow.merge(day, delta, Integer::sum);
        }
        return netFlow;
    }

    /**
     * Returns the time horizon (in days) before the forecasted stock is expected
     * to hit the threshold. Useful for proactive alerting. Returns -1 when stock
     * is currently dropping (negative drift cannot hit threshold from below).
     */
    public int estimateDaysUntilThreshold(Long fillerId, AssetType assetType) {
        FillerStock stock = stockRepo.findByFillerIdAndAssetType(fillerId, assetType)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Stock not found for filler=" + fillerId + ", asset=" + assetType));

        int currentQuantity = stock.getCurrentQuantity();
        int threshold = stock.getThresholdQuantity();

        if (currentQuantity >= threshold) {
            return 0;
        }

        LocalDateTime since = LocalDateTime.now().minusDays(DEFAULT_LOOKBACK_DAYS);
        List<StockMovementHistory> movements = movementRepo.findRecent(fillerId, assetType, since);

        if (movements.isEmpty()) {
            return -1;
        }

        Map<LocalDate, Integer> daily = aggregateDailyNetFlow(movements);
        double meanDaily = daily.values().stream().mapToInt(Integer::intValue).average().orElse(0.0);

        if (meanDaily <= 0.0) {
            return -1;
        }

        return (int) Math.ceil((threshold - currentQuantity) / meanDaily);
    }
}
