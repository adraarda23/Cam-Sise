package ardaaydinkilinc.Cam_Sise.logistics.domain.vo;

/**
 * Status of a vehicle
 */
public enum VehicleStatus {
    AVAILABLE("Available for collection routes"),
    ON_ROUTE("Currently on a collection route"),
    MAINTENANCE("Under maintenance"),
    INACTIVE("Inactive/Decommissioned");

    private final String description;

    VehicleStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
