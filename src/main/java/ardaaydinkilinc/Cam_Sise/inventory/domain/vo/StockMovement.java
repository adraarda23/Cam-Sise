package ardaaydinkilinc.Cam_Sise.inventory.domain.vo;

import ardaaydinkilinc.Cam_Sise.shared.domain.base.ValueObject;

import java.time.LocalDateTime;

/**
 * Stock movement value object - represents a single inflow or collection event
 */
public record StockMovement(
        MovementType type,
        int quantity,
        LocalDateTime occurredAt,
        String referenceId  // CollectionPlanId or InflowId
) implements ValueObject {

    public enum MovementType {
        INFLOW,      // Asset entering filler from glass manufacturer
        COLLECTION,  // Asset collected from filler by pool operator
        ADJUSTMENT   // Manual adjustment
    }

    public StockMovement {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (occurredAt == null) {
            throw new IllegalArgumentException("Occurred date cannot be null");
        }
        if (type == null) {
            throw new IllegalArgumentException("Movement type cannot be null");
        }
    }

    /**
     * Get the signed quantity (positive for INFLOW, negative for COLLECTION/ADJUSTMENT)
     */
    public int getSignedQuantity() {
        return switch (type) {
            case INFLOW -> quantity;
            case COLLECTION, ADJUSTMENT -> -quantity;
        };
    }
}
