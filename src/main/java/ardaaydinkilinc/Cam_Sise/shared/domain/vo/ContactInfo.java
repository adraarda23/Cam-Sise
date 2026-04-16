package ardaaydinkilinc.Cam_Sise.shared.domain.vo;

import ardaaydinkilinc.Cam_Sise.shared.domain.base.ValueObject;

/**
 * Contact information value object
 */
public record ContactInfo(
        String phone,
        String email,
        String contactPersonName
) implements ValueObject {

    private static final String PHONE_PATTERN = "^\\+?[0-9]{10,15}$";
    private static final String EMAIL_PATTERN = "^[A-Za-z0-9+_.-]+@(.+)$";

    public ContactInfo {
        if (phone != null && !phone.matches(PHONE_PATTERN)) {
            throw new IllegalArgumentException("Invalid phone format. Expected: +905551234567 or 05551234567");
        }
        if (email != null && !email.matches(EMAIL_PATTERN)) {
            throw new IllegalArgumentException("Invalid email format");
        }
    }
}
