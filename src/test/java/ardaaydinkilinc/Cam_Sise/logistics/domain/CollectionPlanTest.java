package ardaaydinkilinc.Cam_Sise.logistics.domain;

import ardaaydinkilinc.Cam_Sise.logistics.domain.event.CollectionPlanGenerated;
import ardaaydinkilinc.Cam_Sise.logistics.domain.event.CollectionStarted;
import ardaaydinkilinc.Cam_Sise.logistics.domain.event.RouteAssignedToVehicle;
import ardaaydinkilinc.Cam_Sise.logistics.domain.vo.PlanStatus;
import ardaaydinkilinc.Cam_Sise.shared.domain.vo.Distance;
import ardaaydinkilinc.Cam_Sise.shared.domain.vo.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CollectionPlan Domain Tests")
class CollectionPlanTest {

    @Nested
    @DisplayName("generate")
    class Generate {

        @Test
        @DisplayName("creates plan with GENERATED status and correct fields")
        void createsWithCorrectDefaults() {
            CollectionPlan plan = plan();

            assertThat(plan.getStatus()).isEqualTo(PlanStatus.GENERATED);
            assertThat(plan.getDepotId()).isEqualTo(1L);
            assertThat(plan.getTotalDistance().kilometers()).isEqualTo(120.5);
            assertThat(plan.getEstimatedDuration().minutes()).isEqualTo(180);
            assertThat(plan.getTotalCapacityPallets()).isEqualTo(300);
            assertThat(plan.getTotalCapacitySeparators()).isEqualTo(200);
            assertThat(plan.getAssignedVehicleId()).isNull();
        }

        @Test
        @DisplayName("publishes CollectionPlanGenerated event")
        void publishesEvent() {
            CollectionPlan plan = plan();

            assertThat(plan.getDomainEvents()).hasSize(1);
            assertThat(plan.getDomainEvents().get(0)).isInstanceOf(CollectionPlanGenerated.class);
        }
    }

    @Nested
    @DisplayName("assignVehicle")
    class AssignVehicle {

        @Test
        @DisplayName("transitions GENERATED → ASSIGNED and sets vehicleId")
        void assignsFromGenerated() {
            CollectionPlan plan = plan();
            plan.clearDomainEvents();

            plan.assignVehicle(5L);

            assertThat(plan.getStatus()).isEqualTo(PlanStatus.ASSIGNED);
            assertThat(plan.getAssignedVehicleId()).isEqualTo(5L);
        }

        @Test
        @DisplayName("publishes RouteAssignedToVehicle event")
        void publishesEvent() {
            CollectionPlan plan = plan();
            plan.clearDomainEvents();
            plan.assignVehicle(5L);

            assertThat(plan.getDomainEvents()).hasSize(1);
            assertThat(plan.getDomainEvents().get(0)).isInstanceOf(RouteAssignedToVehicle.class);
        }

        @Test
        @DisplayName("throws when plan is already ASSIGNED")
        void throwsOnAlreadyAssigned() {
            CollectionPlan plan = assigned();
            assertThatThrownBy(() -> plan.assignVehicle(9L)).isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("throws when plan is COMPLETED")
        void throwsOnCompleted() {
            assertThatThrownBy(() -> completed().assignVehicle(1L)).isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("start")
    class Start {

        @Test
        @DisplayName("transitions ASSIGNED → IN_PROGRESS")
        void startsFromAssigned() {
            CollectionPlan plan = assigned();
            plan.start();
            assertThat(plan.getStatus()).isEqualTo(PlanStatus.IN_PROGRESS);
        }

        @Test
        @DisplayName("publishes CollectionStarted event")
        void publishesEvent() {
            CollectionPlan plan = assigned();
            plan.clearDomainEvents();
            plan.start();

            assertThat(plan.getDomainEvents()).hasSize(1);
            assertThat(plan.getDomainEvents().get(0)).isInstanceOf(CollectionStarted.class);
        }

        @Test
        @DisplayName("throws when plan is GENERATED (not yet assigned)")
        void throwsOnGenerated() {
            assertThatThrownBy(() -> plan().start()).isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("throws when plan is COMPLETED")
        void throwsOnCompleted() {
            assertThatThrownBy(() -> completed().start()).isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("complete")
    class Complete {

        @Test
        @DisplayName("transitions IN_PROGRESS → COMPLETED")
        void completesFromInProgress() {
            CollectionPlan plan = inProgress();
            plan.complete(150, 100);
            assertThat(plan.getStatus()).isEqualTo(PlanStatus.COMPLETED);
        }

        @Test
        @DisplayName("throws when plan is GENERATED")
        void throwsOnGenerated() {
            assertThatThrownBy(() -> plan().complete(0, 0)).isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("throws when plan is ASSIGNED")
        void throwsOnAssigned() {
            assertThatThrownBy(() -> assigned().complete(0, 0)).isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("cancel")
    class Cancel {

        @Test
        @DisplayName("transitions GENERATED → CANCELLED")
        void cancelsFromGenerated() {
            CollectionPlan plan = plan();
            plan.cancel();
            assertThat(plan.getStatus()).isEqualTo(PlanStatus.CANCELLED);
        }

        @Test
        @DisplayName("transitions ASSIGNED → CANCELLED")
        void cancelsFromAssigned() {
            CollectionPlan plan = assigned();
            plan.cancel();
            assertThat(plan.getStatus()).isEqualTo(PlanStatus.CANCELLED);
        }

        @Test
        @DisplayName("throws when plan is COMPLETED")
        void throwsOnCompleted() {
            assertThatThrownBy(() -> completed().cancel()).isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("throws when plan is already CANCELLED")
        void throwsOnAlreadyCancelled() {
            CollectionPlan p = plan();
            p.cancel();
            assertThatThrownBy(() -> p.cancel()).isInstanceOf(IllegalStateException.class);
        }
    }

    private CollectionPlan plan() {
        return CollectionPlan.generate(
                1L,
                new Distance(120.5),
                new Duration(180),
                300, 200,
                LocalDate.now().plusDays(1),
                "[]"
        );
    }

    private CollectionPlan assigned() {
        CollectionPlan p = plan();
        p.assignVehicle(5L);
        return p;
    }

    private CollectionPlan inProgress() {
        CollectionPlan p = assigned();
        p.start();
        return p;
    }

    private CollectionPlan completed() {
        CollectionPlan p = inProgress();
        p.complete(150, 100);
        return p;
    }
}
