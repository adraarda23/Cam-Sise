package ardaaydinkilinc.Cam_Sise.logistics.domain.vo;

import ardaaydinkilinc.Cam_Sise.shared.domain.vo.Duration;
import ardaaydinkilinc.Cam_Sise.shared.domain.vo.GeoCoordinates;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("RouteStop Tests")
class RouteStopTest {

    private static final GeoCoordinates LOCATION = new GeoCoordinates(41.0, 29.0);
    private static final Duration SERVICE_TIME = new Duration(30);

    @Nested
    @DisplayName("constructor")
    class Constructor {

        @Test
        @DisplayName("geçerli parametrelerle oluşturulabilmeli")
        void acceptsValidRouteStop() {
            RouteStop stop = new RouteStop(1L, LOCATION, 0, 100, 50, SERVICE_TIME);
            assertThat(stop.fillerId()).isEqualTo(1L);
            assertThat(stop.stopOrder()).isEqualTo(0);
            assertThat(stop.estimatedPallets()).isEqualTo(100);
        }

        @Test
        @DisplayName("fillerId null ise exception fırlatmalı")
        void throwsOnNullFillerId() {
            assertThatThrownBy(() -> new RouteStop(null, LOCATION, 1, 100, 50, SERVICE_TIME))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Filler ID");
        }

        @Test
        @DisplayName("location null ise exception fırlatmalı")
        void throwsOnNullLocation() {
            assertThatThrownBy(() -> new RouteStop(1L, null, 1, 100, 50, SERVICE_TIME))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Location");
        }

        @Test
        @DisplayName("stopOrder negatif ise exception fırlatmalı")
        void throwsOnNegativeStopOrder() {
            assertThatThrownBy(() -> new RouteStop(1L, LOCATION, -1, 100, 50, SERVICE_TIME))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Stop order");
        }

        @Test
        @DisplayName("stopOrder sıfır geçerlidir")
        void acceptsZeroStopOrder() {
            assertThatCode(() -> new RouteStop(1L, LOCATION, 0, 100, 50, SERVICE_TIME))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("estimatedPallets negatif ise exception fırlatmalı")
        void throwsOnNegativeEstimatedPallets() {
            assertThatThrownBy(() -> new RouteStop(1L, LOCATION, 1, -1, 50, SERVICE_TIME))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("quantities");
        }

        @Test
        @DisplayName("estimatedSeparators negatif ise exception fırlatmalı")
        void throwsOnNegativeEstimatedSeparators() {
            assertThatThrownBy(() -> new RouteStop(1L, LOCATION, 1, 100, -1, SERVICE_TIME))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("quantities");
        }

        @Test
        @DisplayName("estimatedServiceTime null ise exception fırlatmalı")
        void throwsOnNullEstimatedServiceTime() {
            assertThatThrownBy(() -> new RouteStop(1L, LOCATION, 1, 100, 50, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Service time");
        }
    }

    @Nested
    @DisplayName("getEstimatedCapacity")
    class GetEstimatedCapacity {

        @Test
        @DisplayName("tahmini kapasiteyi döndürmeli")
        void returnsEstimatedCapacity() {
            RouteStop stop = new RouteStop(1L, LOCATION, 1, 100, 50, SERVICE_TIME);
            Capacity cap = stop.getEstimatedCapacity();
            assertThat(cap.pallets()).isEqualTo(100);
            assertThat(cap.separators()).isEqualTo(50);
        }
    }
}
