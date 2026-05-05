package ardaaydinkilinc.Cam_Sise.shared.domain.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Distance Tests")
class DistanceTest {

    @Nested
    @DisplayName("constructor")
    class Constructor {

        @Test
        @DisplayName("geçerli mesafeyi kabul etmeli")
        void acceptsValidDistance() {
            Distance d = new Distance(100.0);
            assertThat(d.kilometers()).isEqualTo(100.0);
        }

        @Test
        @DisplayName("sıfır mesafeyi kabul etmeli")
        void acceptsZero() {
            assertThat(new Distance(0).kilometers()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("negatif mesafede exception fırlatmalı")
        void throwsOnNegative() {
            assertThatThrownBy(() -> new Distance(-1.0))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("toMeters")
    class ToMeters {

        @Test
        @DisplayName("km'yi metreye çevirmeli")
        void convertsKmToMeters() {
            assertThat(new Distance(5.0).toMeters()).isEqualTo(5000.0);
        }
    }

    @Nested
    @DisplayName("add")
    class Add {

        @Test
        @DisplayName("iki mesafeyi toplameli")
        void addsTwoDistances() {
            Distance result = new Distance(10.0).add(new Distance(5.0));
            assertThat(result.kilometers()).isEqualTo(15.0);
        }
    }

    @Nested
    @DisplayName("subtract")
    class Subtract {

        @Test
        @DisplayName("iki mesafeyi çıkarmalı")
        void subtractsDistances() {
            Distance result = new Distance(10.0).subtract(new Distance(3.0));
            assertThat(result.kilometers()).isEqualTo(7.0);
        }

        @Test
        @DisplayName("negatif sonuçta exception fırlatmalı")
        void throwsOnNegativeResult() {
            assertThatThrownBy(() -> new Distance(5.0).subtract(new Distance(10.0)))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
