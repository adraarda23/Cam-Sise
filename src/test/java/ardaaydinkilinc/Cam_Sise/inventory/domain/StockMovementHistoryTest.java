package ardaaydinkilinc.Cam_Sise.inventory.domain;

import ardaaydinkilinc.Cam_Sise.inventory.domain.vo.AssetType;
import ardaaydinkilinc.Cam_Sise.inventory.domain.vo.StockMovement.MovementType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StockMovementHistoryTest {

    private static final Long FILLER_ID = 1L;
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 14, 10, 0);

    @Test
    @DisplayName("inflow factory sets movement type and signed quantity is positive")
    void inflowFactory() {
        StockMovementHistory m = StockMovementHistory.inflow(
                FILLER_ID, AssetType.PALLET, 50, 200, "INF-1", NOW);

        assertThat(m.getMovementType()).isEqualTo(MovementType.INFLOW);
        assertThat(m.getQuantity()).isEqualTo(50);
        assertThat(m.getQuantityAfter()).isEqualTo(200);
        assertThat(m.signedQuantity()).isEqualTo(50);
    }

    @Test
    @DisplayName("collection factory yields negative signed quantity")
    void collectionFactory() {
        StockMovementHistory m = StockMovementHistory.collection(
                FILLER_ID, AssetType.SEPARATOR, 30, 70, "PLAN-1", NOW);

        assertThat(m.getMovementType()).isEqualTo(MovementType.COLLECTION);
        assertThat(m.signedQuantity()).isEqualTo(-30);
    }

    @Test
    @DisplayName("adjustment factory yields negative signed quantity")
    void adjustmentFactory() {
        StockMovementHistory m = StockMovementHistory.adjustment(
                FILLER_ID, AssetType.PALLET, 5, 195, "ADJ-1", NOW);

        assertThat(m.getMovementType()).isEqualTo(MovementType.ADJUSTMENT);
        assertThat(m.signedQuantity()).isEqualTo(-5);
    }

    @Test
    @DisplayName("non-positive quantity rejected")
    void nonPositiveQuantityRejected() {
        assertThatThrownBy(() -> StockMovementHistory.inflow(
                FILLER_ID, AssetType.PALLET, 0, 100, "X", NOW))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> StockMovementHistory.inflow(
                FILLER_ID, AssetType.PALLET, -1, 100, "X", NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("negative quantityAfter rejected")
    void negativeQuantityAfterRejected() {
        assertThatThrownBy(() -> StockMovementHistory.collection(
                FILLER_ID, AssetType.PALLET, 10, -1, "X", NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("null filler id rejected")
    void nullFillerIdRejected() {
        assertThatThrownBy(() -> StockMovementHistory.inflow(
                null, AssetType.PALLET, 10, 100, "X", NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("null asset type rejected")
    void nullAssetTypeRejected() {
        assertThatThrownBy(() -> StockMovementHistory.inflow(
                FILLER_ID, null, 10, 100, "X", NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("null occurredAt rejected")
    void nullOccurredAtRejected() {
        assertThatThrownBy(() -> StockMovementHistory.inflow(
                FILLER_ID, AssetType.PALLET, 10, 100, "X", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("reference id is optional")
    void referenceIdOptional() {
        StockMovementHistory m = StockMovementHistory.inflow(
                FILLER_ID, AssetType.PALLET, 10, 100, null, NOW);
        assertThat(m.getReferenceId()).isNull();
    }
}
