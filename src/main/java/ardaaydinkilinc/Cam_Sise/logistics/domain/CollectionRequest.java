package ardaaydinkilinc.Cam_Sise.logistics.domain;

import ardaaydinkilinc.Cam_Sise.inventory.domain.vo.AssetType;
import ardaaydinkilinc.Cam_Sise.logistics.domain.event.*;
import ardaaydinkilinc.Cam_Sise.logistics.domain.vo.RequestSource;
import ardaaydinkilinc.Cam_Sise.logistics.domain.vo.RequestStatus;
import ardaaydinkilinc.Cam_Sise.shared.domain.base.AggregateRoot;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * CollectionRequest aggregate root
 * Represents a request to collect assets from a filler
 */
@Entity
@Table(name = "collection_requests")
@Getter
@NoArgsConstructor
public class CollectionRequest extends AggregateRoot<Long> {

    @Column(name = "filler_id", nullable = false)
    private Long fillerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_type", nullable = false)
    private AssetType assetType;

    @Column(name = "estimated_quantity", nullable = false)
    private Integer estimatedQuantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestSource source;

    @Column(name = "approved_by_user_id")
    private Long approvedByUserId;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "collection_plan_id")
    private Long collectionPlanId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Factory method for automatic threshold-triggered request
     */
    public static CollectionRequest createAutomatic(
            Long fillerId,
            AssetType assetType,
            Integer estimatedQuantity
    ) {
        CollectionRequest request = new CollectionRequest();
        request.fillerId = fillerId;
        request.assetType = assetType;
        request.estimatedQuantity = estimatedQuantity;
        request.status = RequestStatus.PENDING;
        request.source = RequestSource.AUTO_THRESHOLD;
        request.createdAt = LocalDateTime.now();
        request.updatedAt = LocalDateTime.now();

        request.addDomainEvent(new CollectionRequestCreated(
                fillerId,
                assetType,
                estimatedQuantity,
                RequestSource.AUTO_THRESHOLD,
                LocalDateTime.now()
        ));

        return request;
    }

    /**
     * Factory method for manual customer request
     */
    public static CollectionRequest createManual(
            Long fillerId,
            AssetType assetType,
            Integer estimatedQuantity,
            Long requestingUserId
    ) {
        CollectionRequest request = new CollectionRequest();
        request.fillerId = fillerId;
        request.assetType = assetType;
        request.estimatedQuantity = estimatedQuantity;
        request.status = RequestStatus.PENDING;
        request.source = RequestSource.MANUAL_CUSTOMER;
        request.createdAt = LocalDateTime.now();
        request.updatedAt = LocalDateTime.now();

        request.addDomainEvent(new CollectionRequestCreated(
                fillerId,
                assetType,
                estimatedQuantity,
                RequestSource.MANUAL_CUSTOMER,
                LocalDateTime.now()
        ));

        return request;
    }

    /**
     * Approve the request
     */
    public void approve(Long approvingUserId) {
        if (!status.canTransitionTo(RequestStatus.APPROVED)) {
            throw new IllegalStateException("Cannot approve request in status: " + status);
        }

        this.status = RequestStatus.APPROVED;
        this.approvedByUserId = approvingUserId;
        this.updatedAt = LocalDateTime.now();

        addDomainEvent(new CollectionRequestApproved(
                this.id,
                this.fillerId,
                approvingUserId,
                LocalDateTime.now()
        ));
    }

    /**
     * Reject the request
     */
    public void reject(String reason) {
        if (!status.canTransitionTo(RequestStatus.REJECTED)) {
            throw new IllegalStateException("Cannot reject request in status: " + status);
        }

        this.status = RequestStatus.REJECTED;
        this.rejectionReason = reason;
        this.updatedAt = LocalDateTime.now();

        addDomainEvent(new CollectionRequestRejected(
                this.id,
                this.fillerId,
                reason,
                LocalDateTime.now()
        ));
    }

    /**
     * Cancel the request
     */
    public void cancel() {
        if (!status.canTransitionTo(RequestStatus.CANCELLED)) {
            throw new IllegalStateException("Cannot cancel request in status: " + status);
        }

        this.status = RequestStatus.CANCELLED;
        this.updatedAt = LocalDateTime.now();

        addDomainEvent(new CollectionRequestCancelled(
                this.id,
                this.fillerId,
                LocalDateTime.now()
        ));
    }

    /**
     * Mark as scheduled (included in a collection plan)
     */
    public void schedule(Long planId) {
        if (!status.canTransitionTo(RequestStatus.SCHEDULED)) {
            throw new IllegalStateException("Cannot schedule request in status: " + status);
        }

        this.status = RequestStatus.SCHEDULED;
        this.collectionPlanId = planId;
        this.updatedAt = LocalDateTime.now();
        addDomainEvent(new CollectionRequestScheduled(this.id, this.fillerId, planId, LocalDateTime.now()));
    }

    public void complete() {
        if (!status.canTransitionTo(RequestStatus.COMPLETED)) {
            throw new IllegalStateException("Cannot complete request in status: " + status);
        }

        this.status = RequestStatus.COMPLETED;
        this.updatedAt = LocalDateTime.now();
        addDomainEvent(new CollectionRequestCompleted(this.id, this.fillerId, LocalDateTime.now()));
    }

    public void updateQuantity(Integer newQuantity) {
        if (this.status != RequestStatus.PENDING) {
            throw new IllegalStateException("Can only update quantity for PENDING requests");
        }

        if (newQuantity == null || newQuantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }

        int oldQuantity = this.estimatedQuantity;
        this.estimatedQuantity = newQuantity;
        this.updatedAt = LocalDateTime.now();
        addDomainEvent(new CollectionRequestQuantityUpdated(this.id, this.fillerId, oldQuantity, newQuantity, LocalDateTime.now()));
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
