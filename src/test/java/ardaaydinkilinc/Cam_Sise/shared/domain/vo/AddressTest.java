package ardaaydinkilinc.Cam_Sise.shared.domain.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Address Tests")
class AddressTest {

    @Nested
    @DisplayName("constructor")
    class Constructor {

        @Test
        @DisplayName("tüm alanlar geçerli olduğunda oluşturulabilmeli")
        void acceptsValidAddress() {
            Address address = new Address("Atatürk Cad. No:1", "İstanbul", "İstanbul", "34000", "Türkiye");
            assertThat(address.city()).isEqualTo("İstanbul");
            assertThat(address.country()).isEqualTo("Türkiye");
        }

        @Test
        @DisplayName("opsiyonel alanlar null olabilmeli")
        void acceptsNullOptionalFields() {
            assertThatCode(() -> new Address(null, "Ankara", null, null, "Türkiye"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("city null ise exception fırlatmalı")
        void throwsOnNullCity() {
            assertThatThrownBy(() -> new Address("Cadde", null, "İl", "12345", "Türkiye"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("City");
        }

        @Test
        @DisplayName("city boş string ise exception fırlatmalı")
        void throwsOnBlankCity() {
            assertThatThrownBy(() -> new Address("Cadde", "", "İl", "12345", "Türkiye"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("City");
        }

        @Test
        @DisplayName("city sadece boşluktan oluşuyorsa exception fırlatmalı")
        void throwsOnWhitespaceCity() {
            assertThatThrownBy(() -> new Address("Cadde", "   ", "İl", "12345", "Türkiye"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("City");
        }

        @Test
        @DisplayName("country null ise exception fırlatmalı")
        void throwsOnNullCountry() {
            assertThatThrownBy(() -> new Address("Cadde", "İstanbul", "İl", "12345", null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Country");
        }

        @Test
        @DisplayName("country boş string ise exception fırlatmalı")
        void throwsOnBlankCountry() {
            assertThatThrownBy(() -> new Address("Cadde", "İstanbul", "İl", "12345", ""))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Country");
        }
    }

    @Nested
    @DisplayName("getFullAddress")
    class GetFullAddress {

        @Test
        @DisplayName("tüm alanlar dolu olduğunda tam adres döndürmeli")
        void returnsFullAddressWhenAllFieldsPresent() {
            Address address = new Address("Atatürk Cad. No:1", "İstanbul", "İstanbul", "34000", "Türkiye");
            String full = address.getFullAddress();
            assertThat(full).contains("Atatürk Cad. No:1")
                    .contains("İstanbul")
                    .contains("34000")
                    .contains("Türkiye");
        }

        @Test
        @DisplayName("street null olduğunda çalışmalı")
        void worksWithNullStreet() {
            Address address = new Address(null, "Ankara", "Ankara", "06000", "Türkiye");
            assertThatCode(address::getFullAddress).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("province null olduğunda çalışmalı")
        void worksWithNullProvince() {
            Address address = new Address("Cadde", "İzmir", null, "35000", "Türkiye");
            assertThatCode(address::getFullAddress).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("postalCode null olduğunda çalışmalı")
        void worksWithNullPostalCode() {
            Address address = new Address("Cadde", "Bursa", "Bursa", null, "Türkiye");
            assertThatCode(address::getFullAddress).doesNotThrowAnyException();
        }
    }
}
