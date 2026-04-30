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
}
