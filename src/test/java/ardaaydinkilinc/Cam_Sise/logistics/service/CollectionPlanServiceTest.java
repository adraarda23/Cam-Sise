package ardaaydinkilinc.Cam_Sise.logistics.service;

import ardaaydinkilinc.Cam_Sise.inventory.domain.vo.AssetType;
import ardaaydinkilinc.Cam_Sise.logistics.domain.CollectionPlan;
import ardaaydinkilinc.Cam_Sise.logistics.domain.CollectionRequest;
import ardaaydinkilinc.Cam_Sise.logistics.domain.Vehicle;
import ardaaydinkilinc.Cam_Sise.logistics.domain.vo.PlanStatus;
import ardaaydinkilinc.Cam_Sise.logistics.domain.vo.RequestStatus;
import ardaaydinkilinc.Cam_Sise.logistics.domain.vo.VehicleStatus;
import ardaaydinkilinc.Cam_Sise.logistics.repository.CollectionPlanRepository;
import ardaaydinkilinc.Cam_Sise.logistics.repository.CollectionRequestRepository;
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
@DisplayName("CollectionPlanService Tests")
class CollectionPlanServiceTest {

    @Mock private CollectionPlanRepository collectionPlanRepository;
    @Mock private VehicleService vehicleService;
    @Mock private CollectionRequestService collectionRequestService;
    @Mock private CollectionRequestRepository collectionRequestRepository;

    @InjectMocks
    private CollectionPlanService service;

    private static final Long DEPOT_ID = 1L;
    private static final Long VEHICLE_ID = 5L;
    private static final Long PLAN_ID = 10L;

    private CollectionPlan generatedPlan;

    @BeforeEach
    void setUp() {
        generatedPlan = CollectionPlan.generate(
                DEPOT_ID, new Distance(150), new Duration(90), 300, 200,
                LocalDate.now().plusDays(1), "[]");
        generatedPlan.clearDomainEvents();
    }

    @Nested
    @DisplayName("generatePlan")
    class GeneratePlan {

