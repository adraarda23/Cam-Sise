package ardaaydinkilinc.Cam_Sise.logistics.domain;

import ardaaydinkilinc.Cam_Sise.inventory.domain.vo.AssetType;
import ardaaydinkilinc.Cam_Sise.logistics.domain.event.*;
import ardaaydinkilinc.Cam_Sise.logistics.domain.vo.RequestSource;
import ardaaydinkilinc.Cam_Sise.logistics.domain.vo.RequestStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CollectionRequest Domain Tests")
class CollectionRequestTest {

    @Nested
    @DisplayName("createAutomatic")
    class CreateAutomatic {

        @Test
        @DisplayName("creates request with PENDING status and AUTO_THRESHOLD source")
        void createsWithCorrectDefaults() {
            CollectionRequest req = CollectionRequest.createAutomatic(1L, AssetType.PALLET, 50);

            assertThat(req.getStatus()).isEqualTo(RequestStatus.PENDING);
            assertThat(req.getSource()).isEqualTo(RequestSource.AUTO_THRESHOLD);
            assertThat(req.getFillerId()).isEqualTo(1L);
            assertThat(req.getAssetType()).isEqualTo(AssetType.PALLET);
            assertThat(req.getEstimatedQuantity()).isEqualTo(50);
        }

        @Test
        @DisplayName("publishes CollectionRequestCreated domain event")
        void publishesCreatedEvent() {
            CollectionRequest req = CollectionRequest.createAutomatic(1L, AssetType.PALLET, 50);

            assertThat(req.getDomainEvents()).hasSize(1);
            assertThat(req.getDomainEvents().get(0)).isInstanceOf(CollectionRequestCreated.class);
        }
    }

    @Nested
    @DisplayName("createManual")
    class CreateManual {

        @Test
        @DisplayName("creates request with PENDING status and MANUAL_CUSTOMER source")
        void createsWithCorrectDefaults() {
            CollectionRequest req = CollectionRequest.createManual(2L, AssetType.SEPARATOR, 100, 99L);

            assertThat(req.getStatus()).isEqualTo(RequestStatus.PENDING);
            assertThat(req.getSource()).isEqualTo(RequestSource.MANUAL_CUSTOMER);
            assertThat(req.getFillerId()).isEqualTo(2L);
            assertThat(req.getAssetType()).isEqualTo(AssetType.SEPARATOR);
            assertThat(req.getEstimatedQuantity()).isEqualTo(100);
        }

        @Test
        @DisplayName("publishes CollectionRequestCreated domain event")
        void publishesCreatedEvent() {
            CollectionRequest req = CollectionRequest.createManual(1L, AssetType.PALLET, 50, 1L);

            assertThat(req.getDomainEvents()).hasSize(1);
            assertThat(req.getDomainEvents().get(0)).isInstanceOf(CollectionRequestCreated.class);
        }
    }

    @Nested
    @DisplayName("approve")
    class Approve {

        @Test
        @DisplayName("transitions PENDING → APPROVED and sets approvedByUserId")
        void approvesFromPending() {
            CollectionRequest req = pending();
            req.clearDomainEvents();

            req.approve(7L);

            assertThat(req.getStatus()).isEqualTo(RequestStatus.APPROVED);
            assertThat(req.getApprovedByUserId()).isEqualTo(7L);
        }

        @Test
        @DisplayName("publishes CollectionRequestApproved event")
        void publishesEvent() {
            CollectionRequest req = pending();
            req.clearDomainEvents();
            req.approve(7L);

            assertThat(req.getDomainEvents()).hasSize(1);
            assertThat(req.getDomainEvents().get(0)).isInstanceOf(CollectionRequestApproved.class);
        }

