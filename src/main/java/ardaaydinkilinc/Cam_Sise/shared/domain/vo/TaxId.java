package ardaaydinkilinc.Cam_Sise.shared.domain.vo;

import ardaaydinkilinc.Cam_Sise.shared.domain.base.ValueObject;

/**
 * Turkish Tax ID (Vergi Kimlik Numarası) value object
 */
public record TaxId(String value) implements ValueObject {

    private static final String TAX_ID_PATTERN = "^\\d{10}$";

    public TaxId {
        if (value == null || !value.matches(TAX_ID_PATTERN)) {
            throw new IllegalArgumentException("Tax ID must be exactly 10 digits");
        }
    }

    public String formatted() {
        // Format: XXX-XXX-XX-XX
        return value.substring(0, 3) + "-" +
                value.substring(3, 6) + "-" +
                value.substring(6, 8) + "-" +
                value.substring(8, 10);
    }
}
