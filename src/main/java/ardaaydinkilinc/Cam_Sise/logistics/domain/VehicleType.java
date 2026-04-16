package ardaaydinkilinc.Cam_Sise.logistics.domain;

import ardaaydinkilinc.Cam_Sise.logistics.domain.event.VehicleTypeCapacityUpdated;
import ardaaydinkilinc.Cam_Sise.logistics.domain.event.VehicleTypeCreated;
import ardaaydinkilinc.Cam_Sise.logistics.domain.event.VehicleTypeDeactivated;
import ardaaydinkilinc.Cam_Sise.logistics.domain.vo.Capacity;
import ardaaydinkilinc.Cam_Sise.shared.domain.base.AggregateRoot;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * VehicleType aggregate root
 * Catalog of vehicle types that can be used for collections
 */
@Entity
@Table(name = "vehicle_types")
@Getter
@NoArgsConstructor
public class VehicleType extends AggregateRoot<Long> {

    @Column(name = "pool_operator_id", nullable = false)
    private Long poolOperatorId;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "pallets", column = @Column(name = "capacity_pallets")),
            @AttributeOverride(name = "separators", column = @Column(name = "capacity_separators"))
    })
    private Capacity capacity;

    @Column(nullable = false)
    private Boolean active;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Factory method to create a new vehicle type
     */
    public static VehicleType create(
            Long poolOperatorId,
            String name,
            String description,
            Capacity capacity
    ) {
        VehicleType vehicleType = new VehicleType();
        vehicleType.poolOperatorId = poolOperatorId;
        vehicleType.name = name;
        vehicleType.description = description;
        vehicleType.capacity = capacity;
        vehicleType.active = true;
        vehicleType.createdAt = LocalDateTime.now();
        vehicleType.updatedAt = LocalDateTime.now();

        vehicleType.addDomainEvent(new VehicleTypeCreated(
                poolOperatorId,
                name,
                capacity,
                LocalDateTime.now()
        ));

        return vehicleType;
    }

    /**
     * Update capacity
     */
    public void updateCapacity(Capacity newCapacity) {
        this.capacity = newCapacity;
        this.updatedAt = LocalDateTime.now();

        addDomainEvent(new VehicleTypeCapacityUpdated(
                this.id,
                this.poolOperatorId,
                newCapacity,
                LocalDateTime.now()
        ));
    }

    /**
     * Deactivate vehicle type
     */
    public void deactivate() {
        if (!this.active) {
            throw new IllegalStateException("Vehicle type is already inactive");
        }
        this.active = false;
        this.updatedAt = LocalDateTime.now();

        addDomainEvent(new VehicleTypeDeactivated(
                this.id,
                this.poolOperatorId,
                this.name,
                LocalDateTime.now()
        ));
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
