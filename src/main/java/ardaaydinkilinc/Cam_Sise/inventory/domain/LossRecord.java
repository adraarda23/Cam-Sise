package ardaaydinkilinc.Cam_Sise.inventory.domain;

import ardaaydinkilinc.Cam_Sise.inventory.domain.event.EstimatedLossRateCalculated;
import ardaaydinkilinc.Cam_Sise.inventory.domain.event.LossRecordUpdated;
import ardaaydinkilinc.Cam_Sise.inventory.domain.vo.AssetType;
import ardaaydinkilinc.Cam_Sise.inventory.domain.vo.LossRate;
import ardaaydinkilinc.Cam_Sise.inventory.domain.vo.Period;
import ardaaydinkilinc.Cam_Sise.shared.domain.base.AggregateRoot;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * LossRecord aggregate root
 * Tracks the loss/damage rate of assets at a filler location
 */
@Entity
@Table(name = "loss_records")
@Getter
@NoArgsConstructor
public class LossRecord extends AggregateRoot<Long> {

    @Column(name = "filler_id", nullable = false)
    private Long fillerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_type", nullable = false)
    private AssetType assetType;

    /**
     * Actual loss rate reported by the filler (nullable)
     */
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "percentage", column = @Column(name = "actual_loss_rate"))
    })
    private LossRate actualRate;

    /**
     * Estimated loss rate calculated by moving average
     */
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "percentage", column = @Column(name = "estimated_loss_rate"))
    })
    private LossRate estimatedRate;

    /**
     * Period for which this loss rate is calculated
     */
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "startDate", column = @Column(name = "period_start_date")),
            @AttributeOverride(name = "endDate", column = @Column(name = "period_end_date"))
    })
    private Period calculationPeriod;

    @Column(nullable = false)
    private LocalDateTime lastUpdated;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Factory method to create initial loss record with estimated rate
     */
    public static LossRecord createWithEstimate(
            Long fillerId,
            AssetType assetType,
            LossRate estimatedRate,
            Period period
    ) {
        LossRecord record = new LossRecord();
        record.fillerId = fillerId;
        record.assetType = assetType;
        record.estimatedRate = estimatedRate;
        record.calculationPeriod = period;
        record.lastUpdated = LocalDateTime.now();
        record.createdAt = LocalDateTime.now();

        record.addDomainEvent(new EstimatedLossRateCalculated(
                fillerId,
                assetType,
                estimatedRate.percentage(),
                period,
                LocalDateTime.now()
        ));

        return record;
    }

    /**
     * Update actual loss rate (reported by filler)
     */
    public void updateActualRate(LossRate newActualRate) {
        this.actualRate = newActualRate;
        this.lastUpdated = LocalDateTime.now();

        addDomainEvent(new LossRecordUpdated(
                this.id,
                this.fillerId,
                this.assetType,
                newActualRate.percentage(),
                this.calculationPeriod,
                LocalDateTime.now()
        ));
    }

    /**
     * Recalculate estimated rate using moving average
     */
    public void recalculateEstimatedRate(LossRate newEstimatedRate, Period newPeriod) {
        this.estimatedRate = newEstimatedRate;
        this.calculationPeriod = newPeriod;
        this.lastUpdated = LocalDateTime.now();

        addDomainEvent(new EstimatedLossRateCalculated(
                this.fillerId,
                this.assetType,
                newEstimatedRate.percentage(),
                newPeriod,
                LocalDateTime.now()
        ));
    }

    /**
     * Get the best available loss rate (actual if available, otherwise estimated)
     */
    public LossRate getBestAvailableRate() {
        return actualRate != null ? actualRate : estimatedRate;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (lastUpdated == null) {
            lastUpdated = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        lastUpdated = LocalDateTime.now();
    }
}
