package ardaaydinkilinc.Cam_Sise.inventory.domain.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class LossRateTest {

    @Test
    @DisplayName("Single-arg constructor produces a point estimate")
    void legacyConstructorIsPointEstimate() {
        LossRate r = new LossRate(5.0);
        assertThat(r.percentage()).isEqualTo(5.0);
        assertThat(r.stdDev()).isEqualTo(0.0);
        assertThat(r.sampleSize()).isEqualTo(1);
        assertThat(r.isPointEstimate()).isTrue();
        assertThat(r.marginOfError95()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Three-arg constructor carries stdDev and sample size")
    void threeArgConstructor() {
        LossRate r = new LossRate(5.0, 1.5, 30);
        assertThat(r.stdDev()).isEqualTo(1.5);
        assertThat(r.sampleSize()).isEqualTo(30);
        assertThat(r.isPointEstimate()).isFalse();
    }

    @Test
    @DisplayName("Margin of error follows 1.96 * stdDev / sqrt(n)")
    void marginOfError() {
        LossRate r = new LossRate(5.0, 2.0, 25);
        double expected = 1.96 * 2.0 / Math.sqrt(25);
        assertThat(r.marginOfError95()).isCloseTo(expected, within(0.001));
    }

    @Test
    @DisplayName("Bounds are clamped to [0, 100]")
    void boundsAreClamped() {
        LossRate r = new LossRate(2.0, 5.0, 4);
        assertThat(r.lowerBound95()).isGreaterThanOrEqualTo(0.0);
        assertThat(r.upperBound95()).isLessThanOrEqualTo(100.0);
    }

    @Test
    @DisplayName("Loss calculation is rounded correctly")
    void calculateLoss() {
        LossRate r = new LossRate(10.0);
        assertThat(r.calculateLoss(100)).isEqualTo(10);
        assertThat(r.calculateRemaining(100)).isEqualTo(90);
    }

    @Test
    @DisplayName("formattedWithConfidence shows ± only when uncertainty exists")
    void formattedWithConfidence() {
        assertThat(new LossRate(5.0).formattedWithConfidence()).isEqualTo("5.00%");
        assertThat(new LossRate(5.0, 1.0, 25).formattedWithConfidence()).contains("±");
    }

    @Test
    @DisplayName("Out-of-range percentage rejected")
    void invalidPercentage() {
        assertThatThrownBy(() -> new LossRate(-1.0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new LossRate(101.0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Negative stdDev rejected")
    void invalidStdDev() {
        assertThatThrownBy(() -> new LossRate(5.0, -1.0, 10))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Negative sample size rejected")
    void invalidSampleSize() {
        assertThatThrownBy(() -> new LossRate(5.0, 1.0, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
