package ardaaydinkilinc.Cam_Sise.inventory.service;

import ardaaydinkilinc.Cam_Sise.inventory.domain.StockMovementHistory;
import ardaaydinkilinc.Cam_Sise.inventory.domain.vo.AssetType;
import ardaaydinkilinc.Cam_Sise.inventory.domain.vo.LossRate;
import ardaaydinkilinc.Cam_Sise.inventory.domain.vo.StockMovement.MovementType;
import ardaaydinkilinc.Cam_Sise.inventory.repository.StockMovementHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Computes statistical loss-rate estimates from the StockMovementHistory time series.
 *
 * <p>For each non-overlapping pair of (INFLOW, COLLECTION) observations we treat
 * <code>1 - collectedQty / inflowQty</code> as a sample of the realised loss
 * fraction during that cycle. The estimated rate is the sample mean (in %),
 * the standard deviation is the sample stdDev (in %), and the sample size is
 * the number of pairs used.
 *
 * <p>Window: configurable lookback (default 30 days). If we cannot find at
 * least two pairs the result falls back to a point estimate using the prior
 * rate or 0%.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class LossEstimationService {

    public static final int DEFAULT_WINDOW_DAYS = 30;
    public static final int MIN_SAMPLES = 2;

    private final StockMovementHistoryRepository movementHistoryRepository;

    public LossRate estimateRate(Long fillerId, AssetType assetType) {
        return estimateRate(fillerId, assetType, DEFAULT_WINDOW_DAYS, null);
    }

    public LossRate estimateRate(Long fillerId, AssetType assetType, int windowDays, LossRate priorRate) {
        LocalDateTime since = LocalDateTime.now().minusDays(windowDays);
        List<StockMovementHistory> movements = movementHistoryRepository
                .findRecent(fillerId, assetType, since);

        List<Double> lossFractions = pairwiseLossFractions(movements);

        if (lossFractions.size() < MIN_SAMPLES) {
            log.debug("Not enough samples ({}) for fillerId={} assetType={} — keeping prior",
                    lossFractions.size(), fillerId, assetType);
            return priorRate != null ? priorRate : new LossRate(0.0);
        }

        DescriptiveStatistics stats = new DescriptiveStatistics();
        lossFractions.forEach(f -> stats.addValue(f * 100.0));

        double mean = clamp(stats.getMean(), 0.0, 100.0);
        double stdDev = Math.max(0.0, stats.getStandardDeviation());
        int n = (int) stats.getN();

        return new LossRate(mean, stdDev, n);
    }

    /**
     * Pair successive INFLOW → COLLECTION events. Each pair yields one loss-fraction sample:
     * <code>1 - collectedQty / inflowQty</code>, clamped to [0, 1].
     * Adjustments are skipped since they do not represent a true cycle.
     */
    private List<Double> pairwiseLossFractions(List<StockMovementHistory> movements) {
        List<Double> fractions = new ArrayList<>();
        int pendingInflowQty = 0;
        for (StockMovementHistory m : movements) {
            if (m.getMovementType() == MovementType.INFLOW) {
                pendingInflowQty += m.getQuantity();
            } else if (m.getMovementType() == MovementType.COLLECTION && pendingInflowQty > 0) {
                int collected = Math.min(m.getQuantity(), pendingInflowQty);
                double fraction = 1.0 - ((double) collected / pendingInflowQty);
                fractions.add(clamp(fraction, 0.0, 1.0));
                pendingInflowQty = 0;
            }
        }
        return fractions;
    }

    private double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
