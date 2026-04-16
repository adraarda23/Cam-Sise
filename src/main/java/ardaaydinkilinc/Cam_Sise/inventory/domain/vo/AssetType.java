package ardaaydinkilinc.Cam_Sise.inventory.domain.vo;

/**
 * Type of asset being tracked
 */
public enum AssetType {
    PALLET("Palet"),
    SEPARATOR("Separatör");

    private final String displayName;

    AssetType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
