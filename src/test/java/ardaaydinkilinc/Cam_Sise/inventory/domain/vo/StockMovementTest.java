package ardaaydinkilinc.Cam_Sise.inventory.domain.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

@DisplayName("StockMovement Tests")
class StockMovementTest {

    private static final LocalDateTime NOW = LocalDateTime.now();

    @Nested
    @DisplayName("constructor")
    class Constructor {

        @Test
        @DisplayName("geçerli giriş hareketi oluşturmalı")
        void acceptsValidInflow() {
            StockMovement movement = new StockMovement(StockMovement.MovementType.INFLOW, 100, NOW, "REF-001");
            assertThat(movement.quantity()).isEqualTo(100);
            assertThat(movement.type()).isEqualTo(StockMovement.MovementType.INFLOW);
        }

        @Test
        @DisplayName("sıfır miktarda exception fırlatmalı")
        void throwsOnZeroQuantity() {
            assertThatThrownBy(() -> new StockMovement(StockMovement.MovementType.INFLOW, 0, NOW, "REF"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("positive");
        }

        @Test
        @DisplayName("negatif miktarda exception fırlatmalı")
        void throwsOnNegativeQuantity() {
            assertThatThrownBy(() -> new StockMovement(StockMovement.MovementType.INFLOW, -1, NOW, "REF"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("null tarihte exception fırlatmalı")
        void throwsOnNullDate() {
            assertThatThrownBy(() -> new StockMovement(StockMovement.MovementType.INFLOW, 10, null, "REF"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("null tipta exception fırlatmalı")
        void throwsOnNullType() {
            assertThatThrownBy(() -> new StockMovement(null, 10, NOW, "REF"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("getSignedQuantity")
    class GetSignedQuantity {

        @Test
        @DisplayName("INFLOW için pozitif döndürmeli")
        void returnsPositiveForInflow() {
            StockMovement movement = new StockMovement(StockMovement.MovementType.INFLOW, 100, NOW, "REF");
            assertThat(movement.getSignedQuantity()).isEqualTo(100);
        }

        @Test
        @DisplayName("COLLECTION için negatif döndürmeli")
        void returnsNegativeForCollection() {
            StockMovement movement = new StockMovement(StockMovement.MovementType.COLLECTION, 50, NOW, "REF");
            assertThat(movement.getSignedQuantity()).isEqualTo(-50);
        }

        @Test
        @DisplayName("ADJUSTMENT için negatif döndürmeli")
        void returnsNegativeForAdjustment() {
            StockMovement movement = new StockMovement(StockMovement.MovementType.ADJUSTMENT, 10, NOW, "REF");
            assertThat(movement.getSignedQuantity()).isEqualTo(-10);
        }
    }
}
