package ardaaydinkilinc.Cam_Sise.shared.domain.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("TaxId Tests")
class TaxIdTest {

    @Nested
    @DisplayName("constructor")
    class Constructor {

        @Test
        @DisplayName("geçerli 10 haneli vergi kimliğini kabul etmeli")
        void acceptsValidTaxId() {
            TaxId taxId = new TaxId("1234567890");
            assertThat(taxId.value()).isEqualTo("1234567890");
        }

        @Test
        @DisplayName("null değerde exception fırlatmalı")
        void throwsOnNull() {
            assertThatThrownBy(() -> new TaxId(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("10 digits");
        }

        @Test
        @DisplayName("10 haneden kısa değerde exception fırlatmalı")
        void throwsOnShortValue() {
            assertThatThrownBy(() -> new TaxId("123456789"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("10 haneden uzun değerde exception fırlatmalı")
        void throwsOnLongValue() {
            assertThatThrownBy(() -> new TaxId("12345678901"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("rakam olmayan karakterde exception fırlatmalı")
        void throwsOnNonDigit() {
            assertThatThrownBy(() -> new TaxId("123456789A"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("formatted")
    class Formatted {

        @Test
        @DisplayName("XXX-XXX-XX-XX formatında döndürmeli")
        void returnsFormattedTaxId() {
            TaxId taxId = new TaxId("1234567890");
            assertThat(taxId.formatted()).isEqualTo("123-456-78-90");
        }
    }
}
