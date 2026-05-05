package ardaaydinkilinc.Cam_Sise.logistics.service.event;

import ardaaydinkilinc.Cam_Sise.logistics.domain.CollectionPlan;
import ardaaydinkilinc.Cam_Sise.logistics.domain.event.*;
import ardaaydinkilinc.Cam_Sise.logistics.domain.vo.PlanStatus;
import ardaaydinkilinc.Cam_Sise.logistics.domain.vo.VehicleStatus;
import ardaaydinkilinc.Cam_Sise.logistics.service.CollectionPlanService;
import ardaaydinkilinc.Cam_Sise.logistics.service.CollectionRequestService;
import ardaaydinkilinc.Cam_Sise.logistics.service.RouteOptimizationService;
import ardaaydinkilinc.Cam_Sise.logistics.service.VehicleService;
import ardaaydinkilinc.Cam_Sise.shared.domain.vo.Distance;
import ardaaydinkilinc.Cam_Sise.shared.domain.vo.Duration;
import ardaaydinkilinc.Cam_Sise.shared.domain.vo.GeoCoordinates;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LogisticsEventHandler Tests")
class LogisticsEventHandlerTest {

    @Mock private CollectionRequestService collectionRequestService;
    @Mock private RouteOptimizationService routeOptimizationService;
    @Mock private CollectionPlanService collectionPlanService;
    @Mock private VehicleService vehicleService;

    @InjectMocks
    private LogisticsEventHandler handler;

    @Test
    @DisplayName("CollectionRequestApproved eventini handle etmeli")
    void handlesCollectionRequestApproved() {
        var event = new CollectionRequestApproved(1L, 2L, 3L, LocalDateTime.now());
        assertThatCode(() -> handler.handleCollectionRequestApproved(event)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("CollectionRequestRejected eventini handle etmeli")
    void handlesCollectionRequestRejected() {
        var event = new CollectionRequestRejected(1L, 2L, "Stok yetersiz", LocalDateTime.now());
        assertThatCode(() -> handler.handleCollectionRequestRejected(event)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("CollectionPlanGenerated eventini handle etmeli")
    void handlesCollectionPlanGenerated() {
        var event = new CollectionPlanGenerated(1L, new Distance(150), 100, 50, LocalDate.now(), LocalDateTime.now());
        assertThatCode(() -> handler.handleCollectionPlanGenerated(event)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("RouteAssignedToVehicle eventini handle etmeli")
    void handlesRouteAssignedToVehicle() {
        var event = new RouteAssignedToVehicle(1L, 2L, LocalDate.now(), LocalDateTime.now());
        assertThatCode(() -> handler.handleRouteAssignedToVehicle(event)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("CollectionStarted eventini handle etmeli")
    void handlesCollectionStarted() {
        var event = new CollectionStarted(1L, 2L, 3L, LocalDateTime.now());
        assertThatCode(() -> handler.handleCollectionStarted(event)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("CollectionCompleted eventini handle etmeli")
    void handlesCollectionCompleted() {
        var event = new CollectionCompleted(1L, 2L, 3L, 100, 50, LocalDateTime.now());
        assertThatCode(() -> handler.handleCollectionCompleted(event)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("VehicleRegistered eventini handle etmeli")
    void handlesVehicleRegistered() {
        var event = new VehicleRegistered(1L, 2L, "34ABC001", LocalDateTime.now());
        assertThatCode(() -> handler.handleVehicleRegistered(event)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("DepotCreated eventini handle etmeli")
    void handlesDepotCreated() {
        var event = new DepotCreated(1L, "Merkez Depo", new GeoCoordinates(41.0, 29.0), LocalDateTime.now());
        assertThatCode(() -> handler.handleDepotCreated(event)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("VehicleStatusChanged: oldStatus ON_ROUTE değilse plan kontrolü yapılmamalı")
    void handlesVehicleStatusChangedWhenNotLeavingOnRoute() {
        var event = new VehicleStatusChanged(1L, VehicleStatus.AVAILABLE, VehicleStatus.MAINTENANCE, LocalDateTime.now());
        handler.handleVehicleStatusChanged(event);
        verifyNoInteractions(collectionPlanService);
    }

    @Test
    @DisplayName("VehicleStatusChanged: ON_ROUTE → MAINTENANCE, aktif plan yok → cancelPlan çağrılmamalı")
    void handlesVehicleStatusChangedOnRouteToMaintenanceNoActivePlans() {
        when(collectionPlanService.findByVehicle(1L)).thenReturn(List.of());

        var event = new VehicleStatusChanged(1L, VehicleStatus.ON_ROUTE, VehicleStatus.MAINTENANCE, LocalDateTime.now());
        handler.handleVehicleStatusChanged(event);

        verify(collectionPlanService).findByVehicle(1L);
        verify(collectionPlanService, never()).cancelPlan(any());
    }

    @Test
    @DisplayName("VehicleStatusChanged: ON_ROUTE → MAINTENANCE, IN_PROGRESS plan var → iptal edilmeli")
    void handlesVehicleStatusChangedOnRouteToMaintenanceWithActivePlan() {
        CollectionPlan plan = CollectionPlan.generate(
                10L, new Distance(100), new Duration(60), 200, 100, LocalDate.now(), "[]");
        plan.assignVehicle(1L);
        plan.start();
        plan.clearDomainEvents();

        when(collectionPlanService.findByVehicle(1L)).thenReturn(List.of(plan));

        var event = new VehicleStatusChanged(1L, VehicleStatus.ON_ROUTE, VehicleStatus.MAINTENANCE, LocalDateTime.now());
        handler.handleVehicleStatusChanged(event);

        verify(collectionPlanService).cancelPlan(plan.getId());
    }

    @Test
    @DisplayName("VehicleStatusChanged: ON_ROUTE → MAINTENANCE, cancelPlan exception atar → exception yutulmalı")
    void handlesVehicleStatusChangedCancelPlanException() {
        CollectionPlan plan = CollectionPlan.generate(
                10L, new Distance(100), new Duration(60), 200, 100, LocalDate.now(), "[]");
        plan.assignVehicle(1L);
        plan.start();
        plan.clearDomainEvents();

        when(collectionPlanService.findByVehicle(1L)).thenReturn(List.of(plan));
        doThrow(new RuntimeException("DB error")).when(collectionPlanService).cancelPlan(any());

        var event = new VehicleStatusChanged(1L, VehicleStatus.ON_ROUTE, VehicleStatus.MAINTENANCE, LocalDateTime.now());
        assertThatCode(() -> handler.handleVehicleStatusChanged(event)).doesNotThrowAnyException();
    }
}
