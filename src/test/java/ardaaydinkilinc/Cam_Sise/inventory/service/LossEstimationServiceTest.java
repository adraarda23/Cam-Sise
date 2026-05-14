package ardaaydinkilinc.Cam_Sise.inventory.service;

import ardaaydinkilinc.Cam_Sise.inventory.domain.StockMovementHistory;
import ardaaydinkilinc.Cam_Sise.inventory.domain.vo.AssetType;
import ardaaydinkilinc.Cam_Sise.inventory.domain.vo.LossRate;
import ardaaydinkilinc.Cam_Sise.inventory.repository.StockMovementHistoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LossEstimationServiceTest {

    @Mock private StockMovementHistoryRepository repo;

    @InjectMocks
    private LossEstimationService service;

    private static final Long FILLER_ID = 1L;
    private static final AssetType ASSET = AssetType.PALLET;

    @Test
    @DisplayName("Falls back to prior rate when no movements")
    void noMovementsFallsBackToPrior() {
        when(repo.findRecent(eq(FILLER_ID), eq(ASSET), any())).thenReturn(List.of());
        LossRate prior = new LossRate(5.0);

        LossRate result = service.estimateRate(FILLER_ID, ASSET, 30, prior);

        assertThat(result).isEqualTo(prior);
    }

    @Test
    @DisplayName("Falls back to zero when no movements and no prior")
    void noMovementsNoPrior() {
        when(repo.findRecent(eq(FILLER_ID), eq(ASSET), any())).thenReturn(List.of());

        LossRate result = service.estimateRate(FILLER_ID, ASSET, 30, null);

        assertThat(result.percentage()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Computes mean loss percentage from inflow/collection pairs")
    void computesMeanFromPairs() {
        List<StockMovementHistory> movements = new ArrayList<>();
        LocalDateTime t = LocalDateTime.of(2026, 4, 1, 10, 0);

        // Pair 1: inflow 100, collected 90 → 10% loss
        movements.add(StockMovementHistory.inflow(FILLER_ID, ASSET, 100, 100, "INF-1", t));
        movements.add(StockMovementHistory.collection(FILLER_ID, ASSET, 90, 10, "PLAN-1", t.plusDays(7)));
        // Pair 2: inflow 100, collected 92 → 8% loss
        movements.add(StockMovementHistory.inflow(FILLER_ID, ASSET, 100, 110, "INF-2", t.plusDays(8)));
        movements.add(StockMovementHistory.collection(FILLER_ID, ASSET, 92, 18, "PLAN-2", t.plusDays(15)));
        // Pair 3: inflow 100, collected 88 → 12% loss
        movements.add(StockMovementHistory.inflow(FILLER_ID, ASSET, 100, 118, "INF-3", t.plusDays(16)));
        movements.add(StockMovementHistory.collection(FILLER_ID, ASSET, 88, 30, "PLAN-3", t.plusDays(23)));

        when(repo.findRecent(eq(FILLER_ID), eq(ASSET), any())).thenReturn(movements);

        LossRate result = service.estimateRate(FILLER_ID, ASSET, 30, null);

        assertThat(result.percentage()).isCloseTo(10.0, within(0.01));
        assertThat(result.sampleSize()).isEqualTo(3);
        assertThat(result.stdDev()).isGreaterThan(0.0);
        assertThat(result.isPointEstimate()).isFalse();
    }

    @Test
    @DisplayName("Single pair returns prior (need at least MIN_SAMPLES)")
    void singlePairFallsBackToPrior() {
        List<StockMovementHistory> movements = new ArrayList<>();
        LocalDateTime t = LocalDateTime.of(2026, 4, 1, 10, 0);
        movements.add(StockMovementHistory.inflow(FILLER_ID, ASSET, 100, 100, "INF-1", t));
        movements.add(StockMovementHistory.collection(FILLER_ID, ASSET, 90, 10, "PLAN-1", t.plusDays(7)));

        when(repo.findRecent(eq(FILLER_ID), eq(ASSET), any())).thenReturn(movements);
        LossRate prior = new LossRate(7.0);

        LossRate result = service.estimateRate(FILLER_ID, ASSET, 30, prior);

        assertThat(result).isEqualTo(prior);
    }

    @Test
    @DisplayName("Loss fraction is clamped to [0,1] even for over-collection")
    void overCollectionClampsToZeroLoss() {
        List<StockMovementHistory> movements = new ArrayList<>();
        LocalDateTime t = LocalDateTime.of(2026, 4, 1, 10, 0);
        movements.add(StockMovementHistory.inflow(FILLER_ID, ASSET, 100, 100, "INF-1", t));
        movements.add(StockMovementHistory.collection(FILLER_ID, ASSET, 100, 0, "PLAN-1", t.plusDays(7)));
        movements.add(StockMovementHistory.inflow(FILLER_ID, ASSET, 100, 100, "INF-2", t.plusDays(8)));
        movements.add(StockMovementHistory.collection(FILLER_ID, ASSET, 100, 0, "PLAN-2", t.plusDays(15)));

        when(repo.findRecent(eq(FILLER_ID), eq(ASSET), any())).thenReturn(movements);

        LossRate result = service.estimateRate(FILLER_ID, ASSET, 30, null);

        assertThat(result.percentage()).isEqualTo(0.0);
        assertThat(result.sampleSize()).isEqualTo(2);
    }
}
