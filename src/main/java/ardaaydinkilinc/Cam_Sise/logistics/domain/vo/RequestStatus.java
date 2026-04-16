package ardaaydinkilinc.Cam_Sise.logistics.domain.vo;

/**
 * Status of a collection request
 */
public enum RequestStatus {
    PENDING("Waiting for approval"),
    APPROVED("Approved and ready for planning"),
    REJECTED("Rejected by staff"),
    CANCELLED("Cancelled"),
    SCHEDULED("Included in a collection plan"),
    COMPLETED("Collection completed");

    private final String description;

    RequestStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean canTransitionTo(RequestStatus newStatus) {
        return switch (this) {
            case PENDING -> newStatus == APPROVED || newStatus == REJECTED || newStatus == CANCELLED;
            case APPROVED -> newStatus == SCHEDULED || newStatus == CANCELLED;
            case SCHEDULED -> newStatus == COMPLETED || newStatus == CANCELLED;
            case REJECTED, CANCELLED, COMPLETED -> false;
        };
    }
}
