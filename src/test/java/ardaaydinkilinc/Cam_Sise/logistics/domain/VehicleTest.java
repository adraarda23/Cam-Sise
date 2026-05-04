package ardaaydinkilinc.Cam_Sise.logistics.domain;

import ardaaydinkilinc.Cam_Sise.logistics.domain.event.VehicleRegistered;
import ardaaydinkilinc.Cam_Sise.logistics.domain.event.VehicleReturnedToDepot;
import ardaaydinkilinc.Cam_Sise.logistics.domain.vo.DriverInfo;
import ardaaydinkilinc.Cam_Sise.logistics.domain.vo.VehicleStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Vehicle Domain Tests")
class VehicleTest {

    @Nested
    @DisplayName("register")
    class Register {

        @Test
        @DisplayName("creates vehicle with AVAILABLE status and correct fields")
        void createsWithDefaults() {
            Vehicle v = vehicle();

            assertThat(v.getStatus()).isEqualTo(VehicleStatus.AVAILABLE);
            assertThat(v.getPlateNumber()).isEqualTo("34ABC123");
            assertThat(v.getDepotId()).isEqualTo(1L);
            assertThat(v.getCurrentCollectionPlanId()).isNull();
        }

        @Test
        @DisplayName("publishes VehicleRegistered event")
        void publishesEvent() {
            Vehicle v = vehicle();

            assertThat(v.getDomainEvents()).hasSize(1);
            assertThat(v.getDomainEvents().get(0)).isInstanceOf(VehicleRegistered.class);
        }
    }

    @Nested
    @DisplayName("assignToPlan")
    class AssignToPlan {

        @Test
        @DisplayName("AVAILABLE → ON_ROUTE and sets planId")
        void assignsToRoute() {
            Vehicle v = vehicle();
            v.clearDomainEvents();

            v.assignToPlan(42L);

            assertThat(v.getStatus()).isEqualTo(VehicleStatus.ON_ROUTE);
            assertThat(v.getCurrentCollectionPlanId()).isEqualTo(42L);
        }

        @Test
        @DisplayName("throws when vehicle is not AVAILABLE")
        void throwsWhenNotAvailable() {
            Vehicle v = onRoute();

            assertThatThrownBy(() -> v.assignToPlan(99L))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("assignToRoute")
    class AssignToRoute {

        @Test
        @DisplayName("AVAILABLE → ON_ROUTE and sets driver info")
        void assignsWithDriver() {
            Vehicle v = vehicle();
            DriverInfo driver = new DriverInfo("Ali Demir", "B12345", "05551234567");

            v.assignToRoute(10L, driver);

            assertThat(v.getStatus()).isEqualTo(VehicleStatus.ON_ROUTE);
            assertThat(v.getCurrentDriver()).isEqualTo(driver);
        }

        @Test
        @DisplayName("throws when vehicle is not AVAILABLE")
        void throwsWhenNotAvailable() {
            Vehicle v = onRoute();
            DriverInfo driver = new DriverInfo("X", "ABCDEF", "05550000000");

            assertThatThrownBy(() -> v.assignToRoute(20L, driver))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("departFromDepot")
    class DepartFromDepot {

        @Test
        @DisplayName("publishes departure event when ON_ROUTE")
        void publishesEvent() {
            Vehicle v = onRoute();
            v.clearDomainEvents();

            v.departFromDepot();

            assertThat(v.getDomainEvents()).hasSize(1);
        }

        @Test
        @DisplayName("throws when vehicle is not ON_ROUTE")
        void throwsWhenNotOnRoute() {
            Vehicle v = vehicle();

            assertThatThrownBy(() -> v.departFromDepot())
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("returnToDepot")
    class ReturnToDepot {

        @Test
        @DisplayName("ON_ROUTE → AVAILABLE, clears driver and plan")
        void returnsToDepot() {
            Vehicle v = onRoute();
            v.clearDomainEvents();

            v.returnToDepot();

            assertThat(v.getStatus()).isEqualTo(VehicleStatus.AVAILABLE);
            assertThat(v.getCurrentCollectionPlanId()).isNull();
            assertThat(v.getCurrentDriver()).isNull();
        }

        @Test
        @DisplayName("publishes VehicleReturnedToDepot event")
        void publishesEvent() {
            Vehicle v = onRoute();
            v.clearDomainEvents();
            v.returnToDepot();

            assertThat(v.getDomainEvents()).hasSize(1);
            assertThat(v.getDomainEvents().get(0)).isInstanceOf(VehicleReturnedToDepot.class);
        }

        @Test
        @DisplayName("throws when vehicle is not ON_ROUTE")
        void throwsWhenNotOnRoute() {
            Vehicle v = vehicle();

            assertThatThrownBy(() -> v.returnToDepot())
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("changeStatus")
    class ChangeStatus {

        @Test
        @DisplayName("AVAILABLE → MAINTENANCE succeeds")
        void availableToMaintenance() {
            Vehicle v = vehicle();
            v.changeStatus(VehicleStatus.MAINTENANCE);
            assertThat(v.getStatus()).isEqualTo(VehicleStatus.MAINTENANCE);
        }

        @Test
        @DisplayName("AVAILABLE → INACTIVE succeeds")
        void availableToInactive() {
            Vehicle v = vehicle();
            v.changeStatus(VehicleStatus.INACTIVE);
            assertThat(v.getStatus()).isEqualTo(VehicleStatus.INACTIVE);
        }

        @Test
        @DisplayName("ON_ROUTE → MAINTENANCE succeeds and clears plan reference")
        void onRouteToMaintenanceClearsPlan() {
            Vehicle v = onRoute();
            v.changeStatus(VehicleStatus.MAINTENANCE);

            assertThat(v.getStatus()).isEqualTo(VehicleStatus.MAINTENANCE);
            assertThat(v.getCurrentCollectionPlanId()).isNull();
        }

        @Test
        @DisplayName("throws when manually changing to ON_ROUTE")
        void throwsChangingToOnRoute() {
            Vehicle v = vehicle();
            assertThatThrownBy(() -> v.changeStatus(VehicleStatus.ON_ROUTE))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("throws when changing from ON_ROUTE to AVAILABLE directly")
        void throwsOnRouteToAvailable() {
            Vehicle v = onRoute();
            assertThatThrownBy(() -> v.changeStatus(VehicleStatus.AVAILABLE))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("throws when deactivating while ON_ROUTE")
        void throwsDeactivateOnRoute() {
            Vehicle v = onRoute();
            assertThatThrownBy(() -> v.changeStatus(VehicleStatus.INACTIVE))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    private Vehicle vehicle() {
        return Vehicle.register(1L, 1L, "34ABC123");
    }

    private Vehicle onRoute() {
        Vehicle v = vehicle();
        v.assignToPlan(5L);
        return v;
    }
}
