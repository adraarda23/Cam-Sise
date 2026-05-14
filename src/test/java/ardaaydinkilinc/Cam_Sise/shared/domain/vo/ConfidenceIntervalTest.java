package ardaaydinkilinc.Cam_Sise.shared.domain.vo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class ConfidenceIntervalTest {

    @Test
    @DisplayName("of() default confidence is 0.95")
    void defaultConfidenceLevel() {
        ConfidenceInterval ci = ConfidenceInterval.of(45.0, 5.0, 30);
        assertEquals(0.95, ci.confidenceLevel());
        assertEquals(1.96, ci.zScore(), 0.01);
    }

    @Test
    @DisplayName("Margin of error uses z * stdDev / sqrt(n) at 95%")
    void marginOfErrorAt95Percent() {
        ConfidenceInterval ci = ConfidenceInterval.of(45.0, 10.0, 100);
        double expectedMargin = 1.96 * 10.0 / Math.sqrt(100);
        assertThat(ci.marginOfError()).isCloseTo(expectedMargin, within(0.001));
    }

    @Test
    @DisplayName("Lower and upper bounds bracket the mean symmetrically")
    void boundsAreSymmetric() {
        ConfidenceInterval ci = ConfidenceInterval.of(45.0, 10.0, 100);
        double margin = ci.marginOfError();
        assertThat(ci.lowerBound()).isCloseTo(45.0 - margin, within(0.001));
        assertThat(ci.upperBound()).isCloseTo(45.0 + margin, within(0.001));
    }

    @Test
    @DisplayName("formatted produces '45.0 ± 1.96' style string")
    void formattedDisplay() {
        ConfidenceInterval ci = ConfidenceInterval.of(45.0, 10.0, 100);
        String formatted = ci.formatted();
        assertThat(formatted).startsWith("45.0 ± ");
        assertThat(formatted).contains("±");
    }

    @Test
    @DisplayName("formattedBounds produces '[lower – upper]' style string")
    void formattedBoundsDisplay() {
        ConfidenceInterval ci = ConfidenceInterval.of(45.0, 10.0, 100);
        String bounds = ci.formattedBounds();
        assertThat(bounds).startsWith("[");
        assertThat(bounds).endsWith("]");
        assertThat(bounds).contains("–");
    }

    @Test
    @DisplayName("Point estimate has zero stdDev and width")
    void pointEstimate() {
        ConfidenceInterval ci = ConfidenceInterval.pointEstimate(42.0);
        assertEquals(42.0, ci.mean());
        assertEquals(0.0, ci.stdDev());
        assertEquals(0.0, ci.marginOfError());
        assertEquals(0.0, ci.width());
    }

    @Test
    @DisplayName("99% confidence uses z=2.576")
    void zScoreAt99Percent() {
        ConfidenceInterval ci = new ConfidenceInterval(0, 1, 10, 0.99);
        assertEquals(2.576, ci.zScore(), 0.01);
    }

    @Test
    @DisplayName("90% confidence uses z=1.645")
    void zScoreAt90Percent() {
        ConfidenceInterval ci = new ConfidenceInterval(0, 1, 10, 0.90);
        assertEquals(1.645, ci.zScore(), 0.01);
    }

    @Test
    @DisplayName("Standard error with n=1 falls back to stdDev")
    void standardErrorEdgeCase() {
        ConfidenceInterval ci = ConfidenceInterval.of(10.0, 3.0, 1);
        assertEquals(3.0, ci.standardError());
    }

    @Test
    @DisplayName("Negative stdDev rejected")
    void negativeStdDevRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new ConfidenceInterval(10, -1, 5, 0.95));
    }

    @Test
    @DisplayName("Confidence level outside (0,1) rejected")
    void invalidConfidenceLevelRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new ConfidenceInterval(10, 1, 5, 1.0));
        assertThrows(IllegalArgumentException.class,
                () -> new ConfidenceInterval(10, 1, 5, 0.0));
    }

    @Test
    @DisplayName("Negative sample size rejected")
    void negativeSampleSizeRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new ConfidenceInterval(10, 1, -1, 0.95));
    }

    @Test
    @DisplayName("NaN mean rejected")
    void nanMeanRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new ConfidenceInterval(Double.NaN, 1, 5, 0.95));
    }

    @Test
    @DisplayName("Relative uncertainty = margin / |mean|")
    void relativeUncertainty() {
        ConfidenceInterval ci = ConfidenceInterval.of(100.0, 10.0, 100);
        double expected = ci.marginOfError() / 100.0;
        assertThat(ci.relativeUncertainty()).isCloseTo(expected, within(0.001));
    }

    @Test
    @DisplayName("Relative uncertainty handles zero mean")
    void relativeUncertaintyZeroMean() {
        ConfidenceInterval zeroPoint = ConfidenceInterval.pointEstimate(0.0);
        assertEquals(0.0, zeroPoint.relativeUncertainty());

        ConfidenceInterval zeroMeanWithSpread = ConfidenceInterval.of(0.0, 5.0, 10);
        assertEquals(1.0, zeroMeanWithSpread.relativeUncertainty());
    }

    @Test
    @DisplayName("Width equals upper minus lower")
    void widthMatchesBounds() {
        ConfidenceInterval ci = ConfidenceInterval.of(50.0, 5.0, 25);
        assertThat(ci.width()).isCloseTo(ci.upperBound() - ci.lowerBound(), within(0.001));
    }
}
