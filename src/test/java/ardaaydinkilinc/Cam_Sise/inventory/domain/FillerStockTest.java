package ardaaydinkilinc.Cam_Sise.inventory.domain;

import ardaaydinkilinc.Cam_Sise.inventory.domain.event.AssetCollected;
import ardaaydinkilinc.Cam_Sise.inventory.domain.event.AssetInflowRecorded;
import ardaaydinkilinc.Cam_Sise.inventory.domain.event.StockThresholdExceeded;
import ardaaydinkilinc.Cam_Sise.inventory.domain.vo.AssetType;
import ardaaydinkilinc.Cam_Sise.inventory.domain.vo.LossRate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("FillerStock Domain Tests")
class FillerStockTest {

    @Test
    @DisplayName("Should initialize stock with default values")
    void shouldInitializeStock() {
        Long fillerId = 1L;
        AssetType assetType = AssetType.PALLET;
        int threshold = 100;
        LossRate lossRate = new LossRate(5.0);

        FillerStock stock = FillerStock.initialize(fillerId, assetType, threshold, lossRate);

        assertThat(stock.getFillerId()).isEqualTo(fillerId);
        assertThat(stock.getAssetType()).isEqualTo(assetType);
        assertThat(stock.getCurrentQuantity()).isZero();
        assertThat(stock.getThresholdQuantity()).isEqualTo(threshold);
        assertThat(stock.getEstimatedLossRate()).isEqualTo(lossRate);
    }

    @Test
    @DisplayName("Should record inflow and increase stock quantity")
    void shouldRecordInflow() {
        FillerStock stock = FillerStock.initialize(1L, AssetType.PALLET, 100, new LossRate(5.0));
        int inflowQuantity = 50;
        String referenceId = "INF-001";

        stock.recordInflow(inflowQuantity, referenceId);

        assertThat(stock.getCurrentQuantity()).isEqualTo(50);
        assertThat(stock.getDomainEvents()).hasSize(1);
        assertThat(stock.getDomainEvents().get(0)).isInstanceOf(AssetInflowRecorded.class);

        AssetInflowRecorded event = (AssetInflowRecorded) stock.getDomainEvents().get(0);
        assertThat(event.quantity()).isEqualTo(inflowQuantity);
        assertThat(event.newTotalQuantity()).isEqualTo(50);
    }

    @Test
    @DisplayName("Should record collection and decrease stock quantity")
    void shouldRecordCollection() {
        FillerStock stock = FillerStock.initialize(1L, AssetType.PALLET, 100, new LossRate(5.0));
        stock.recordInflow(100, "INF-001");
        stock.clearDomainEvents();

        int collectionQuantity = 30;
        String collectionPlanId = "PLAN-001";

        stock.recordCollection(collectionQuantity, collectionPlanId);

        assertThat(stock.getCurrentQuantity()).isEqualTo(70);
        assertThat(stock.getDomainEvents()).hasSize(1);
        assertThat(stock.getDomainEvents().get(0)).isInstanceOf(AssetCollected.class);

        AssetCollected event = (AssetCollected) stock.getDomainEvents().get(0);
        assertThat(event.quantity()).isEqualTo(collectionQuantity);
        assertThat(event.remainingQuantity()).isEqualTo(70);
    }

    @Test
    @DisplayName("Should publish StockThresholdExceeded event when threshold is exceeded")
    void shouldPublishStockThresholdExceededEvent() {
        FillerStock stock = FillerStock.initialize(1L, AssetType.PALLET, 100, new LossRate(5.0));
        stock.clearDomainEvents();

        stock.recordInflow(150, "INF-001");

        assertThat(stock.getCurrentQuantity()).isEqualTo(150);
        assertThat(stock.getDomainEvents()).hasSize(2);

        boolean hasThresholdEvent = stock.getDomainEvents().stream()
                .anyMatch(event -> event instanceof StockThresholdExceeded);
        assertThat(hasThresholdEvent).isTrue();
    }

    @Test
    @DisplayName("Should update threshold successfully")
    void shouldUpdateThreshold() {
        FillerStock stock = FillerStock.initialize(1L, AssetType.PALLET, 100, new LossRate(5.0));
        int newThreshold = 200;

        stock.updateThreshold(newThreshold);

        assertThat(stock.getThresholdQuantity()).isEqualTo(newThreshold);
    }

    @Test
    @DisplayName("Should update loss rate successfully")
    void shouldUpdateLossRate() {
        FillerStock stock = FillerStock.initialize(1L, AssetType.PALLET, 100, new LossRate(5.0));
        LossRate newLossRate = new LossRate(7.5);

        stock.updateEstimatedLossRate(newLossRate);

        assertThat(stock.getEstimatedLossRate()).isEqualTo(newLossRate);
        assertThat(stock.getEstimatedLossRate().percentage()).isEqualTo(7.5);
    }

    @Test
    @DisplayName("Should throw exception when collection quantity exceeds current stock")
    void shouldThrowExceptionWhenCollectionExceedsStock() {
        FillerStock stock = FillerStock.initialize(1L, AssetType.PALLET, 100, new LossRate(5.0));
        stock.recordInflow(50, "INF-001");

        assertThatThrownBy(() -> stock.recordCollection(100, "PLAN-001"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot collect");
    }

    @Test
    @DisplayName("Should throw exception when inflow quantity is negative")
    void shouldThrowExceptionForNegativeInflow() {
        FillerStock stock = FillerStock.initialize(1L, AssetType.PALLET, 100, new LossRate(5.0));

        assertThatThrownBy(() -> stock.recordInflow(-10, "INF-001"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Inflow quantity must be positive");
    }

    @Test
    @DisplayName("Should throw exception when collection quantity is negative")
    void shouldThrowExceptionForNegativeCollection() {
        FillerStock stock = FillerStock.initialize(1L, AssetType.PALLET, 100, new LossRate(5.0));
        stock.recordInflow(100, "INF-001");

        assertThatThrownBy(() -> stock.recordCollection(-10, "PLAN-001"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Collection quantity must be positive");
    }

    @Test
    @DisplayName("Should not publish threshold event when stock is below threshold")
    void shouldNotPublishThresholdEventWhenBelowThreshold() {
        FillerStock stock = FillerStock.initialize(1L, AssetType.PALLET, 100, new LossRate(5.0));
        stock.clearDomainEvents();

        stock.recordInflow(50, "INF-001");

        assertThat(stock.getCurrentQuantity()).isEqualTo(50);
        assertThat(stock.getDomainEvents()).hasSize(1);
        assertThat(stock.getDomainEvents().get(0)).isInstanceOf(AssetInflowRecorded.class);
    }
}
