package ardaaydinkilinc.Cam_Sise.core.domain;

import ardaaydinkilinc.Cam_Sise.core.domain.event.FillerActivated;
import ardaaydinkilinc.Cam_Sise.core.domain.event.FillerDeactivated;
import ardaaydinkilinc.Cam_Sise.core.domain.event.FillerRegistered;
import ardaaydinkilinc.Cam_Sise.shared.domain.base.AggregateRoot;
import ardaaydinkilinc.Cam_Sise.shared.domain.vo.Address;
import ardaaydinkilinc.Cam_Sise.shared.domain.vo.ContactInfo;
import ardaaydinkilinc.Cam_Sise.shared.domain.vo.GeoCoordinates;
import ardaaydinkilinc.Cam_Sise.shared.domain.vo.TaxId;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Filler (Dolumcu) aggregate root
 * Represents a filling company that receives pallets/separators and returns them
 */
@Entity
@Table(name = "fillers")
@Getter
@NoArgsConstructor
public class Filler extends AggregateRoot<Long> {

    @Column(name = "pool_operator_id", nullable = false)
    private Long poolOperatorId;

    @Column(nullable = false)
    private String name;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "street", column = @Column(name = "address_street")),
            @AttributeOverride(name = "city", column = @Column(name = "address_city")),
            @AttributeOverride(name = "province", column = @Column(name = "address_province")),
            @AttributeOverride(name = "postalCode", column = @Column(name = "address_postal_code")),
            @AttributeOverride(name = "country", column = @Column(name = "address_country"))
    })
    private Address address;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "latitude", column = @Column(name = "location_latitude")),
            @AttributeOverride(name = "longitude", column = @Column(name = "location_longitude"))
    })
    private GeoCoordinates location;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "phone", column = @Column(name = "contact_phone")),
            @AttributeOverride(name = "email", column = @Column(name = "contact_email")),
            @AttributeOverride(name = "contactPersonName", column = @Column(name = "contact_person_name"))
    })
    private ContactInfo contactInfo;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "value", column = @Column(name = "tax_id", unique = true))
    })
    private TaxId taxId;

    @Column(nullable = false)
    private Boolean active;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Factory method to register a new filler
     */
    public static Filler register(
            Long poolOperatorId,
            String name,
            Address address,
            GeoCoordinates location,
            ContactInfo contactInfo,
            TaxId taxId
    ) {
        Filler filler = new Filler();
        filler.poolOperatorId = poolOperatorId;
        filler.name = name;
        filler.address = address;
        filler.location = location;
        filler.contactInfo = contactInfo;
        filler.taxId = taxId;
        filler.active = true;
        filler.createdAt = LocalDateTime.now();
        filler.updatedAt = LocalDateTime.now();

        filler.addDomainEvent(new FillerRegistered(
                poolOperatorId,
                name,
                location,
                LocalDateTime.now()
        ));

        return filler;
    }

    /**
     * Activate the filler
     */
    public void activate() {
        if (this.active) {
            throw new IllegalStateException("Filler is already active");
        }
        this.active = true;
        this.updatedAt = LocalDateTime.now();

        addDomainEvent(new FillerActivated(
                this.id,
                this.poolOperatorId,
                this.name,
                LocalDateTime.now()
        ));
    }

    /**
     * Deactivate the filler
     */
    public void deactivate() {
        if (!this.active) {
            throw new IllegalStateException("Filler is already inactive");
        }
        this.active = false;
        this.updatedAt = LocalDateTime.now();

        addDomainEvent(new FillerDeactivated(
                this.id,
                this.poolOperatorId,
                this.name,
                LocalDateTime.now()
        ));
    }

    /**
     * Update contact information
     */
    public void updateContactInfo(ContactInfo newContactInfo) {
        this.contactInfo = newContactInfo;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Update location
     */
    public void updateLocation(GeoCoordinates newLocation) {
        this.location = newLocation;
        this.updatedAt = LocalDateTime.now();
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
