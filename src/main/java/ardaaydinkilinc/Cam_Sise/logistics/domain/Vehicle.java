package ardaaydinkilinc.Cam_Sise.logistics.domain;

import ardaaydinkilinc.Cam_Sise.logistics.domain.event.*;
import ardaaydinkilinc.Cam_Sise.logistics.domain.vo.DriverInfo;
import ardaaydinkilinc.Cam_Sise.logistics.domain.vo.VehicleStatus;
import ardaaydinkilinc.Cam_Sise.shared.domain.base.AggregateRoot;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Vehicle aggregate root
 * Represents a collection vehicle
 */
@Entity
@Table(name = "vehicles")
@Getter
@NoArgsConstructor
public class Vehicle extends AggregateRoot<Long> {

    @Column(name = "depot_id", nullable = false)
    private Long depotId;

    @Column(name = "vehicle_type_id", nullable = false)
    private Long vehicleTypeId;

    @Column(nullable = false, unique = true)
    private String plateNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VehicleStatus status;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "name", column = @Column(name = "driver_name")),
            @AttributeOverride(name = "licenseNumber", column = @Column(name = "driver_license")),
            @AttributeOverride(name = "phone", column = @Column(name = "driver_phone"))
    })
    private DriverInfo currentDriver;

    @Column(name = "current_collection_plan_id")
    private Long currentCollectionPlanId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Factory method to register a new vehicle
     */
    public static Vehicle register(
            Long depotId,
            Long vehicleTypeId,
            String plateNumber
    ) {
        Vehicle vehicle = new Vehicle();
        vehicle.depotId = depotId;
        vehicle.vehicleTypeId = vehicleTypeId;
        vehicle.plateNumber = plateNumber;
        vehicle.status = VehicleStatus.AVAILABLE;
        vehicle.createdAt = LocalDateTime.now();
        vehicle.updatedAt = LocalDateTime.now();

        vehicle.addDomainEvent(new VehicleRegistered(
                depotId,
                vehicleTypeId,
                plateNumber,
                LocalDateTime.now()
        ));

        return vehicle;
    }

    /**
     * Assign vehicle to a collection plan (without driver info)
     */
    public void assignToPlan(Long collectionPlanId) {
        if (this.status != VehicleStatus.AVAILABLE) {
            throw new IllegalStateException("Vehicle is not available for assignment");
        }

        this.currentCollectionPlanId = collectionPlanId;
        this.status = VehicleStatus.ON_ROUTE;
        this.updatedAt = LocalDateTime.now();

        addDomainEvent(new VehicleAssignedToRoute(
                this.id,
                collectionPlanId,
                null,
                LocalDateTime.now()
        ));
    }

    /**
     * Assign vehicle to a collection route (with driver info)
     */
    public void assignToRoute(Long collectionPlanId, DriverInfo driver) {
        if (this.status != VehicleStatus.AVAILABLE) {
            throw new IllegalStateException("Vehicle is not available for assignment");
        }

        this.currentCollectionPlanId = collectionPlanId;
        this.currentDriver = driver;
        this.status = VehicleStatus.ON_ROUTE;
        this.updatedAt = LocalDateTime.now();

        addDomainEvent(new VehicleAssignedToRoute(
                this.id,
                collectionPlanId,
                driver,
                LocalDateTime.now()
        ));
    }

    /**
     * Vehicle departs from depot
     */
    public void departFromDepot() {
        if (this.status != VehicleStatus.ON_ROUTE) {
            throw new IllegalStateException("Vehicle must be assigned to route before departure");
        }

        addDomainEvent(new VehicleDepartedFromDepot(
                this.id,
                this.depotId,
                this.currentCollectionPlanId,
                LocalDateTime.now()
        ));
    }

    /**
     * Vehicle returns to depot
     */
    public void returnToDepot() {
        if (this.status != VehicleStatus.ON_ROUTE) {
            throw new IllegalStateException("Vehicle is not on route");
        }

        this.status = VehicleStatus.AVAILABLE;
        this.currentDriver = null;
        this.currentCollectionPlanId = null;
        this.updatedAt = LocalDateTime.now();

        addDomainEvent(new VehicleReturnedToDepot(
                this.id,
                this.depotId,
                LocalDateTime.now()
        ));
    }

    /**
     * Change vehicle status manually (with business rule validation)
     */
    public void changeStatus(VehicleStatus newStatus) {
        // Cannot manually change to ON_ROUTE (must use assignToRoute)
        if (newStatus == VehicleStatus.ON_ROUTE) {
            throw new IllegalStateException(
                "Cannot manually change status to ON_ROUTE. Use assignToRoute() method."
            );
        }

        // Cannot manually change from ON_ROUTE to AVAILABLE (must use returnToDepot)
        if (this.status == VehicleStatus.ON_ROUTE && newStatus == VehicleStatus.AVAILABLE) {
            throw new IllegalStateException(
                "Cannot manually change status from ON_ROUTE to AVAILABLE. Use returnToDepot() method."
            );
        }

        // Cannot deactivate vehicle while on route
        if (this.status == VehicleStatus.ON_ROUTE && newStatus == VehicleStatus.INACTIVE) {
            throw new IllegalStateException("Cannot deactivate vehicle while on route");
        }

        VehicleStatus oldStatus = this.status;
        this.status = newStatus;
        this.updatedAt = LocalDateTime.now();

        // If leaving ON_ROUTE status (e.g., going to MAINTENANCE due to accident)
        // Clear the current collection plan reference
        if (oldStatus == VehicleStatus.ON_ROUTE && newStatus != VehicleStatus.ON_ROUTE) {
            this.currentCollectionPlanId = null;
            this.currentDriver = null;
        }

        addDomainEvent(new VehicleStatusChanged(
                this.id,
                oldStatus,
                newStatus,
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
