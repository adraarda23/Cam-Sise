package ardaaydinkilinc.Cam_Sise.analytics;

import ardaaydinkilinc.Cam_Sise.core.repository.FillerRepository;
import ardaaydinkilinc.Cam_Sise.inventory.domain.FillerStock;
import ardaaydinkilinc.Cam_Sise.inventory.domain.vo.AssetType;
import ardaaydinkilinc.Cam_Sise.inventory.domain.vo.LossRate;
import ardaaydinkilinc.Cam_Sise.inventory.repository.FillerStockRepository;
import ardaaydinkilinc.Cam_Sise.logistics.domain.CollectionPlan;
import ardaaydinkilinc.Cam_Sise.logistics.domain.CollectionRequest;
import ardaaydinkilinc.Cam_Sise.logistics.repository.CollectionPlanRepository;
import ardaaydinkilinc.Cam_Sise.logistics.repository.CollectionRequestRepository;
import ardaaydinkilinc.Cam_Sise.shared.domain.vo.Distance;
import ardaaydinkilinc.Cam_Sise.shared.domain.vo.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AnalyticsService Tests")
class AnalyticsServiceTest {

    @Mock private CollectionRequestRepository requestRepo;
    @Mock private CollectionPlanRepository planRepo;
    @Mock private FillerStockRepository stockRepo;
    @Mock private FillerRepository fillerRepo;

    @InjectMocks
    private AnalyticsService service;

    private static final Long POOL_OPERATOR_ID = 1L;

    @Nested
    @DisplayName("getSummary - request stats")
    class RequestStats {

        @Test
        @DisplayName("returns zero counts when no requests exist")
        void returnsZeroWhenEmpty() {
            when(requestRepo.findByPoolOperatorId(POOL_OPERATOR_ID)).thenReturn(List.of());
            when(planRepo.findByPoolOperatorId(POOL_OPERATOR_ID)).thenReturn(List.of());
            when(stockRepo.findByPoolOperatorId(POOL_OPERATOR_ID)).thenReturn(List.of());
            when(fillerRepo.findByPoolOperatorId(POOL_OPERATOR_ID)).thenReturn(List.of());

            AnalyticsSummary summary = service.getSummary(POOL_OPERATOR_ID);

            assertThat(summary.totalRequests()).isZero();
            assertThat(summary.palletRequests()).isZero();
            assertThat(summary.separatorRequests()).isZero();
            assertThat(summary.requestsByStatus()).isEmpty();
        }

        @Test
        @DisplayName("counts requests by asset type correctly")
        void countsAssetTypesSplit() {
            CollectionRequest p1 = CollectionRequest.createManual(1L, AssetType.PALLET, 100, 1L);
            CollectionRequest p2 = CollectionRequest.createManual(1L, AssetType.PALLET, 50, 1L);
            CollectionRequest s1 = CollectionRequest.createManual(1L, AssetType.SEPARATOR, 200, 1L);

            when(requestRepo.findByPoolOperatorId(POOL_OPERATOR_ID)).thenReturn(List.of(p1, p2, s1));
            when(planRepo.findByPoolOperatorId(POOL_OPERATOR_ID)).thenReturn(List.of());
            when(stockRepo.findByPoolOperatorId(POOL_OPERATOR_ID)).thenReturn(List.of());
            when(fillerRepo.findByPoolOperatorId(POOL_OPERATOR_ID)).thenReturn(List.of());

            AnalyticsSummary summary = service.getSummary(POOL_OPERATOR_ID);

            assertThat(summary.totalRequests()).isEqualTo(3);
            assertThat(summary.palletRequests()).isEqualTo(2);
            assertThat(summary.separatorRequests()).isEqualTo(1);
        }

        @Test
        @DisplayName("groups requests by status correctly")
        void groupsByStatus() {
            CollectionRequest pending = CollectionRequest.createManual(1L, AssetType.PALLET, 100, 1L);
            CollectionRequest approved = CollectionRequest.createManual(1L, AssetType.PALLET, 100, 1L);
            approved.approve(1L);
            CollectionRequest cancelled = CollectionRequest.createManual(1L, AssetType.PALLET, 100, 1L);
            cancelled.cancel();

            when(requestRepo.findByPoolOperatorId(POOL_OPERATOR_ID)).thenReturn(List.of(pending, approved, cancelled));
            when(planRepo.findByPoolOperatorId(POOL_OPERATOR_ID)).thenReturn(List.of());
            when(stockRepo.findByPoolOperatorId(POOL_OPERATOR_ID)).thenReturn(List.of());
            when(fillerRepo.findByPoolOperatorId(POOL_OPERATOR_ID)).thenReturn(List.of());

            AnalyticsSummary summary = service.getSummary(POOL_OPERATOR_ID);

            assertThat(summary.requestsByStatus()).containsEntry("PENDING", 1L);
            assertThat(summary.requestsByStatus()).containsEntry("APPROVED", 1L);
            assertThat(summary.requestsByStatus()).containsEntry("CANCELLED", 1L);
        }
    }

