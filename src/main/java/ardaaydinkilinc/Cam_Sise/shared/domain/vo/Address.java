package ardaaydinkilinc.Cam_Sise.shared.domain.vo;

import ardaaydinkilinc.Cam_Sise.shared.domain.base.ValueObject;

/**
 * Address value object
 */
public record Address(
        String street,
        String city,
        String province,
        String postalCode,
        String country
) implements ValueObject {

    public Address {
        if (city == null || city.isBlank()) {
            throw new IllegalArgumentException("City cannot be blank");
        }
        if (country == null || country.isBlank()) {
            throw new IllegalArgumentException("Country cannot be blank");
        }
    }

    public String getFullAddress() {
        return String.format("%s, %s, %s %s, %s",
                street != null ? street : "",
                city,
                province != null ? province : "",
                postalCode != null ? postalCode : "",
                country
        ).replaceAll(",\\s*,", ",").trim();
    }
}
