package ardaaydinkilinc.Cam_Sise.shared.domain.vo;

import ardaaydinkilinc.Cam_Sise.shared.domain.base.ValueObject;

/**
 * Confidence interval value object.
 * Represents a statistical estimate (mean) with its uncertainty (standard deviation)
 * computed from a finite sample. Used for stock forecasts, loss rate estimation,
 * and any other prediction that should communicate uncertainty.
 *
 * For 95% confidence, z ≈ 1.96. For 99%, z ≈ 2.576. For 90%, z ≈ 1.645.
 */
public record ConfidenceInterval(
        double mean,
        double stdDev,
        int sampleSize,
        double confidenceLevel
) implements ValueObject {

    public static final double DEFAULT_CONFIDENCE = 0.95;

    public ConfidenceInterval {
        if (Double.isNaN(mean) || Double.isInfinite(mean)) {
            throw new IllegalArgumentException("Mean must be a finite number");
        }
        if (stdDev < 0 || Double.isNaN(stdDev) || Double.isInfinite(stdDev)) {
            throw new IllegalArgumentException("Standard deviation must be non-negative and finite");
        }
        if (sampleSize < 0) {
            throw new IllegalArgumentException("Sample size cannot be negative");
        }
        if (confidenceLevel <= 0 || confidenceLevel >= 1) {
            throw new IllegalArgumentException("Confidence level must be in (0, 1)");
        }
    }

    public static ConfidenceInterval of(double mean, double stdDev, int sampleSize) {
        return new ConfidenceInterval(mean, stdDev, sampleSize, DEFAULT_CONFIDENCE);
    }

    public static ConfidenceInterval pointEstimate(double value) {
        return new ConfidenceInterval(value, 0.0, 1, DEFAULT_CONFIDENCE);
    }

    /**
     * Z-score lookup for common confidence levels.
     * Falls back to 1.96 (95%) for non-standard levels.
     */
    public double zScore() {
        if (confidenceLevel >= 0.99) return 2.576;
        if (confidenceLevel >= 0.95) return 1.960;
        if (confidenceLevel >= 0.90) return 1.645;
        return 1.960;
    }

    /**
     * Standard error of the mean = stdDev / sqrt(n).
     * When n <= 1 the standard error is undefined; we return stdDev as a conservative bound.
     */
    public double standardError() {
        if (sampleSize <= 1) {
            return stdDev;
        }
        return stdDev / Math.sqrt(sampleSize);
    }

    /**
     * Margin of error = z * standardError().
     */
    public double marginOfError() {
        return zScore() * standardError();
    }

    public double lowerBound() {
        return mean - marginOfError();
    }

    public double upperBound() {
        return mean + marginOfError();
    }

    /**
     * Width of the interval (upper - lower).
     */
    public double width() {
        return 2 * marginOfError();
    }

    /**
     * Relative uncertainty: marginOfError / |mean|.
     * Returns 0 when mean is zero. Useful for UI color coding
     * (narrow = high confidence, wide = low confidence).
     */
    public double relativeUncertainty() {
        if (mean == 0.0) {
            return stdDev == 0.0 ? 0.0 : 1.0;
        }
        return marginOfError() / Math.abs(mean);
    }

    /**
     * Formatted as "45.0 ± 3.4" for UI display.
     */
    public String formatted() {
        return String.format("%.1f ± %.1f", mean, marginOfError());
    }

    /**
     * Formatted with explicit bounds: "[41.6 – 48.4]".
     */
    public String formattedBounds() {
        return String.format("[%.1f – %.1f]", lowerBound(), upperBound());
    }
}
