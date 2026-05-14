package ardaaydinkilinc.Cam_Sise.inventory.service;

import ardaaydinkilinc.Cam_Sise.inventory.domain.FillerStock;
import ardaaydinkilinc.Cam_Sise.inventory.domain.StockMovementHistory;
import ardaaydinkilinc.Cam_Sise.inventory.domain.vo.AssetType;
import ardaaydinkilinc.Cam_Sise.inventory.domain.vo.LossRate;
import ardaaydinkilinc.Cam_Sise.inventory.repository.FillerStockRepository;
import ardaaydinkilinc.Cam_Sise.inventory.repository.StockMovementHistoryRepository;
import ardaaydinkilinc.Cam_Sise.shared.domain.vo.ConfidenceInterval;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockForecastServiceTest {

    @Mock private StockMovementHistoryRepository movementRepo;
    @Mock private FillerStockRepository stockRepo;

    @InjectMocks
    private StockForecastService service;

    private static final Long FILLER_ID = 1L;
    private static final AssetType ASSET = AssetType.PALLET;

    private FillerStock stock;

    @BeforeEach
    void setUp() {
        stock = FillerStock.initialize(FILLER_ID, ASSET, 200, new LossRate(5.0));
        stock.recordInflow(100, "INIT");
        stock.clearDomainEvents();
    }

    @Test
    @DisplayName("Returns point estimate when there are no movements")
    void noMovementsPointEstimate() {
        when(stockRepo.findByFillerIdAndAssetType(FILLER_ID, ASSET)).thenReturn(Optional.of(stock));
        when(movementRepo.findRecent(eq(FILLER_ID), eq(ASSET), any())).thenReturn(List.of());

        ConfidenceInterval ci = service.forecast(FILLER_ID, ASSET, 7);

        assertThat(ci.mean()).isEqualTo((double) stock.getCurrentQuantity());
        assertThat(ci.stdDev()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Projects forward using daily mean from movement history")
    void projectsForwardUsingDailyMean() {
        when(stockRepo.findByFillerIdAndAssetType(FILLER_ID, ASSET)).thenReturn(Optional.of(stock));

        List<StockMovementHistory> movements = buildDailyInflows(10, 4); // 4 different days, +10 each
        when(movementRepo.findRecent(eq(FILLER_ID), eq(ASSET), any())).thenReturn(movements);

        ConfidenceInterval ci = service.forecast(FILLER_ID, ASSET, 7);

        double expectedMean = stock.getCurrentQuantity() + 10.0 * 7;
        assertThat(ci.mean()).isCloseTo(expectedMean, within(0.5));
        assertThat(ci.sampleSize()).isEqualTo(4);
        assertThat(ci.stdDev()).isCloseTo(0.0, within(0.01));
    }

    @Test
    @DisplayName("Forecast stdDev grows with horizon (sqrt scaling)")
    void uncertaintyGrowsWithHorizon() {
        when(stockRepo.findByFillerIdAndAssetType(FILLER_ID, ASSET)).thenReturn(Optional.of(stock));

        List<StockMovementHistory> movements = buildVariableInflows(new int[]{5, 15, 10, 20, 12});
        when(movementRepo.findRecent(eq(FILLER_ID), eq(ASSET), any())).thenReturn(movements);

        ConfidenceInterval ci1 = service.forecast(FILLER_ID, ASSET, 1);
        ConfidenceInterval ci7 = service.forecast(FILLER_ID, ASSET, 7);

        assertThat(ci7.stdDev()).isGreaterThan(ci1.stdDev());
        assertThat(ci7.stdDev()).isCloseTo(ci1.stdDev() * Math.sqrt(7), within(0.01));
    }

    @Test
    @DisplayName("estimateDaysUntilThreshold returns reasonable estimate for rising stock")
    void daysUntilThreshold() {
        when(stockRepo.findByFillerIdAndAssetType(FILLER_ID, ASSET)).thenReturn(Optional.of(stock));

        List<StockMovementHistory> movements = buildDailyInflows(20, 5);
        when(movementRepo.findRecent(eq(FILLER_ID), eq(ASSET), any())).thenReturn(movements);

        int days = service.estimateDaysUntilThreshold(FILLER_ID, ASSET);

        int expected = (int) Math.ceil((200 - stock.getCurrentQuantity()) / 20.0);
        assertThat(days).isEqualTo(expected);
    }

    @Test
    @DisplayName("estimateDaysUntilThreshold returns -1 when stock is dropping")
    void noThresholdHitWhenDropping() {
        when(stockRepo.findByFillerIdAndAssetType(FILLER_ID, ASSET)).thenReturn(Optional.of(stock));

        List<StockMovementHistory> movements = new ArrayList<>();
        LocalDateTime base = LocalDateTime.now().minusDays(5);
        for (int i = 0; i < 5; i++) {
            movements.add(StockMovementHistory.collection(
                    FILLER_ID, ASSET, 5, stock.getCurrentQuantity() - 5,
                    "PLAN-" + i, base.plusDays(i)));
        }
        when(movementRepo.findRecent(eq(FILLER_ID), eq(ASSET), any())).thenReturn(movements);

        assertThat(service.estimateDaysUntilThreshold(FILLER_ID, ASSET)).isEqualTo(-1);
    }

    @Test
    @DisplayName("Throws when stock not found")
    void throwsWhenStockMissing() {
        when(stockRepo.findByFillerIdAndAssetType(FILLER_ID, ASSET)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.forecast(FILLER_ID, ASSET, 7))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Rejects non-positive horizon")
    void rejectsBadHorizon() {
        assertThatThrownBy(() -> service.forecast(FILLER_ID, ASSET, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.forecast(FILLER_ID, ASSET, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private List<StockMovementHistory> buildDailyInflows(int qtyPerDay, int days) {
        List<StockMovementHistory> list = new ArrayList<>();
        LocalDateTime base = LocalDateTime.now().minusDays(days);
        int running = stock.getCurrentQuantity();
        for (int i = 0; i < days; i++) {
            running += qtyPerDay;
            list.add(StockMovementHistory.inflow(
                    FILLER_ID, ASSET, qtyPerDay, running, "INF-" + i, base.plusDays(i)));
        }
        return list;
    }

    private List<StockMovementHistory> buildVariableInflows(int[] dailyQuantities) {
        List<StockMovementHistory> list = new ArrayList<>();
        LocalDateTime base = LocalDateTime.now().minusDays(dailyQuantities.length);
        int running = stock.getCurrentQuantity();
        for (int i = 0; i < dailyQuantities.length; i++) {
            running += dailyQuantities[i];
            list.add(StockMovementHistory.inflow(
                    FILLER_ID, ASSET, dailyQuantities[i], running, "INF-" + i, base.plusDays(i)));
        }
        return list;
    }
}
