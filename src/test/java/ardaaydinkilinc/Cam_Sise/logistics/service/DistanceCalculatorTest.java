package ardaaydinkilinc.Cam_Sise.logistics.service;

import ardaaydinkilinc.Cam_Sise.shared.domain.vo.GeoCoordinates;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("DistanceCalculator Tests")
class DistanceCalculatorTest {

    private DistanceCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new DistanceCalculator();
    }

    @Nested
    @DisplayName("calculateDistance")
    class CalculateDistance {

        @Test
        @DisplayName("from null ise exception fırlatmalı")
        void throwsOnNullFrom() {
            assertThatThrownBy(() -> calculator.calculateDistance(null, new GeoCoordinates(41.0, 29.0)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("null");
        }

        @Test
        @DisplayName("to null ise exception fırlatmalı")
        void throwsOnNullTo() {
            assertThatThrownBy(() -> calculator.calculateDistance(new GeoCoordinates(41.0, 29.0), null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("null");
        }

        @Test
        @DisplayName("geçerli koordinatlar arasındaki mesafeyi hesaplamalı")
        void calculatesDistanceBetweenTwoPoints() {
            // İstanbul → Ankara yaklaşık 350 km
            GeoCoordinates istanbul = new GeoCoordinates(41.0082, 28.9784);
            GeoCoordinates ankara = new GeoCoordinates(39.9334, 32.8597);
            double distance = calculator.calculateDistance(istanbul, ankara);
            assertThat(distance).isBetween(340.0, 370.0);
        }

        @Test
        @DisplayName("aynı noktalar arasındaki mesafe sıfır olmalı")
        void returnsZeroForSamePoint() {
            GeoCoordinates point = new GeoCoordinates(41.0, 29.0);
            assertThat(calculator.calculateDistance(point, point)).isCloseTo(0.0, within(0.001));
        }
    }

    @Nested
    @DisplayName("calculateTotalDistance")
    class CalculateTotalDistance {

        @Test
        @DisplayName("null liste 0.0 döndürmeli")
        void returnsZeroForNullList() {
            assertThat(calculator.calculateTotalDistance(null)).isEqualTo(0.0);
        }

        @Test
        @DisplayName("boş liste 0.0 döndürmeli")
        void returnsZeroForEmptyList() {
            assertThat(calculator.calculateTotalDistance(List.of())).isEqualTo(0.0);
        }

        @Test
        @DisplayName("tek nokta listesi 0.0 döndürmeli")
        void returnsZeroForSinglePoint() {
            assertThat(calculator.calculateTotalDistance(List.of(new GeoCoordinates(41.0, 29.0))))
                    .isEqualTo(0.0);
        }

        @Test
        @DisplayName("iki nokta listesi mesafeyi hesaplamalı")
        void calculatesTwoPointDistance() {
            GeoCoordinates istanbul = new GeoCoordinates(41.0082, 28.9784);
            GeoCoordinates ankara = new GeoCoordinates(39.9334, 32.8597);
            double total = calculator.calculateTotalDistance(List.of(istanbul, ankara));
            assertThat(total).isGreaterThan(0.0);
        }

        @Test
        @DisplayName("üç nokta listesi iki mesafeyi toplamalı")
        void sumsMultipleSegments() {
            GeoCoordinates a = new GeoCoordinates(41.0082, 28.9784);
            GeoCoordinates b = new GeoCoordinates(39.9334, 32.8597);
            GeoCoordinates c = new GeoCoordinates(38.6748, 39.2225);
            double total = calculator.calculateTotalDistance(List.of(a, b, c));
            double ab = calculator.calculateDistance(a, b);
            double bc = calculator.calculateDistance(b, c);
            assertThat(total).isCloseTo(ab + bc, within(0.001));
        }
    }

    @Nested
    @DisplayName("estimateDuration")
    class EstimateDuration {

        @Test
        @DisplayName("100 km için doğru süreyi döndürmeli (120 dk = 50 km/s)")
        void estimatesDurationCorrectly() {
            assertThat(calculator.estimateDuration(100.0)).isEqualTo(120);
        }

        @Test
        @DisplayName("sıfır mesafe için 0 döndürmeli")
        void returnsZeroForZeroDistance() {
            assertThat(calculator.estimateDuration(0.0)).isEqualTo(0);
        }
    }
}
