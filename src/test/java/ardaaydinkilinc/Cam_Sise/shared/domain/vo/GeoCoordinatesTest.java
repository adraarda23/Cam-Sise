package ardaaydinkilinc.Cam_Sise.shared.domain.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("GeoCoordinates Tests")
class GeoCoordinatesTest {

    @Nested
    @DisplayName("constructor")
    class Constructor {

        @Test
        @DisplayName("geçerli koordinatları kabul etmeli")
        void acceptsValidCoordinates() {
            GeoCoordinates geo = new GeoCoordinates(41.0, 29.0);
            assertThat(geo.latitude()).isEqualTo(41.0);
            assertThat(geo.longitude()).isEqualTo(29.0);
        }

        @Test
        @DisplayName("sınır değerleri kabul etmeli")
        void acceptsBoundaryValues() {
            assertThatCode(() -> new GeoCoordinates(-90, -180)).doesNotThrowAnyException();
            assertThatCode(() -> new GeoCoordinates(90, 180)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("geçersiz latitude'de exception fırlatmalı")
        void throwsOnInvalidLatitude() {
            assertThatThrownBy(() -> new GeoCoordinates(91.0, 29.0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Latitude");
        }

        @Test
        @DisplayName("geçersiz longitude'de exception fırlatmalı")
        void throwsOnInvalidLongitude() {
            assertThatThrownBy(() -> new GeoCoordinates(41.0, 181.0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Longitude");
        }
    }

    @Nested
    @DisplayName("distanceTo")
    class DistanceTo {

        @Test
        @DisplayName("aynı noktadan sıfır mesafe döndürmeli")
        void returnsZeroForSamePoint() {
            GeoCoordinates point = new GeoCoordinates(41.0, 29.0);
            assertThat(point.distanceTo(point).kilometers()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("iki nokta arasındaki mesafeyi hesaplamalı")
        void calculatesDistanceBetweenTwoPoints() {
            GeoCoordinates istanbul = new GeoCoordinates(41.0082, 28.9784);
            GeoCoordinates ankara = new GeoCoordinates(39.9334, 32.8597);
            Distance distance = istanbul.distanceTo(ankara);
            assertThat(distance.kilometers()).isBetween(340.0, 360.0);
        }
    }
}
