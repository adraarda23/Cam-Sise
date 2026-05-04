package ardaaydinkilinc.Cam_Sise.core.domain;

import ardaaydinkilinc.Cam_Sise.core.domain.event.PoolOperatorActivated;
import ardaaydinkilinc.Cam_Sise.core.domain.event.PoolOperatorDeactivated;
import ardaaydinkilinc.Cam_Sise.core.domain.event.PoolOperatorRegistered;
import ardaaydinkilinc.Cam_Sise.core.domain.event.PoolOperatorUpdated;
import ardaaydinkilinc.Cam_Sise.shared.domain.base.AggregateRoot;
import ardaaydinkilinc.Cam_Sise.shared.domain.vo.ContactInfo;
import ardaaydinkilinc.Cam_Sise.shared.domain.vo.TaxId;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Pool Operator aggregate root (Tenant)
 * Represents a company that uses the system to manage their pallet/separator pool
 */
@Entity
@Table(name = "pool_operators")
@Getter
@NoArgsConstructor
public class PoolOperator extends AggregateRoot<Long> {

    @Column(nullable = false)
    private String companyName;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "value", column = @Column(name = "tax_id", unique = true, nullable = false))
    })
    private TaxId taxId;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "phone", column = @Column(name = "contact_phone")),
            @AttributeOverride(name = "email", column = @Column(name = "contact_email")),
            @AttributeOverride(name = "contactPersonName", column = @Column(name = "contact_person_name"))
    })
    private ContactInfo contactInfo;

    @Column(nullable = false)
    private Boolean active;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Factory method to create a new PoolOperator
     */
    public static PoolOperator register(
            String companyName,
            TaxId taxId,
            ContactInfo contactInfo
    ) {
        PoolOperator operator = new PoolOperator();
        operator.companyName = companyName;
        operator.taxId = taxId;
        operator.contactInfo = contactInfo;
        operator.active = true;
        operator.createdAt = LocalDateTime.now();
        operator.updatedAt = LocalDateTime.now();

        operator.addDomainEvent(new PoolOperatorRegistered(
                companyName,
                taxId.value(),
                LocalDateTime.now()
        ));

        return operator;
    }

    /**
     * Activate the pool operator
     */
    public void activate() {
        if (this.active) {
            throw new IllegalStateException("Pool operator is already active");
        }
        this.active = true;
        this.updatedAt = LocalDateTime.now();

        addDomainEvent(new PoolOperatorActivated(
                this.id,
                this.companyName,
                LocalDateTime.now()
        ));
    }

    /**
     * Deactivate the pool operator
     */
    public void deactivate() {
        if (!this.active) {
            throw new IllegalStateException("Pool operator is already inactive");
        }
        this.active = false;
        this.updatedAt = LocalDateTime.now();

        addDomainEvent(new PoolOperatorDeactivated(
                this.id,
                this.companyName,
                LocalDateTime.now()
        ));
    }

    /**
     * Update contact information
     */
    public void updateContactInfo(ContactInfo newContactInfo) {
        this.contactInfo = newContactInfo;
        this.updatedAt = LocalDateTime.now();
        addDomainEvent(new PoolOperatorUpdated(this.id, "contactInfo", LocalDateTime.now()));
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
