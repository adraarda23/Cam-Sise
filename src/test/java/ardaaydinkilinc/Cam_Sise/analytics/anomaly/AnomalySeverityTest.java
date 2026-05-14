package ardaaydinkilinc.Cam_Sise.analytics.anomaly;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AnomalySeverityTest {

    @Test
    @DisplayName("|z| < 2 yields NORMAL")
    void normalBand() {
        assertThat(AnomalySeverity.fromZScore(0.0)).isEqualTo(AnomalySeverity.NORMAL);
        assertThat(AnomalySeverity.fromZScore(1.9)).isEqualTo(AnomalySeverity.NORMAL);
        assertThat(AnomalySeverity.fromZScore(-1.9)).isEqualTo(AnomalySeverity.NORMAL);
    }

    @Test
    @DisplayName("2 <= |z| < 3 yields WARNING")
    void warningBand() {
        assertThat(AnomalySeverity.fromZScore(2.0)).isEqualTo(AnomalySeverity.WARNING);
        assertThat(AnomalySeverity.fromZScore(2.9)).isEqualTo(AnomalySeverity.WARNING);
        assertThat(AnomalySeverity.fromZScore(-2.5)).isEqualTo(AnomalySeverity.WARNING);
    }

    @Test
    @DisplayName("|z| >= 3 yields CRITICAL")
    void criticalBand() {
        assertThat(AnomalySeverity.fromZScore(3.0)).isEqualTo(AnomalySeverity.CRITICAL);
        assertThat(AnomalySeverity.fromZScore(5.5)).isEqualTo(AnomalySeverity.CRITICAL);
        assertThat(AnomalySeverity.fromZScore(-4.0)).isEqualTo(AnomalySeverity.CRITICAL);
    }

    @Test
    @DisplayName("isAlarmWorthy: only WARNING and CRITICAL")
    void alarmWorthy() {
        assertThat(AnomalySeverity.NORMAL.isAlarmWorthy()).isFalse();
        assertThat(AnomalySeverity.WARNING.isAlarmWorthy()).isTrue();
        assertThat(AnomalySeverity.CRITICAL.isAlarmWorthy()).isTrue();
    }
}