    @Nested
    @DisplayName("getSummary - plan stats")
    class PlanStats {

        @Test
        @DisplayName("returns zero plan stats when no plans exist")
        void returnsZeroWhenNoPlans() {
            when(requestRepo.findByPoolOperatorId(POOL_OPERATOR_ID)).thenReturn(List.of());
            when(planRepo.findByPoolOperatorId(POOL_OPERATOR_ID)).thenReturn(List.of());
            when(stockRepo.findByPoolOperatorId(POOL_OPERATOR_ID)).thenReturn(List.of());
            when(fillerRepo.findByPoolOperatorId(POOL_OPERATOR_ID)).thenReturn(List.of());

            AnalyticsSummary summary = service.getSummary(POOL_OPERATOR_ID);

            assertThat(summary.totalPlans()).isZero();
            assertThat(summary.avgDistanceKm()).isZero();
            assertThat(summary.avgDurationMinutes()).isZero();
        }

        @Test
        @DisplayName("calculates average distance and duration across plans")
        void calculatesAverages() {
            CollectionPlan plan1 = CollectionPlan.generate(1L, new Distance(100), new Duration(120), 200, 100, LocalDate.now(), "[]");
            CollectionPlan plan2 = CollectionPlan.generate(1L, new Distance(200), new Duration(240), 300, 150, LocalDate.now(), "[]");

            when(requestRepo.findByPoolOperatorId(POOL_OPERATOR_ID)).thenReturn(List.of());
            when(planRepo.findByPoolOperatorId(POOL_OPERATOR_ID)).thenReturn(List.of(plan1, plan2));
            when(stockRepo.findByPoolOperatorId(POOL_OPERATOR_ID)).thenReturn(List.of());
            when(fillerRepo.findByPoolOperatorId(POOL_OPERATOR_ID)).thenReturn(List.of());

            AnalyticsSummary summary = service.getSummary(POOL_OPERATOR_ID);

            assertThat(summary.totalPlans()).isEqualTo(2);
            assertThat(summary.avgDistanceKm()).isEqualTo(150.0);
            assertThat(summary.avgDurationMinutes()).isEqualTo(180.0);
        }
    }

    @Nested
    @DisplayName("getSummary - stock stats")
    class StockStats {

        @Test
        @DisplayName("sums pallet and separator stocks separately")
        void sumsStocksByAssetType() {
            FillerStock pallet1 = stockWith(1L, AssetType.PALLET, 300);
            FillerStock pallet2 = stockWith(2L, AssetType.PALLET, 200);
            FillerStock sep1 = stockWith(1L, AssetType.SEPARATOR, 150);

            when(requestRepo.findByPoolOperatorId(POOL_OPERATOR_ID)).thenReturn(List.of());
            when(planRepo.findByPoolOperatorId(POOL_OPERATOR_ID)).thenReturn(List.of());
            when(stockRepo.findByPoolOperatorId(POOL_OPERATOR_ID)).thenReturn(List.of(pallet1, pallet2, sep1));
            when(fillerRepo.findByPoolOperatorId(POOL_OPERATOR_ID)).thenReturn(List.of());

            AnalyticsSummary summary = service.getSummary(POOL_OPERATOR_ID);

            assertThat(summary.totalPalletStock()).isEqualTo(500);
            assertThat(summary.totalSeparatorStock()).isEqualTo(150);
        }

        @Test
        @DisplayName("counts fillers with stock at or above threshold")
        void countsFillersBelowThreshold() {
            FillerStock above = FillerStock.initialize(1L, AssetType.PALLET, 100, new LossRate(5.0));
            above.recordInflow(150, "INF");
            above.clearDomainEvents();

            FillerStock below = FillerStock.initialize(2L, AssetType.PALLET, 200, new LossRate(5.0));
            below.recordInflow(50, "INF");
            below.clearDomainEvents();

            when(requestRepo.findByPoolOperatorId(POOL_OPERATOR_ID)).thenReturn(List.of());
            when(planRepo.findByPoolOperatorId(POOL_OPERATOR_ID)).thenReturn(List.of());
            when(stockRepo.findByPoolOperatorId(POOL_OPERATOR_ID)).thenReturn(List.of(above, below));
            when(fillerRepo.findByPoolOperatorId(POOL_OPERATOR_ID)).thenReturn(List.of());

            AnalyticsSummary summary = service.getSummary(POOL_OPERATOR_ID);

            assertThat(summary.fillersWithLowPalletStock()).isEqualTo(1);
        }
    }

    private FillerStock stockWith(Long fillerId, AssetType assetType, int quantity) {
        FillerStock stock = FillerStock.initialize(fillerId, assetType, 50, new LossRate(5.0));
        stock.recordInflow(quantity, "INF");
        stock.clearDomainEvents();
        return stock;
    }
}
