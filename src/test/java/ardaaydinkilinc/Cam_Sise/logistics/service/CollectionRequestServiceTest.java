package ardaaydinkilinc.Cam_Sise.logistics.service;

import ardaaydinkilinc.Cam_Sise.inventory.domain.FillerStock;
import ardaaydinkilinc.Cam_Sise.inventory.domain.vo.AssetType;
import ardaaydinkilinc.Cam_Sise.inventory.domain.vo.LossRate;
import ardaaydinkilinc.Cam_Sise.inventory.service.FillerStockService;
import ardaaydinkilinc.Cam_Sise.logistics.domain.CollectionRequest;
import ardaaydinkilinc.Cam_Sise.logistics.domain.vo.RequestStatus;
import ardaaydinkilinc.Cam_Sise.logistics.repository.CollectionRequestRepository;
import ardaaydinkilinc.Cam_Sise.settings.domain.CompanySettings;
import ardaaydinkilinc.Cam_Sise.settings.service.CompanySettingsService;
import ardaaydinkilinc.Cam_Sise.shared.exception.BusinessRuleViolationException;
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
@DisplayName("CollectionRequestService Tests")
class CollectionRequestServiceTest {

    @Mock private CollectionRequestRepository collectionRequestRepository;
    @Mock private FillerStockService fillerStockService;
    @Mock private CompanySettingsService companySettingsService;

    @InjectMocks
    private CollectionRequestService service;

    private static final Long FILLER_ID = 1L;
    private static final Long POOL_OPERATOR_ID = 1L;
    private static final Long USER_ID = 10L;

    private FillerStock palletStock;
    private CompanySettings settings;

    @BeforeEach
    void setUp() {
        palletStock = FillerStock.initialize(FILLER_ID, AssetType.PALLET, 50, new LossRate(5.0));
        palletStock.recordInflow(500, "INF-001");
        palletStock.clearDomainEvents();

        settings = new CompanySettings(POOL_OPERATOR_ID);
        settings.setMinPalletRequestQty(20);
        settings.setMinSeparatorRequestQty(10);
    }

    // ─── createManual ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("createManual")
    class CreateManual {

