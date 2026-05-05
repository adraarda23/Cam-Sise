package ardaaydinkilinc.Cam_Sise.logistics.domain.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("DriverInfo Tests")
class DriverInfoTest {

    @Nested
    @DisplayName("constructor")
    class Constructor {

        @Test
        @DisplayName("geçerli tüm alanlarla oluşturulabilmeli")
        void acceptsValidDriverInfo() {
            DriverInfo info = new DriverInfo("Ali Demir", "B99990", "05550001111");
            assertThat(info.name()).isEqualTo("Ali Demir");
            assertThat(info.licenseNumber()).isEqualTo("B99990");
            assertThat(info.phone()).isEqualTo("05550001111");
        }

        @Test
        @DisplayName("phone null ise geçerlidir (opsiyonel)")
        void acceptsNullPhone() {
            assertThatCode(() -> new DriverInfo("Mehmet Yılmaz", "ABC123", null))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("name null ise exception fırlatmalı")
        void throwsOnNullName() {
            assertThatThrownBy(() -> new DriverInfo(null, "ABC123", null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("name");
        }

        @Test
        @DisplayName("name boş string ise exception fırlatmalı")
        void throwsOnBlankName() {
            assertThatThrownBy(() -> new DriverInfo("", "ABC123", null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("name");
        }

        @Test
        @DisplayName("name sadece boşluktan oluşuyorsa exception fırlatmalı")
        void throwsOnWhitespaceName() {
            assertThatThrownBy(() -> new DriverInfo("   ", "ABC123", null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("name");
        }

        @Test
        @DisplayName("licenseNumber null ise exception fırlatmalı")
        void throwsOnNullLicenseNumber() {
            assertThatThrownBy(() -> new DriverInfo("Ali", null, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("license");
        }

        @Test
        @DisplayName("lisans numarası 5 karakter (çok kısa) olursa exception fırlatmalı")
        void throwsOnTooShortLicenseNumber() {
            assertThatThrownBy(() -> new DriverInfo("Ali", "AB123", null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("license");
        }

        @Test
        @DisplayName("lisans numarası 13 karakter (çok uzun) olursa exception fırlatmalı")
        void throwsOnTooLongLicenseNumber() {
            assertThatThrownBy(() -> new DriverInfo("Ali", "ABCDEFGHIJK12", null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("license");
        }

        @Test
        @DisplayName("lisans numarası küçük harf içeriyorsa exception fırlatmalı")
        void throwsOnLowercaseLicenseNumber() {
            assertThatThrownBy(() -> new DriverInfo("Ali", "abc123", null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("license");
        }

        @Test
        @DisplayName("phone geçersiz format ise exception fırlatmalı")
        void throwsOnInvalidPhone() {
            assertThatThrownBy(() -> new DriverInfo("Ali", "ABC123", "123"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("phone");
        }
    }
}
