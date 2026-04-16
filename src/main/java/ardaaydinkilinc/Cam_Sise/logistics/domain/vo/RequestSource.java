package ardaaydinkilinc.Cam_Sise.logistics.domain.vo;

/**
 * Source of a collection request
 */
public enum RequestSource {
    AUTO_THRESHOLD("Automatically generated when stock threshold exceeded"),
    MANUAL_CUSTOMER("Manually created by filler customer");

    private final String description;

    RequestSource(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