        @Test
        @DisplayName("throws BusinessRuleViolationException when quantity is below minimum")
        void throwsWhenBelowMinimum() {
            when(companySettingsService.getSettings(POOL_OPERATOR_ID)).thenReturn(settings);

            assertThatThrownBy(() ->
                    service.createManual(FILLER_ID, AssetType.PALLET, 5, USER_ID, POOL_OPERATOR_ID))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("Minimum toplama talebi 20 adettir");

            verify(collectionRequestRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws BusinessRuleViolationException when quantity exceeds available stock")
        void throwsWhenExceedsStock() {
            when(companySettingsService.getSettings(POOL_OPERATOR_ID)).thenReturn(settings);
            when(fillerStockService.getStock(FILLER_ID, AssetType.PALLET)).thenReturn(palletStock);
            when(collectionRequestRepository.findByFillerIdAndAssetTypeAndStatusIn(
                    eq(FILLER_ID), eq(AssetType.PALLET), any())).thenReturn(List.of());

            assertThatThrownBy(() ->
                    service.createManual(FILLER_ID, AssetType.PALLET, 600, USER_ID, POOL_OPERATOR_ID))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("kullanılabilir stoktan");

            verify(collectionRequestRepository, never()).save(any());
        }

        @Test
        @DisplayName("creates new request when no PENDING request exists")
        void createsNewRequest() {
            when(companySettingsService.getSettings(POOL_OPERATOR_ID)).thenReturn(settings);
            when(fillerStockService.getStock(FILLER_ID, AssetType.PALLET)).thenReturn(palletStock);
            when(collectionRequestRepository.findByFillerIdAndAssetTypeAndStatusIn(
                    eq(FILLER_ID), eq(AssetType.PALLET), any())).thenReturn(List.of());
            when(collectionRequestRepository.save(any(CollectionRequest.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            CollectionRequest result = service.createManual(FILLER_ID, AssetType.PALLET, 100, USER_ID, POOL_OPERATOR_ID);

            assertThat(result.getStatus()).isEqualTo(RequestStatus.PENDING);
            assertThat(result.getEstimatedQuantity()).isEqualTo(100);
            verify(collectionRequestRepository).save(any(CollectionRequest.class));
        }

        @Test
        @DisplayName("merges quantity into existing PENDING request")
        void mergesWithExistingPendingRequest() {
            CollectionRequest existingPending = CollectionRequest.createManual(FILLER_ID, AssetType.PALLET, 100, USER_ID);

            when(companySettingsService.getSettings(POOL_OPERATOR_ID)).thenReturn(settings);
            when(fillerStockService.getStock(FILLER_ID, AssetType.PALLET)).thenReturn(palletStock);
            when(collectionRequestRepository.findByFillerIdAndAssetTypeAndStatusIn(
                    eq(FILLER_ID), eq(AssetType.PALLET), any())).thenReturn(List.of(existingPending));
            when(collectionRequestRepository.save(any(CollectionRequest.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            CollectionRequest result = service.createManual(FILLER_ID, AssetType.PALLET, 50, USER_ID, POOL_OPERATOR_ID);

            assertThat(result.getEstimatedQuantity()).isEqualTo(150);
            verify(collectionRequestRepository).save(existingPending);
        }

        @Test
        @DisplayName("throws when merge total would exceed available stock")
        void throwsWhenMergeTotalExceedsStock() {
            CollectionRequest existingPending = CollectionRequest.createManual(FILLER_ID, AssetType.PALLET, 400, USER_ID);

            when(companySettingsService.getSettings(POOL_OPERATOR_ID)).thenReturn(settings);
            when(fillerStockService.getStock(FILLER_ID, AssetType.PALLET)).thenReturn(palletStock);
            when(collectionRequestRepository.findByFillerIdAndAssetTypeAndStatusIn(
                    eq(FILLER_ID), eq(AssetType.PALLET), any())).thenReturn(List.of(existingPending));

            // existing=400, new=200, total=600 > stock=500
            assertThatThrownBy(() ->
                    service.createManual(FILLER_ID, AssetType.PALLET, 200, USER_ID, POOL_OPERATOR_ID))
                    .isInstanceOf(BusinessRuleViolationException.class);

            verify(collectionRequestRepository, never()).save(any());
        }

        @Test
        @DisplayName("accounts for APPROVED requests when calculating available stock")
        void subtractsApprovedRequestsFromAvailable() {
            CollectionRequest approvedReq = CollectionRequest.createManual(FILLER_ID, AssetType.PALLET, 400, USER_ID);
            approvedReq.approve(USER_ID);

            when(companySettingsService.getSettings(POOL_OPERATOR_ID)).thenReturn(settings);
            when(fillerStockService.getStock(FILLER_ID, AssetType.PALLET)).thenReturn(palletStock);
            when(collectionRequestRepository.findByFillerIdAndAssetTypeAndStatusIn(
                    eq(FILLER_ID), eq(AssetType.PALLET), any())).thenReturn(List.of(approvedReq));

            // stock=500, approved=400, available=100 → requesting 150 should fail
            assertThatThrownBy(() ->
                    service.createManual(FILLER_ID, AssetType.PALLET, 150, USER_ID, POOL_OPERATOR_ID))
                    .isInstanceOf(BusinessRuleViolationException.class);
        }
    }

    // ─── createAutomatic ──────────────────────────────────────────────────

    @Nested
    @DisplayName("createAutomatic")
    class CreateAutomatic {

        @Test
        @DisplayName("creates request with PENDING status")
        void createsRequest() {
            when(collectionRequestRepository.save(any(CollectionRequest.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            CollectionRequest result = service.createAutomatic(FILLER_ID, AssetType.PALLET, 75);

            assertThat(result.getStatus()).isEqualTo(RequestStatus.PENDING);
            assertThat(result.getEstimatedQuantity()).isEqualTo(75);
            verify(collectionRequestRepository).save(any(CollectionRequest.class));
        }
    }

    // ─── approve ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("approve")
    class Approve {

        @Test
        @DisplayName("approves existing PENDING request")
        void approvesRequest() {
            CollectionRequest req = CollectionRequest.createManual(FILLER_ID, AssetType.PALLET, 100, USER_ID);
            when(collectionRequestRepository.findById(1L)).thenReturn(Optional.of(req));
            when(collectionRequestRepository.save(req)).thenReturn(req);

            CollectionRequest result = service.approve(1L, USER_ID);

            assertThat(result.getStatus()).isEqualTo(RequestStatus.APPROVED);
            verify(collectionRequestRepository).save(req);
        }

        @Test
        @DisplayName("throws IllegalArgumentException when request not found")
        void throwsWhenNotFound() {
            when(collectionRequestRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.approve(999L, USER_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("not found");
        }
    }

    // ─── reject ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("reject")
    class Reject {

        @Test
        @DisplayName("rejects existing PENDING request with reason")
        void rejectsRequest() {
            CollectionRequest req = CollectionRequest.createManual(FILLER_ID, AssetType.PALLET, 100, USER_ID);
            when(collectionRequestRepository.findById(1L)).thenReturn(Optional.of(req));
            when(collectionRequestRepository.save(req)).thenReturn(req);

            CollectionRequest result = service.reject(1L, "Stok yetersiz");

            assertThat(result.getStatus()).isEqualTo(RequestStatus.REJECTED);
            assertThat(result.getRejectionReason()).isEqualTo("Stok yetersiz");
        }

        @Test
        @DisplayName("throws when request not found")
        void throwsWhenNotFound() {
            when(collectionRequestRepository.findById(999L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.reject(999L, "sebep"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ─── cancel ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("cancel")
    class Cancel {

        @Test
        @DisplayName("cancels existing request")
        void cancelsRequest() {
            CollectionRequest req = CollectionRequest.createManual(FILLER_ID, AssetType.PALLET, 100, USER_ID);
            when(collectionRequestRepository.findById(1L)).thenReturn(Optional.of(req));
            when(collectionRequestRepository.save(req)).thenReturn(req);

            CollectionRequest result = service.cancel(1L);

            assertThat(result.getStatus()).isEqualTo(RequestStatus.CANCELLED);
        }

        @Test
        @DisplayName("throws when request not found")
        void throwsWhenNotFound() {
            when(collectionRequestRepository.findById(999L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.cancel(999L))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ─── schedule / complete ──────────────────────────────────────────────

    @Nested
    @DisplayName("schedule")
    class Schedule {

        @Test
        @DisplayName("schedules APPROVED request with planId")
        void schedulesRequest() {
            CollectionRequest req = CollectionRequest.createManual(FILLER_ID, AssetType.PALLET, 100, USER_ID);
            req.approve(USER_ID);
            when(collectionRequestRepository.findById(1L)).thenReturn(Optional.of(req));
            when(collectionRequestRepository.save(req)).thenReturn(req);

            CollectionRequest result = service.schedule(1L, 42L);

            assertThat(result.getStatus()).isEqualTo(RequestStatus.SCHEDULED);
            assertThat(result.getCollectionPlanId()).isEqualTo(42L);
        }
    }

    @Nested
    @DisplayName("complete")
    class Complete {

        @Test
        @DisplayName("completes SCHEDULED request")
        void completesRequest() {
            CollectionRequest req = CollectionRequest.createManual(FILLER_ID, AssetType.PALLET, 100, USER_ID);
            req.approve(USER_ID);
            req.schedule(1L);
            when(collectionRequestRepository.findById(1L)).thenReturn(Optional.of(req));
            when(collectionRequestRepository.save(req)).thenReturn(req);

            CollectionRequest result = service.complete(1L);

            assertThat(result.getStatus()).isEqualTo(RequestStatus.COMPLETED);
        }
    }
}