        @Test
        @DisplayName("saves plan with GENERATED status")
        void savesWithGeneratedStatus() {
            when(collectionPlanRepository.save(any(CollectionPlan.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            CollectionPlan result = service.generatePlan(
                    DEPOT_ID, 150.0, 90, 300, 200, LocalDate.now().plusDays(1), "[]");

            assertThat(result.getStatus()).isEqualTo(PlanStatus.GENERATED);
            assertThat(result.getTotalCapacityPallets()).isEqualTo(300);
            verify(collectionPlanRepository).save(any(CollectionPlan.class));
        }
    }

    @Nested
    @DisplayName("assignVehicle")
    class AssignVehicle {

        @Test
        @DisplayName("transitions plan to ASSIGNED and calls vehicleService.assignToPlan")
        void assignsVehicle() {
            when(collectionPlanRepository.findById(PLAN_ID)).thenReturn(Optional.of(generatedPlan));
            when(collectionPlanRepository.save(generatedPlan)).thenReturn(generatedPlan);

            Vehicle vehicle = Vehicle.register(DEPOT_ID, 1L, "34TST001");
            when(vehicleService.assignToPlan(VEHICLE_ID, PLAN_ID)).thenReturn(vehicle);

            CollectionPlan result = service.assignVehicle(PLAN_ID, VEHICLE_ID);

            assertThat(result.getStatus()).isEqualTo(PlanStatus.ASSIGNED);
            assertThat(result.getAssignedVehicleId()).isEqualTo(VEHICLE_ID);
            verify(vehicleService).assignToPlan(VEHICLE_ID, PLAN_ID);
        }

        @Test
        @DisplayName("throws when plan not found")
        void throwsWhenNotFound() {
            when(collectionPlanRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.assignVehicle(999L, VEHICLE_ID))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("startCollection")
    class StartCollection {

        @Test
        @DisplayName("transitions ASSIGNED plan to IN_PROGRESS")
        void startsCollection() {
            generatedPlan.assignVehicle(VEHICLE_ID);
            generatedPlan.clearDomainEvents();

            when(collectionPlanRepository.findById(PLAN_ID)).thenReturn(Optional.of(generatedPlan));
            when(collectionPlanRepository.save(generatedPlan)).thenReturn(generatedPlan);

            CollectionPlan result = service.startCollection(PLAN_ID);

            assertThat(result.getStatus()).isEqualTo(PlanStatus.IN_PROGRESS);
        }

        @Test
        @DisplayName("throws when plan not found")
        void throwsWhenNotFound() {
            when(collectionPlanRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.startCollection(999L))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("completeCollection")
    class CompleteCollection {

        @Test
        @DisplayName("transitions IN_PROGRESS → COMPLETED, completes requests, returns vehicle")
        void completesAndCascades() {
            generatedPlan.assignVehicle(VEHICLE_ID);
            generatedPlan.start();
            generatedPlan.clearDomainEvents();

            CollectionRequest req = CollectionRequest.createManual(1L, AssetType.PALLET, 100, 1L);
            req.approve(1L);
            req.schedule(PLAN_ID);

            when(collectionPlanRepository.findById(PLAN_ID)).thenReturn(Optional.of(generatedPlan));
            when(collectionPlanRepository.save(generatedPlan)).thenReturn(generatedPlan);
            when(collectionRequestRepository.findByCollectionPlanId(PLAN_ID))
                    .thenReturn(List.of(req));
            when(collectionRequestService.complete(req.getId())).thenReturn(req);

            Vehicle vehicle = Vehicle.register(DEPOT_ID, 1L, "34TST001");
            when(vehicleService.returnToDepot(VEHICLE_ID)).thenReturn(vehicle);

            CollectionPlan result = service.completeCollection(PLAN_ID, 100, 50);

            assertThat(result.getStatus()).isEqualTo(PlanStatus.COMPLETED);
            verify(collectionRequestService).complete(req.getId());
            verify(vehicleService).returnToDepot(VEHICLE_ID);
        }
    }

    @Nested
    @DisplayName("cancelPlan")
    class CancelPlan {

        @Test
        @DisplayName("transitions GENERATED → CANCELLED and cancels associated requests")
        void cancelsAndCascades() {
            CollectionRequest req = CollectionRequest.createManual(1L, AssetType.PALLET, 100, 1L);
            req.approve(1L);
            req.schedule(PLAN_ID);

            when(collectionPlanRepository.findById(PLAN_ID)).thenReturn(Optional.of(generatedPlan));
            when(collectionPlanRepository.save(generatedPlan)).thenReturn(generatedPlan);
            when(collectionRequestRepository.findByCollectionPlanId(PLAN_ID))
                    .thenReturn(List.of(req));
            when(collectionRequestService.cancel(req.getId())).thenReturn(req);

            CollectionPlan result = service.cancelPlan(PLAN_ID);

            assertThat(result.getStatus()).isEqualTo(PlanStatus.CANCELLED);
            verify(collectionRequestService).cancel(req.getId());
        }

        @Test
        @DisplayName("cancels ASSIGNED plan and returns vehicle to depot")
        void cancelsAssignedPlanAndReturnsVehicle() {
            generatedPlan.assignVehicle(VEHICLE_ID);
            generatedPlan.clearDomainEvents();

            when(collectionPlanRepository.findById(PLAN_ID)).thenReturn(Optional.of(generatedPlan));
            when(collectionPlanRepository.save(generatedPlan)).thenReturn(generatedPlan);
            when(collectionRequestRepository.findByCollectionPlanId(PLAN_ID)).thenReturn(List.of());

            Vehicle vehicle = Vehicle.register(DEPOT_ID, 1L, "34TST001");
            when(vehicleService.returnToDepot(VEHICLE_ID)).thenReturn(vehicle);

            CollectionPlan result = service.cancelPlan(PLAN_ID);

            assertThat(result.getStatus()).isEqualTo(PlanStatus.CANCELLED);
            verify(vehicleService).returnToDepot(VEHICLE_ID);
        }

        @Test
        @DisplayName("throws when plan not found")
        void throwsWhenNotFound() {
            when(collectionPlanRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.cancelPlan(999L))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
