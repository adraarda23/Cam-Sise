package ardaaydinkilinc.Cam_Sise.logistics.service;

import ardaaydinkilinc.Cam_Sise.inventory.domain.vo.AssetType;
import ardaaydinkilinc.Cam_Sise.logistics.domain.CollectionPlan;
import ardaaydinkilinc.Cam_Sise.logistics.domain.CollectionRequest;
import ardaaydinkilinc.Cam_Sise.logistics.domain.Vehicle;
import ardaaydinkilinc.Cam_Sise.logistics.domain.vo.PlanStatus;
import ardaaydinkilinc.Cam_Sise.logistics.domain.vo.VehicleStatus;
import ardaaydinkilinc.Cam_Sise.logistics.repository.CollectionPlanRepository;
import ardaaydinkilinc.Cam_Sise.logistics.repository.CollectionRequestRepository;
import ardaaydinkilinc.Cam_Sise.logistics.repository.VehicleRepository;
import ardaaydinkilinc.Cam_Sise.shared.domain.vo.Distance;
import ardaaydinkilinc.Cam_Sise.shared.domain.vo.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("VehicleService Tests")
class VehicleServiceTest {

    @Mock private VehicleRepository vehicleRepository;
    @Mock private CollectionPlanRepository collectionPlanRepository;
    @Mock private CollectionRequestRepository collectionRequestRepository;

    @InjectMocks
    private VehicleService service;

    private static final Long VEHICLE_ID = 1L;
    private static final Long DEPOT_ID = 10L;
    private static final Long PLAN_ID = 20L;

    private Vehicle availableVehicle;

    @BeforeEach
    void setUp() {
        availableVehicle = Vehicle.register(DEPOT_ID, 1L, "34ABC001");
        availableVehicle.clearDomainEvents();
    }

    @Nested
    @DisplayName("registerVehicle")
    class RegisterVehicle {

        @Test
        @DisplayName("saves and returns new vehicle when plate is unique")
        void registersNewVehicle() {
            when(vehicleRepository.existsByPlateNumber("34NEW001")).thenReturn(false);
            when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(inv -> inv.getArgument(0));

            Vehicle result = service.registerVehicle(DEPOT_ID, 1L, "34NEW001");

            assertThat(result.getStatus()).isEqualTo(VehicleStatus.AVAILABLE);
            assertThat(result.getPlateNumber()).isEqualTo("34NEW001");
            verify(vehicleRepository).save(any(Vehicle.class));
        }

