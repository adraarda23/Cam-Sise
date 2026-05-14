package ardaaydinkilinc.Cam_Sise.inventory.domain.vo;

import ardaaydinkilinc.Cam_Sise.shared.domain.base.ValueObject;

/**
 * Loss rate value object.
 *
 * <p>Carries the estimated loss percentage plus the statistical uncertainty
 * (standard deviation, sample size) used to compute it. When stdDev is zero
 * and sampleSize is 1, the value is a point estimate (no uncertainty info).
 *
 * <p>Persisted as embeddable on FillerStock and LossRecord. {@code stdDev}
 * and {@code sampleSize} are wrapper types so Hibernate can read existing
 * rows where these new columns are still NULL (legacy data); null is
 * normalised to "point estimate" semantics via {@link #stdDevOrZero()} and
 * {@link #sampleSizeOrOne()}.
 *
 * <p>The legacy single-arg constructor is preserved so older callers and
 * stored rows continue to work.
 */
public record LossRate(double percentage, Double stdDev, Integer sampleSize) implements ValueObject {

    public LossRate {
        if (percentage < 0 || percentage > 100) {
            throw new IllegalArgumentException("Loss rate must be between 0 and 100");
        }
        if (stdDev != null && (stdDev < 0 || Double.isNaN(stdDev) || Double.isInfinite(stdDev))) {
            throw new IllegalArgumentException("Standard deviation must be non-negative and finite");
        }
        if (sampleSize != null && sampleSize < 0) {
            throw new IllegalArgumentException("Sample size cannot be negative");
        }
    }

    public LossRate(double percentage) {
        this(percentage, 0.0, 1);
    }

    /**
     * Backward-compatible primitive constructor — kept so existing call sites
     * that pass three primitive arguments continue to compile and behave as
     * before. New callers should prefer the canonical (Double, Integer) form
     * when the values might legitimately be unknown.
     */
    public LossRate(double percentage, double stdDev, int sampleSize) {
        this(percentage, Double.valueOf(stdDev), Integer.valueOf(sampleSize));
    }

    /**
     * Null-safe accessor: returns 0.0 when the column was NULL in the database
     * or when the value was never set (legacy / point estimate).
     */
    public double stdDevOrZero() {
        return stdDev != null ? stdDev : 0.0;
    }

    /**
     * Null-safe accessor: returns 1 when the column was NULL or never set.
     */
    public int sampleSizeOrOne() {
        return sampleSize != null && sampleSize > 0 ? sampleSize : 1;
    }

    public int calculateLoss(int totalQuantity) {
        return (int) Math.round(totalQuantity * (percentage / 100.0));
    }

    public int calculateRemaining(int totalQuantity) {
        return totalQuantity - calculateLoss(totalQuantity);
    }

    public boolean isPointEstimate() {
        return stdDevOrZero() == 0.0 && sampleSizeOrOne() <= 1;
    }

    public double marginOfError95() {
        if (isPointEstimate()) return 0.0;
        return 1.96 * stdDevOrZero() / Math.sqrt(Math.max(1, sampleSizeOrOne()));
    }

    public double lowerBound95() {
        return Math.max(0.0, percentage - marginOfError95());
    }

    public double upperBound95() {
        return Math.min(100.0, percentage + marginOfError95());
    }

    public String formatted() {
        return String.format("%.2f%%", percentage);
    }

    public String formattedWithConfidence() {
        if (isPointEstimate()) return formatted();
        return String.format("%.2f%% ± %.2f%%", percentage, marginOfError95());
    }
}
