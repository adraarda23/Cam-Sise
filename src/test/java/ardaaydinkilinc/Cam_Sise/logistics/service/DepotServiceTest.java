package ardaaydinkilinc.Cam_Sise.logistics.service;

import ardaaydinkilinc.Cam_Sise.logistics.domain.Depot;
import ardaaydinkilinc.Cam_Sise.logistics.repository.DepotRepository;
import ardaaydinkilinc.Cam_Sise.shared.domain.vo.Address;
import ardaaydinkilinc.Cam_Sise.shared.domain.vo.GeoCoordinates;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DepotService Tests")
class DepotServiceTest {

    @Mock private DepotRepository depotRepository;

    @InjectMocks
    private DepotService service;

    private static final Long POOL_OPERATOR_ID = 1L;
    private static final Long DEPOT_ID = 10L;
    private static final Long VEHICLE_ID = 20L;

    private Depot depot;

    @BeforeEach
    void setUp() {
        depot = Depot.create(POOL_OPERATOR_ID, "Ana Depo",
                new Address("Sanayi Cd. No:1", "İstanbul", "İstanbul", "34000", "TR"),
                new GeoCoordinates(41.0, 29.0));
        depot.clearDomainEvents();
    }

    @Nested
    @DisplayName("createDepot")
    class CreateDepot {

        @Test
        @DisplayName("saves and returns depot with active=true")
        void createsDepot() {
            when(depotRepository.save(any(Depot.class))).thenAnswer(inv -> inv.getArgument(0));

            Depot result = service.createDepot(
                    POOL_OPERATOR_ID, "Yeni Depo",
                    "Atatürk Blv.", "Ankara", "Ankara", "06000", "TR",
                    39.9, 32.8);

            assertThat(result.getName()).isEqualTo("Yeni Depo");
            assertThat(result.getActive()).isTrue();
            verify(depotRepository).save(any(Depot.class));
        }
    }

    @Nested
    @DisplayName("addVehicle")
    class AddVehicle {

        @Test
        @DisplayName("adds vehicle id to depot vehicle list")
        void addsVehicle() {
            when(depotRepository.findById(DEPOT_ID)).thenReturn(Optional.of(depot));
            when(depotRepository.save(depot)).thenReturn(depot);

            Depot result = service.addVehicle(DEPOT_ID, VEHICLE_ID);

            assertThat(result.getVehicleIds()).contains(VEHICLE_ID);
            verify(depotRepository).save(depot);
        }

        @Test
        @DisplayName("throws IllegalArgumentException when vehicle already assigned")
        void throwsOnDuplicateVehicle() {
            depot.addVehicle(VEHICLE_ID);
            when(depotRepository.findById(DEPOT_ID)).thenReturn(Optional.of(depot));

            assertThatThrownBy(() -> service.addVehicle(DEPOT_ID, VEHICLE_ID))
                    .isInstanceOf(IllegalArgumentException.class);

            verify(depotRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws when depot not found")
        void throwsWhenDepotNotFound() {
            when(depotRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.addVehicle(999L, VEHICLE_ID))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("removeVehicle")
    class RemoveVehicle {

        @Test
        @DisplayName("removes vehicle id from depot vehicle list")
        void removesVehicle() {
            depot.addVehicle(VEHICLE_ID);
            when(depotRepository.findById(DEPOT_ID)).thenReturn(Optional.of(depot));
            when(depotRepository.save(depot)).thenReturn(depot);

            Depot result = service.removeVehicle(DEPOT_ID, VEHICLE_ID);

            assertThat(result.getVehicleIds()).doesNotContain(VEHICLE_ID);
        }

        @Test
        @DisplayName("throws when depot not found")
        void throwsWhenDepotNotFound() {
            when(depotRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.removeVehicle(999L, VEHICLE_ID))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("returns depot when found")
        void returnsDepot() {
            when(depotRepository.findById(DEPOT_ID)).thenReturn(Optional.of(depot));

            Depot result = service.findById(DEPOT_ID);

            assertThat(result).isEqualTo(depot);
        }

        @Test
        @DisplayName("throws when depot not found")
        void throwsWhenNotFound() {
            when(depotRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.findById(999L))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("findByPoolOperator")
    class FindByPoolOperator {

        @Test
        @DisplayName("returns all depots when active filter is null")
        void returnsAllWhenNoFilter() {
            when(depotRepository.findByPoolOperatorId(POOL_OPERATOR_ID)).thenReturn(List.of(depot));

            List<Depot> result = service.findByPoolOperator(POOL_OPERATOR_ID, null);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("filters by active=true")
        void filtersActive() {
            when(depotRepository.findByPoolOperatorIdAndActive(POOL_OPERATOR_ID, true))
                    .thenReturn(List.of(depot));

            List<Depot> result = service.findByPoolOperator(POOL_OPERATOR_ID, true);

            assertThat(result).hasSize(1);
        }
    }
}