        @Test
        @DisplayName("throws IllegalArgumentException when plate number already exists")
        void throwsOnDuplicatePlate() {
            when(vehicleRepository.existsByPlateNumber("34DUP001")).thenReturn(true);

            assertThatThrownBy(() -> service.registerVehicle(DEPOT_ID, 1L, "34DUP001"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("already exists");

            verify(vehicleRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("assignToPlan")
    class AssignToPlan {

        @Test
        @DisplayName("transitions vehicle to ON_ROUTE and saves")
        void assignsVehicle() {
            when(vehicleRepository.findById(VEHICLE_ID)).thenReturn(Optional.of(availableVehicle));
            when(vehicleRepository.save(availableVehicle)).thenReturn(availableVehicle);

            Vehicle result = service.assignToPlan(VEHICLE_ID, PLAN_ID);

            assertThat(result.getStatus()).isEqualTo(VehicleStatus.ON_ROUTE);
            assertThat(result.getCurrentCollectionPlanId()).isEqualTo(PLAN_ID);
            verify(vehicleRepository).save(availableVehicle);
        }

        @Test
        @DisplayName("throws when vehicle not found")
        void throwsWhenNotFound() {
            when(vehicleRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.assignToPlan(999L, PLAN_ID))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("assignToRoute")
    class AssignToRoute {

        @Test
        @DisplayName("transitions vehicle to ON_ROUTE with driver info")
        void assignsWithDriver() {
            when(vehicleRepository.findById(VEHICLE_ID)).thenReturn(Optional.of(availableVehicle));
            when(vehicleRepository.save(availableVehicle)).thenReturn(availableVehicle);

            Vehicle result = service.assignToRoute(VEHICLE_ID, PLAN_ID, "Ali Demir", "B99990", "05550001111");

            assertThat(result.getStatus()).isEqualTo(VehicleStatus.ON_ROUTE);
            assertThat(result.getCurrentDriver()).isNotNull();
        }
    }

    @Nested
    @DisplayName("departFromDepot")
    class DepartFromDepot {

        @Test
        @DisplayName("saves vehicle and publishes event")
        void departsSuccessfully() {
            availableVehicle.assignToPlan(PLAN_ID);
            availableVehicle.clearDomainEvents();

            when(vehicleRepository.findById(VEHICLE_ID)).thenReturn(Optional.of(availableVehicle));
            when(vehicleRepository.save(availableVehicle)).thenReturn(availableVehicle);

            service.departFromDepot(VEHICLE_ID);

            verify(vehicleRepository).save(availableVehicle);
        }
    }

    @Nested
    @DisplayName("returnToDepot")
    class ReturnToDepot {

        @Test
        @DisplayName("transitions ON_ROUTE vehicle to AVAILABLE")
        void returnsToDepot() {
            availableVehicle.assignToPlan(PLAN_ID);
            availableVehicle.clearDomainEvents();

            when(vehicleRepository.findById(VEHICLE_ID)).thenReturn(Optional.of(availableVehicle));
            when(vehicleRepository.save(availableVehicle)).thenReturn(availableVehicle);

            Vehicle result = service.returnToDepot(VEHICLE_ID);

            assertThat(result.getStatus()).isEqualTo(VehicleStatus.AVAILABLE);
        }
    }

    @Nested
    @DisplayName("changeStatus")
    class ChangeStatus {

        @Test
        @DisplayName("normal status change from AVAILABLE to MAINTENANCE")
        void normalStatusChange() {
            when(vehicleRepository.findById(VEHICLE_ID)).thenReturn(Optional.of(availableVehicle));
            when(vehicleRepository.save(availableVehicle)).thenReturn(availableVehicle);

            Vehicle result = service.changeStatus(VEHICLE_ID, VehicleStatus.MAINTENANCE);

            assertThat(result.getStatus()).isEqualTo(VehicleStatus.MAINTENANCE);
            verifyNoInteractions(collectionPlanRepository, collectionRequestRepository);
        }

        @Test
        @DisplayName("ON_ROUTE → MAINTENANCE cancels active plans and their requests")
        void cascadesCancelOnPlanWhenLeavingOnRoute() {
            availableVehicle.assignToPlan(PLAN_ID);
            availableVehicle.clearDomainEvents();

            CollectionPlan inProgressPlan = CollectionPlan.generate(
                    DEPOT_ID, new Distance(100), new Duration(60), 200, 100, LocalDate.now(), "[]");
            inProgressPlan.assignVehicle(VEHICLE_ID);
            inProgressPlan.start();
            inProgressPlan.clearDomainEvents();

            CollectionRequest req = CollectionRequest.createManual(5L, AssetType.PALLET, 100, 1L);
            req.approve(1L);
            req.schedule(PLAN_ID);

            when(vehicleRepository.findById(VEHICLE_ID)).thenReturn(Optional.of(availableVehicle));
            when(vehicleRepository.save(availableVehicle)).thenReturn(availableVehicle);
            when(collectionPlanRepository.findByAssignedVehicleId(VEHICLE_ID))
                    .thenReturn(List.of(inProgressPlan));
            when(collectionRequestRepository.findByCollectionPlanId(any()))
                    .thenReturn(List.of(req));

            service.changeStatus(VEHICLE_ID, VehicleStatus.MAINTENANCE);

            assertThat(inProgressPlan.getStatus()).isEqualTo(PlanStatus.CANCELLED);
            verify(collectionPlanRepository).save(inProgressPlan);
            verify(collectionRequestRepository).save(req);
        }

        @Test
        @DisplayName("AVAILABLE → MAINTENANCE does not touch plans")
        void noPlansQueriedWhenNotOnRoute() {
            when(vehicleRepository.findById(VEHICLE_ID)).thenReturn(Optional.of(availableVehicle));
            when(vehicleRepository.save(availableVehicle)).thenReturn(availableVehicle);

            service.changeStatus(VEHICLE_ID, VehicleStatus.MAINTENANCE);

            verifyNoInteractions(collectionPlanRepository);
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("Araç bulunduğunda döndürmeli")
        void returnsVehicleWhenFound() {
            when(vehicleRepository.findById(VEHICLE_ID)).thenReturn(Optional.of(availableVehicle));

            Vehicle result = service.findById(VEHICLE_ID);

            assertThat(result).isEqualTo(availableVehicle);
        }

        @Test
        @DisplayName("Araç bulunamazsa exception fırlatmalı")
        void throwsWhenNotFound() {
            when(vehicleRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.findById(999L))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("findByPlateNumber")
    class FindByPlateNumber {

        @Test
        @DisplayName("Plaka ile araç bulunduğunda döndürmeli")
        void returnsVehicleWhenFound() {
            when(vehicleRepository.findByPlateNumber("34ABC001")).thenReturn(Optional.of(availableVehicle));

            Vehicle result = service.findByPlateNumber("34ABC001");

            assertThat(result).isEqualTo(availableVehicle);
        }

        @Test
        @DisplayName("Plaka bulunamazsa exception fırlatmalı")
        void throwsWhenNotFound() {
            when(vehicleRepository.findByPlateNumber("00YOK001")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.findByPlateNumber("00YOK001"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("findByDepot")
    class FindByDepot {

        @Test
        @DisplayName("Status verilince filtrelenmiş araçları döndürmeli")
        void returnsFilteredVehiclesWhenStatusProvided() {
            when(vehicleRepository.findByDepotIdAndStatus(DEPOT_ID, VehicleStatus.AVAILABLE))
                    .thenReturn(List.of(availableVehicle));

            List<Vehicle> result = service.findByDepot(DEPOT_ID, VehicleStatus.AVAILABLE);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Status null ise tüm araçları döndürmeli")
        void returnsAllVehiclesWhenStatusIsNull() {
            when(vehicleRepository.findByDepotId(DEPOT_ID)).thenReturn(List.of(availableVehicle));

            List<Vehicle> result = service.findByDepot(DEPOT_ID, null);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Depoda araç yoksa boş liste döndürmeli")
        void returnsEmptyListWhenNoVehicles() {
            when(vehicleRepository.findByDepotId(DEPOT_ID)).thenReturn(List.of());

            List<Vehicle> result = service.findByDepot(DEPOT_ID, null);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByStatus")
    class FindByStatus {

        @Test
        @DisplayName("Status'a göre araçları döndürmeli")
        void returnsVehiclesByStatus() {
            when(vehicleRepository.findByStatus(VehicleStatus.AVAILABLE))
                    .thenReturn(List.of(availableVehicle));

            List<Vehicle> result = service.findByStatus(VehicleStatus.AVAILABLE);

            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("Tüm araçları döndürmeli")
        void returnsAllVehicles() {
            when(vehicleRepository.findAll()).thenReturn(List.of(availableVehicle));

            List<Vehicle> result = service.findAll();

            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("findByPoolOperatorId")
    class FindByPoolOperatorId {

        @Test
        @DisplayName("PoolOperatorId'ye göre araçları döndürmeli")
        void returnsVehiclesForPoolOperator() {
            when(vehicleRepository.findByPoolOperatorId(1L)).thenReturn(List.of(availableVehicle));

            List<Vehicle> result = service.findByPoolOperatorId(1L);

            assertThat(result).hasSize(1);
            verify(vehicleRepository).findByPoolOperatorId(1L);
        }
    }

    @Nested
    @DisplayName("not-found dalları (eksik branch'ler)")
    class NotFoundBranches {

        @Test
        @DisplayName("assignToRoute: araç bulunamazsa exception fırlatmalı")
        void assignToRouteThrowsWhenNotFound() {
            when(vehicleRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.assignToRoute(999L, PLAN_ID, "Ali", "B99990", "05550001111"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("departFromDepot: araç bulunamazsa exception fırlatmalı")
        void departFromDepotThrowsWhenNotFound() {
            when(vehicleRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.departFromDepot(999L))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("returnToDepot: araç bulunamazsa exception fırlatmalı")
        void returnToDepotThrowsWhenNotFound() {
            when(vehicleRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.returnToDepot(999L))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("changeStatus: araç bulunamazsa exception fırlatmalı")
        void changeStatusThrowsWhenNotFound() {
            when(vehicleRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.changeStatus(999L, VehicleStatus.MAINTENANCE))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("changeStatus — ON_ROUTE boş plan listesi")
    class ChangeStatusNoActivePlans {

        @Test
        @DisplayName("ON_ROUTE → MAINTENANCE, aktif plan yok → cancelPlan çağrılmamalı")
        void onRouteToMaintenanceWithNoActivePlans() {
            availableVehicle.assignToPlan(PLAN_ID);
            availableVehicle.clearDomainEvents();

            when(vehicleRepository.findById(VEHICLE_ID)).thenReturn(Optional.of(availableVehicle));
            when(vehicleRepository.save(availableVehicle)).thenReturn(availableVehicle);
            when(collectionPlanRepository.findByAssignedVehicleId(VEHICLE_ID)).thenReturn(List.of());

            service.changeStatus(VEHICLE_ID, VehicleStatus.MAINTENANCE);

            verify(collectionPlanRepository).findByAssignedVehicleId(VEHICLE_ID);
            verify(collectionPlanRepository, never()).save(any());
        }
    }
}
