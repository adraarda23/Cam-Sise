package ardaaydinkilinc.Cam_Sise.logistics.domain.vo;

import ardaaydinkilinc.Cam_Sise.shared.domain.base.ValueObject;

/**
 * Driver information value object
 */
public record DriverInfo(
        String name,
        String licenseNumber,
        String phone
) implements ValueObject {

    private static final String LICENSE_PATTERN = "^[A-Z0-9]{6,12}$";
    private static final String PHONE_PATTERN = "^\\+?[0-9]{10,15}$";

    public DriverInfo {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Driver name cannot be blank");
        }
        if (licenseNumber == null || !licenseNumber.matches(LICENSE_PATTERN)) {
            throw new IllegalArgumentException("Invalid license number format. Expected: 6-12 alphanumeric characters");
        }
        if (phone != null && !phone.matches(PHONE_PATTERN)) {
            throw new IllegalArgumentException("Invalid phone format");
        }
    }
}
