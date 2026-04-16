package ardaaydinkilinc.Cam_Sise.inventory.domain;

import ardaaydinkilinc.Cam_Sise.inventory.domain.event.AssetCollected;
import ardaaydinkilinc.Cam_Sise.inventory.domain.event.AssetInflowRecorded;
import ardaaydinkilinc.Cam_Sise.inventory.domain.event.StockThresholdExceeded;
import ardaaydinkilinc.Cam_Sise.inventory.domain.vo.AssetType;
import ardaaydinkilinc.Cam_Sise.inventory.domain.vo.LossRate;
import ardaaydinkilinc.Cam_Sise.inventory.domain.vo.StockMovement;
import ardaaydinkilinc.Cam_Sise.shared.domain.base.AggregateRoot;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * FillerStock aggregate root
 * Tracks the stock of a specific asset type at a filler location
 */
@Entity
@Table(name = "filler_stocks", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"filler_id", "asset_type"})
})
@Getter
@NoArgsConstructor
public class FillerStock extends AggregateRoot<Long> {

    @Column(name = "filler_id", nullable = false)
    private Long fillerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_type", nullable = false)
    private AssetType assetType;

    @Column(name = "current_quantity", nullable = false)
    private Integer currentQuantity;

    @Column(name = "threshold_quantity", nullable = false)
    private Integer thresholdQuantity;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "percentage", column = @Column(name = "estimated_loss_rate"))
    })
    private LossRate estimatedLossRate;

    /**
     * Movement history is not stored in this aggregate
     * Use separate StockMovementHistory entity or query DomainEventStore
     */

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Factory method to create initial stock for a filler
     */
    public static FillerStock initialize(
            Long fillerId,
            AssetType assetType,
            Integer thresholdQuantity,
            LossRate estimatedLossRate
    ) {
        FillerStock stock = new FillerStock();
        stock.fillerId = fillerId;
        stock.assetType = assetType;
        stock.currentQuantity = 0;
        stock.thresholdQuantity = thresholdQuantity;
        stock.estimatedLossRate = estimatedLossRate;
        stock.createdAt = LocalDateTime.now();
        stock.updatedAt = LocalDateTime.now();

        return stock;
    }

    /**
     * Record an asset inflow (from glass manufacturer)
     */
    public void recordInflow(int quantity, String referenceId) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Inflow quantity must be positive");
        }

        this.currentQuantity += quantity;
        this.updatedAt = LocalDateTime.now();

        addDomainEvent(new AssetInflowRecorded(
                this.id,
                this.fillerId,
                this.assetType,
                quantity,
                this.currentQuantity,
                referenceId,
                LocalDateTime.now()
        ));

        // Check if threshold is exceeded
        checkThreshold();
    }

    /**
     * Record an asset collection (pool operator picks up assets)
     */
    public void recordCollection(int quantity, String collectionPlanId) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Collection quantity must be positive");
        }
        if (quantity > this.currentQuantity) {
            throw new IllegalArgumentException(
                    String.format("Cannot collect %d items, only %d available", quantity, this.currentQuantity)
            );
        }

        this.currentQuantity -= quantity;
        this.updatedAt = LocalDateTime.now();

        addDomainEvent(new AssetCollected(
                this.id,
                this.fillerId,
                this.assetType,
                quantity,
                this.currentQuantity,
                collectionPlanId,
                LocalDateTime.now()
        ));
    }

    /**
     * Update threshold quantity
     */
    public void updateThreshold(int newThreshold) {
        if (newThreshold < 0) {
            throw new IllegalArgumentException("Threshold cannot be negative");
        }
        this.thresholdQuantity = newThreshold;
        this.updatedAt = LocalDateTime.now();

        // Check if current stock exceeds new threshold
        checkThreshold();
    }

    /**
     * Update estimated loss rate
     */
    public void updateEstimatedLossRate(LossRate newRate) {
        this.estimatedLossRate = newRate;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Check if current quantity exceeds threshold
     */
    private void checkThreshold() {
        if (this.currentQuantity >= this.thresholdQuantity) {
            addDomainEvent(new StockThresholdExceeded(
                    this.id,
                    this.fillerId,
                    this.assetType,
                    this.currentQuantity,
                    this.thresholdQuantity,
                    LocalDateTime.now()
            ));
        }
    }

    /**
     * Calculate estimated available quantity after loss
     */
    public int getEstimatedAvailableQuantity() {
        if (estimatedLossRate == null) {
            return currentQuantity;
        }
        return estimatedLossRate.calculateRemaining(currentQuantity);
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