        @Test
        @DisplayName("throws when request is already APPROVED")
        void throwsOnAlreadyApproved() {
            CollectionRequest req = approved();
            assertThatThrownBy(() -> req.approve(1L)).isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("throws when request is CANCELLED")
        void throwsOnCancelled() {
            CollectionRequest req = cancelled();
            assertThatThrownBy(() -> req.approve(1L)).isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("throws when request is COMPLETED")
        void throwsOnCompleted() {
            CollectionRequest req = completed();
            assertThatThrownBy(() -> req.approve(1L)).isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("reject")
    class Reject {

        @Test
        @DisplayName("transitions PENDING → REJECTED and sets rejectionReason")
        void rejectsFromPending() {
            CollectionRequest req = pending();
            req.reject("Stok bilgisi hatalı");

            assertThat(req.getStatus()).isEqualTo(RequestStatus.REJECTED);
            assertThat(req.getRejectionReason()).isEqualTo("Stok bilgisi hatalı");
        }

        @Test
        @DisplayName("publishes CollectionRequestRejected event")
        void publishesEvent() {
            CollectionRequest req = pending();
            req.clearDomainEvents();
            req.reject("sebep");

            assertThat(req.getDomainEvents()).hasSize(1);
            assertThat(req.getDomainEvents().get(0)).isInstanceOf(CollectionRequestRejected.class);
        }

        @Test
        @DisplayName("throws when request is APPROVED")
        void throwsOnApproved() {
            CollectionRequest req = approved();
            assertThatThrownBy(() -> req.reject("sebep")).isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("throws when request is already REJECTED")
        void throwsOnAlreadyRejected() {
            CollectionRequest req = pending();
            req.reject("ilk red");
            assertThatThrownBy(() -> req.reject("ikinci red")).isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("cancel")
    class Cancel {

        @Test
        @DisplayName("transitions PENDING → CANCELLED")
        void cancelsFromPending() {
            CollectionRequest req = pending();
            req.cancel();
            assertThat(req.getStatus()).isEqualTo(RequestStatus.CANCELLED);
        }

        @Test
        @DisplayName("transitions APPROVED → CANCELLED")
        void cancelsFromApproved() {
            CollectionRequest req = approved();
            req.cancel();
            assertThat(req.getStatus()).isEqualTo(RequestStatus.CANCELLED);
        }

        @Test
        @DisplayName("transitions SCHEDULED → CANCELLED")
        void cancelsFromScheduled() {
            CollectionRequest req = scheduled();
            req.cancel();
            assertThat(req.getStatus()).isEqualTo(RequestStatus.CANCELLED);
        }

        @Test
        @DisplayName("publishes CollectionRequestCancelled event")
        void publishesEvent() {
            CollectionRequest req = pending();
            req.clearDomainEvents();
            req.cancel();

            assertThat(req.getDomainEvents()).hasSize(1);
            assertThat(req.getDomainEvents().get(0)).isInstanceOf(CollectionRequestCancelled.class);
        }

        @Test
        @DisplayName("throws when request is COMPLETED")
        void throwsOnCompleted() {
            CollectionRequest req = completed();
            assertThatThrownBy(() -> req.cancel()).isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("throws when request is already CANCELLED")
        void throwsOnAlreadyCancelled() {
            CollectionRequest req = cancelled();
            assertThatThrownBy(() -> req.cancel()).isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("schedule")
    class Schedule {

        @Test
        @DisplayName("transitions APPROVED → SCHEDULED and sets collectionPlanId")
        void schedulesFromApproved() {
            CollectionRequest req = approved();
            req.schedule(42L);

            assertThat(req.getStatus()).isEqualTo(RequestStatus.SCHEDULED);
            assertThat(req.getCollectionPlanId()).isEqualTo(42L);
        }

        @Test
        @DisplayName("throws when request is PENDING")
        void throwsOnPending() {
            CollectionRequest req = pending();
            assertThatThrownBy(() -> req.schedule(1L)).isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("throws when request is COMPLETED")
        void throwsOnCompleted() {
            assertThatThrownBy(() -> completed().schedule(1L)).isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("complete")
    class Complete {

        @Test
        @DisplayName("transitions SCHEDULED → COMPLETED")
        void completesFromScheduled() {
            CollectionRequest req = scheduled();
            req.complete();
            assertThat(req.getStatus()).isEqualTo(RequestStatus.COMPLETED);
        }

        @Test
        @DisplayName("throws when request is PENDING")
        void throwsOnPending() {
            assertThatThrownBy(() -> pending().complete()).isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("throws when request is APPROVED")
        void throwsOnApproved() {
            assertThatThrownBy(() -> approved().complete()).isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("updateQuantity")
    class UpdateQuantity {

        @Test
        @DisplayName("updates quantity when PENDING")
        void updatesWhenPending() {
            CollectionRequest req = pending();
            req.updateQuantity(200);
            assertThat(req.getEstimatedQuantity()).isEqualTo(200);
        }

        @Test
        @DisplayName("throws when quantity is zero")
        void throwsOnZeroQuantity() {
            CollectionRequest req = pending();
            assertThatThrownBy(() -> req.updateQuantity(0)).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("throws when quantity is negative")
        void throwsOnNegativeQuantity() {
            CollectionRequest req = pending();
            assertThatThrownBy(() -> req.updateQuantity(-5)).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("throws when request is APPROVED")
        void throwsOnApproved() {
            assertThatThrownBy(() -> approved().updateQuantity(100)).isInstanceOf(IllegalStateException.class);
        }
    }

    private CollectionRequest pending() {
        return CollectionRequest.createManual(1L, AssetType.PALLET, 100, 1L);
    }

    private CollectionRequest approved() {
        CollectionRequest r = pending();
        r.approve(1L);
        return r;
    }

    private CollectionRequest scheduled() {
        CollectionRequest r = approved();
        r.schedule(1L);
        return r;
    }

    private CollectionRequest completed() {
        CollectionRequest r = scheduled();
        r.complete();
        return r;
    }

    private CollectionRequest cancelled() {
        CollectionRequest r = pending();
        r.cancel();
        return r;
    }
}
