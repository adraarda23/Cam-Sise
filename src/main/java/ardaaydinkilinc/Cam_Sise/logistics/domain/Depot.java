package ardaaydinkilinc.Cam_Sise.logistics.domain;

import ardaaydinkilinc.Cam_Sise.logistics.domain.event.DepotCreated;
import ardaaydinkilinc.Cam_Sise.logistics.domain.event.VehicleAddedToDepot;
import ardaaydinkilinc.Cam_Sise.logistics.domain.event.VehicleRemovedFromDepot;
import ardaaydinkilinc.Cam_Sise.shared.domain.base.AggregateRoot;
import ardaaydinkilinc.Cam_Sise.shared.domain.vo.Address;
import ardaaydinkilinc.Cam_Sise.shared.domain.vo.GeoCoordinates;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Depot aggregate root
 * Represents a warehouse/depot from which collection vehicles depart
 */
@Entity
@Table(name = "depots")
@Getter
@NoArgsConstructor
public class Depot extends AggregateRoot<Long> {

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

    @ElementCollection
    @CollectionTable(name = "depot_vehicles", joinColumns = @JoinColumn(name = "depot_id"))
    @Column(name = "vehicle_id")
    private List<Long> vehicleIds = new ArrayList<>();

    @Column(nullable = false)
    private Boolean active;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Factory method to create a new depot
     */
    public static Depot create(
            Long poolOperatorId,
            String name,
            Address address,
            GeoCoordinates location
    ) {
        Depot depot = new Depot();
        depot.poolOperatorId = poolOperatorId;
        depot.name = name;
        depot.address = address;
        depot.location = location;
        depot.active = true;
        depot.createdAt = LocalDateTime.now();
        depot.updatedAt = LocalDateTime.now();

        depot.addDomainEvent(new DepotCreated(
                poolOperatorId,
                name,
                location,
                LocalDateTime.now()
        ));

        return depot;
    }

    /**
     * Add a vehicle to this depot
     */
    public void addVehicle(Long vehicleId) {
        if (this.vehicleIds.contains(vehicleId)) {
            throw new IllegalArgumentException("Vehicle already assigned to this depot");
        }
        this.vehicleIds.add(vehicleId);
        this.updatedAt = LocalDateTime.now();

        addDomainEvent(new VehicleAddedToDepot(
                this.id,
                this.poolOperatorId,
                vehicleId,
                LocalDateTime.now()
        ));
    }

    /**
     * Remove a vehicle from this depot
     */
    public void removeVehicle(Long vehicleId) {
        if (!this.vehicleIds.contains(vehicleId)) {
            throw new IllegalArgumentException("Vehicle not assigned to this depot");
        }
        this.vehicleIds.remove(vehicleId);
        this.updatedAt = LocalDateTime.now();

        addDomainEvent(new VehicleRemovedFromDepot(
                this.id,
                this.poolOperatorId,
                vehicleId,
                LocalDateTime.now()
        ));
    }

    /**
     * Get number of vehicles at this depot
     */
    public int getVehicleCount() {
        return vehicleIds.size();
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
