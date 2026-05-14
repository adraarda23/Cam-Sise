package ardaaydinkilinc.Cam_Sise.inventory.domain;

import ardaaydinkilinc.Cam_Sise.inventory.domain.vo.AssetType;
import ardaaydinkilinc.Cam_Sise.inventory.domain.vo.StockMovement.MovementType;
import ardaaydinkilinc.Cam_Sise.shared.domain.base.Entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Persistent record of a single stock movement (inflow or collection).
 * Created automatically by FillerStockService whenever stock changes.
 * Forms the time-series foundation used by anomaly detection, loss-rate
 * recalculation, and stock forecasting.
 */
@jakarta.persistence.Entity
@Table(
        name = "stock_movement_history",
        indexes = {
                @Index(name = "idx_smh_filler_asset_time", columnList = "filler_id, asset_type, occurred_at"),
                @Index(name = "idx_smh_occurred_at", columnList = "occurred_at")
        }
)
@Getter
@NoArgsConstructor
public class StockMovementHistory extends Entity<Long> {

    @Column(name = "filler_id", nullable = false)
    private Long fillerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_type", nullable = false)
    private AssetType assetType;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false)
    private MovementType movementType;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "quantity_after", nullable = false)
    private Integer quantityAfter;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @Column(name = "reference_id")
    private String referenceId;

    public static StockMovementHistory inflow(
            Long fillerId,
            AssetType assetType,
            int quantity,
            int quantityAfter,
            String referenceId,
            LocalDateTime occurredAt
    ) {
        return create(fillerId, assetType, MovementType.INFLOW, quantity, quantityAfter, referenceId, occurredAt);
    }

    public static StockMovementHistory collection(
            Long fillerId,
            AssetType assetType,
            int quantity,
            int quantityAfter,
            String referenceId,
            LocalDateTime occurredAt
    ) {
        return create(fillerId, assetType, MovementType.COLLECTION, quantity, quantityAfter, referenceId, occurredAt);
    }

    public static StockMovementHistory adjustment(
            Long fillerId,
            AssetType assetType,
            int quantity,
            int quantityAfter,
            String referenceId,
            LocalDateTime occurredAt
    ) {
        return create(fillerId, assetType, MovementType.ADJUSTMENT, quantity, quantityAfter, referenceId, occurredAt);
    }

    private static StockMovementHistory create(
            Long fillerId,
            AssetType assetType,
            MovementType type,
            int quantity,
            int quantityAfter,
            String referenceId,
            LocalDateTime occurredAt
    ) {
        if (fillerId == null) {
            throw new IllegalArgumentException("fillerId is required");
        }
        if (assetType == null) {
            throw new IllegalArgumentException("assetType is required");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Movement quantity must be positive");
        }
        if (quantityAfter < 0) {
            throw new IllegalArgumentException("quantityAfter cannot be negative");
        }
        if (occurredAt == null) {
            throw new IllegalArgumentException("occurredAt is required");
        }

        StockMovementHistory m = new StockMovementHistory();
        m.fillerId = fillerId;
        m.assetType = assetType;
        m.movementType = type;
        m.quantity = quantity;
        m.quantityAfter = quantityAfter;
        m.referenceId = referenceId;
        m.occurredAt = occurredAt;
        return m;
    }

    /**
     * Signed delta: positive for INFLOW, negative for COLLECTION/ADJUSTMENT.
     */
    public int signedQuantity() {
        return switch (movementType) {
            case INFLOW -> quantity;
            case COLLECTION, ADJUSTMENT -> -quantity;
        };
    }
}
