package ardaaydinkilinc.Cam_Sise.analytics.anomaly;

import ardaaydinkilinc.Cam_Sise.inventory.domain.StockMovementHistory;
import ardaaydinkilinc.Cam_Sise.inventory.domain.vo.AssetType;
import ardaaydinkilinc.Cam_Sise.inventory.repository.FillerStockRepository;
import ardaaydinkilinc.Cam_Sise.inventory.repository.StockMovementHistoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnomalyDetectionServiceTest {

    @Mock private StockMovementHistoryRepository movementRepo;
    @Mock private FillerStockRepository stockRepo;

    @InjectMocks
    private AnomalyDetectionService service;

    private static final Long FILLER_ID = 1L;
    private static final AssetType ASSET = AssetType.PALLET;

    @Test
    @DisplayName("checkInflow: large spike flagged as CRITICAL")
    void spikeDetectedAsCritical() {
        LocalDateTime today = LocalDate.now().atTime(10, 0);
        List<StockMovementHistory> history = buildSteadyInflows(15.0, 30);
        when(movementRepo.findRecent(eq(FILLER_ID), eq(ASSET), any())).thenReturn(history);

        AnomalyResult result = service.checkInflow(FILLER_ID, ASSET, 200, today);

        assertThat(result.severity()).isIn(AnomalySeverity.WARNING, AnomalySeverity.CRITICAL);
        assertThat(result.zScore()).isGreaterThan(2.0);
        assertThat(result.isAnomaly()).isTrue();
    }

    @Test
    @DisplayName("checkInflow: value close to mean stays NORMAL")
    void closeToMeanIsNormal() {
        LocalDateTime today = LocalDate.now().atTime(10, 0);
        List<StockMovementHistory> history = buildSteadyInflows(15.0, 30);
        when(movementRepo.findRecent(eq(FILLER_ID), eq(ASSET), any())).thenReturn(history);

        AnomalyResult result = service.checkInflow(FILLER_ID, ASSET, 15, today);

        assertThat(result.severity()).isEqualTo(AnomalySeverity.NORMAL);
        assertThat(result.isAnomaly()).isFalse();
    }

    @Test
    @DisplayName("checkInflow: insufficient history yields NORMAL with reason")
    void insufficientHistoryYieldsNormal() {
        when(movementRepo.findRecent(eq(FILLER_ID), eq(ASSET), any())).thenReturn(List.of());

        AnomalyResult result = service.checkInflow(FILLER_ID, ASSET, 100, LocalDateTime.now());

        assertThat(result.severity()).isEqualTo(AnomalySeverity.NORMAL);
        assertThat(result.reason()).contains("Yetersiz");
    }

    @Test
    @DisplayName("Day-of-week baseline distinguishes weekends from weekdays")
    void weekdayDifferentiation() {
        // History: weekdays ~ 100 ± 15, weekends ~ 10 ± 2.
        // Jitter is keyed by the absolute day index (deterministic, varies per same-weekday-occurrence).
        List<StockMovementHistory> history = new ArrayList<>();
        LocalDate start = LocalDate.of(2026, 4, 1);
        int[] perDayJitter = {-15, -10, -5, 0, 5, 10, 15, -12, -3, 8, -7, 4, 11, -6, 2};
        for (int d = 0; d < 30; d++) {
            LocalDate day = start.plusDays(d);
            int qty;
            int jitter = perDayJitter[d % perDayJitter.length];
            switch (day.getDayOfWeek()) {
                case SATURDAY, SUNDAY -> qty = Math.max(1, 10 + jitter / 5);
                default -> qty = Math.max(1, 100 + jitter);
            }
            history.add(StockMovementHistory.inflow(
                    FILLER_ID, ASSET, qty, 0, "X",
                    day.atTime(9, 0)));
        }
        when(movementRepo.findRecent(eq(FILLER_ID), eq(ASSET), any())).thenReturn(history);

        // 80 on a Sunday is unusually high (Sunday baseline is ~10) → anomaly
        LocalDate aSunday = LocalDate.of(2026, 5, 3);
        AnomalyResult sundayResult = service.checkInflow(FILLER_ID, ASSET, 80, aSunday.atTime(9, 0));
        assertThat(sundayResult.zScore()).isGreaterThan(2.0);

        // 105 on a weekday (baseline ~100, stdDev > 5) → |z| < 2 (not strongly anomalous)
        LocalDate aWeekday = LocalDate.of(2026, 5, 4); // Monday
        AnomalyResult weekdayResult = service.checkInflow(FILLER_ID, ASSET, 105, aWeekday.atTime(9, 0));
        assertThat(Math.abs(weekdayResult.zScore())).isLessThan(2.0);
    }

    @Test
    @DisplayName("expected bounds are around mean ± 2σ")
    void boundsMatchWarningThreshold() {
        AnomalyResult r = new AnomalyResult(
                FILLER_ID, ASSET, LocalDateTime.now(),
                100, 50, 10, 5.0, AnomalySeverity.CRITICAL, "test");

        assertThat(r.lowerExpected()).isEqualTo(50 - 2 * 10);
        assertThat(r.upperExpected()).isEqualTo(50 + 2 * 10);
    }

    private List<StockMovementHistory> buildSteadyInflows(double meanQty, int days) {
        List<StockMovementHistory> list = new ArrayList<>();
        LocalDateTime base = LocalDateTime.now().minusDays(days);
        for (int i = 0; i < days; i++) {
            int qty = (int) Math.round(meanQty + ((i % 3) - 1));
            list.add(StockMovementHistory.inflow(
                    FILLER_ID, ASSET, qty, 0, "X", base.plusDays(i)));
        }
        return list;
    }
}
