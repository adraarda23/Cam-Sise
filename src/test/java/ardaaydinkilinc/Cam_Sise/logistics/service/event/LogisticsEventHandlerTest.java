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
    @DisplayName("CollectionRequestApproved: servisler çağrılmamalı (sadece loglama)")
    void handlesCollectionRequestApproved() {
        var event = new CollectionRequestApproved(1L, 2L, 3L, LocalDateTime.now());
        handler.handleCollectionRequestApproved(event);
        verifyNoInteractions(collectionPlanService, collectionRequestService,
                routeOptimizationService, vehicleService);
    }

    @Test
    @DisplayName("CollectionRequestRejected: servisler çağrılmamalı (sadece loglama)")
    void handlesCollectionRequestRejected() {
        var event = new CollectionRequestRejected(1L, 2L, "Stok yetersiz", LocalDateTime.now());
        handler.handleCollectionRequestRejected(event);
        verifyNoInteractions(collectionPlanService, collectionRequestService,
                routeOptimizationService, vehicleService);
    }

    @Test
    @DisplayName("CollectionPlanGenerated: servisler çağrılmamalı (sadece loglama)")
    void handlesCollectionPlanGenerated() {
        var event = new CollectionPlanGenerated(1L, new Distance(150), 100, 50, LocalDate.now(), LocalDateTime.now());
        handler.handleCollectionPlanGenerated(event);
        verifyNoInteractions(collectionPlanService, collectionRequestService,
                routeOptimizationService, vehicleService);
    }

    @Test
    @DisplayName("RouteAssignedToVehicle: servisler çağrılmamalı (sadece loglama)")
    void handlesRouteAssignedToVehicle() {
        var event = new RouteAssignedToVehicle(1L, 2L, LocalDate.now(), LocalDateTime.now());
        handler.handleRouteAssignedToVehicle(event);
        verifyNoInteractions(collectionPlanService, collectionRequestService,
                routeOptimizationService, vehicleService);
    }

    @Test
    @DisplayName("CollectionStarted: servisler çağrılmamalı (sadece loglama)")
    void handlesCollectionStarted() {
        var event = new CollectionStarted(1L, 2L, 3L, LocalDateTime.now());
        handler.handleCollectionStarted(event);
        verifyNoInteractions(collectionPlanService, collectionRequestService,
                routeOptimizationService, vehicleService);
    }

    @Test
    @DisplayName("CollectionCompleted: servisler çağrılmamalı (sadece loglama)")
    void handlesCollectionCompleted() {
        var event = new CollectionCompleted(1L, 2L, 3L, 100, 50, LocalDateTime.now());
        handler.handleCollectionCompleted(event);
        verifyNoInteractions(collectionPlanService, collectionRequestService,
                routeOptimizationService, vehicleService);
    }

    @Test
    @DisplayName("VehicleRegistered: servisler çağrılmamalı (sadece loglama)")
    void handlesVehicleRegistered() {
        var event = new VehicleRegistered(1L, 2L, "34ABC001", LocalDateTime.now());
        handler.handleVehicleRegistered(event);
        verifyNoInteractions(collectionPlanService, collectionRequestService,
                routeOptimizationService, vehicleService);
    }

    @Test
    @DisplayName("DepotCreated: servisler çağrılmamalı (sadece loglama)")
    void handlesDepotCreated() {
        var event = new DepotCreated(1L, "Merkez Depo", new GeoCoordinates(41.0, 29.0), LocalDateTime.now());
        handler.handleDepotCreated(event);
        verifyNoInteractions(collectionPlanService, collectionRequestService,
                routeOptimizationService, vehicleService);
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
