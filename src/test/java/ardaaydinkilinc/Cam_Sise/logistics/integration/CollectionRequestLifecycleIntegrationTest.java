package ardaaydinkilinc.Cam_Sise.logistics.integration;

import ardaaydinkilinc.Cam_Sise.core.domain.Filler;
import ardaaydinkilinc.Cam_Sise.core.domain.PoolOperator;
import ardaaydinkilinc.Cam_Sise.core.repository.FillerRepository;
import ardaaydinkilinc.Cam_Sise.core.repository.PoolOperatorRepository;
import ardaaydinkilinc.Cam_Sise.inventory.domain.FillerStock;
import ardaaydinkilinc.Cam_Sise.inventory.domain.vo.AssetType;
import ardaaydinkilinc.Cam_Sise.inventory.domain.vo.LossRate;
import ardaaydinkilinc.Cam_Sise.inventory.repository.FillerStockRepository;
import ardaaydinkilinc.Cam_Sise.logistics.domain.CollectionRequest;
import ardaaydinkilinc.Cam_Sise.logistics.domain.vo.RequestStatus;
import ardaaydinkilinc.Cam_Sise.logistics.repository.CollectionRequestRepository;
import ardaaydinkilinc.Cam_Sise.logistics.service.CollectionRequestService;
import ardaaydinkilinc.Cam_Sise.settings.repository.CompanySettingsRepository;
import ardaaydinkilinc.Cam_Sise.shared.domain.vo.Address;
import ardaaydinkilinc.Cam_Sise.shared.domain.vo.ContactInfo;
import ardaaydinkilinc.Cam_Sise.shared.domain.vo.GeoCoordinates;
import ardaaydinkilinc.Cam_Sise.shared.domain.vo.TaxId;
import ardaaydinkilinc.Cam_Sise.shared.exception.BusinessRuleViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for CollectionRequest full lifecycle using H2 in-memory database.
 * Covers the core business rules that protect against regressions.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("CollectionRequest Lifecycle Integration Tests")
class CollectionRequestLifecycleIntegrationTest {

    @Autowired private CollectionRequestService collectionRequestService;
    @Autowired private CollectionRequestRepository collectionRequestRepository;
    @Autowired private FillerRepository fillerRepository;
    @Autowired private FillerStockRepository fillerStockRepository;
    @Autowired private PoolOperatorRepository poolOperatorRepository;
    @Autowired private CompanySettingsRepository companySettingsRepository;

    private static final Long USER_ID = 99L;

    private Long poolOperatorId;
    private Long fillerId;

    @BeforeEach
    void setUp() {
        collectionRequestRepository.deleteAll();
        fillerStockRepository.deleteAll();
        companySettingsRepository.deleteAll();
        fillerRepository.deleteAll();
        poolOperatorRepository.deleteAll();

        PoolOperator po = PoolOperator.register(
                "Test Havuz",
                new TaxId("5555555555"),
                new ContactInfo("05550000000", "po@test.com", "Admin"));
        po = poolOperatorRepository.save(po);
        poolOperatorId = po.getId();

        Filler filler = Filler.register(
                poolOperatorId, "Test Dolumcu",
                new Address("Sk. 1", "İstanbul", "İstanbul", "34000", "TR"),
                new GeoCoordinates(41.0, 29.0),
                new ContactInfo("05551111111", "f@test.com", "Müdür"),
                null);
        filler.clearDomainEvents();
        filler = fillerRepository.save(filler);
        fillerId = filler.getId();

        FillerStock stock = FillerStock.initialize(fillerId, AssetType.PALLET, 50, new LossRate(5.0));
        stock.recordInflow(500, "INIT");
        stock.clearDomainEvents();
        fillerStockRepository.save(stock);
    }

    // ─── Full lifecycle ───────────────────────────────────────────────────

    @Nested
    @DisplayName("Full lifecycle: createManual → approve → schedule → complete")
    class FullLifecycle {

        @Test
        @DisplayName("request transitions through all statuses to COMPLETED")
        void fullLifecycle() {
            // 1. Create
            CollectionRequest req = collectionRequestService.createManual(
                    fillerId, AssetType.PALLET, 100, USER_ID, poolOperatorId);

            assertThat(req.getId()).isNotNull();
            assertThat(req.getStatus()).isEqualTo(RequestStatus.PENDING);
            assertThat(req.getEstimatedQuantity()).isEqualTo(100);

            // 2. Approve
            CollectionRequest approved = collectionRequestService.approve(req.getId(), USER_ID);
            assertThat(approved.getStatus()).isEqualTo(RequestStatus.APPROVED);

            // 3. Schedule
            CollectionRequest scheduled = collectionRequestService.schedule(req.getId(), 42L);
            assertThat(scheduled.getStatus()).isEqualTo(RequestStatus.SCHEDULED);
            assertThat(scheduled.getCollectionPlanId()).isEqualTo(42L);

            // 4. Complete
            CollectionRequest completed = collectionRequestService.complete(req.getId());
            assertThat(completed.getStatus()).isEqualTo(RequestStatus.COMPLETED);

            // Verify persisted state
            CollectionRequest fromDb = collectionRequestRepository.findById(req.getId()).orElseThrow();
            assertThat(fromDb.getStatus()).isEqualTo(RequestStatus.COMPLETED);
        }

        @Test
        @DisplayName("request can be cancelled from PENDING")
        void cancelsFromPending() {
            CollectionRequest req = collectionRequestService.createManual(
                    fillerId, AssetType.PALLET, 100, USER_ID, poolOperatorId);

            CollectionRequest cancelled = collectionRequestService.cancel(req.getId());

            assertThat(cancelled.getStatus()).isEqualTo(RequestStatus.CANCELLED);
        }

        @Test
        @DisplayName("request can be rejected from PENDING with reason")
        void rejectsFromPending() {
            CollectionRequest req = collectionRequestService.createManual(
                    fillerId, AssetType.PALLET, 100, USER_ID, poolOperatorId);

            CollectionRequest rejected = collectionRequestService.reject(req.getId(), "Stok yetersiz");

            assertThat(rejected.getStatus()).isEqualTo(RequestStatus.REJECTED);
            assertThat(rejected.getRejectionReason()).isEqualTo("Stok yetersiz");
        }
    }

    // ─── Business rule regressions ────────────────────────────────────────

    @Nested
    @DisplayName("Minimum quantity enforcement (regression)")
    class MinimumQuantity {

        @Test
        @DisplayName("rejects request below default minimum (20 pallets)")
        void rejectsBelowDefaultMinimum() {
            assertThatThrownBy(() ->
                    collectionRequestService.createManual(
                            fillerId, AssetType.PALLET, 5, USER_ID, poolOperatorId))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("20");
        }

        @Test
        @DisplayName("accepts request exactly at minimum (20 pallets)")
        void acceptsAtMinimum() {
            CollectionRequest req = collectionRequestService.createManual(
                    fillerId, AssetType.PALLET, 20, USER_ID, poolOperatorId);

            assertThat(req.getStatus()).isEqualTo(RequestStatus.PENDING);
        }
    }

    @Nested
    @DisplayName("Stock availability enforcement (regression)")
    class StockAvailability {

        @Test
        @DisplayName("rejects request exceeding available stock")
        void rejectsWhenExceedsStock() {
            assertThatThrownBy(() ->
                    collectionRequestService.createManual(
                            fillerId, AssetType.PALLET, 600, USER_ID, poolOperatorId))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("kullanılabilir stoktan");
        }

        @Test
        @DisplayName("approved requests reduce available stock for new requests")
        void approvedRequestsReduceAvailableStock() {
            // Approve a request for 400 → only 100 of the 500 stock remains
            CollectionRequest first = collectionRequestService.createManual(
                    fillerId, AssetType.PALLET, 400, USER_ID, poolOperatorId);
            collectionRequestService.approve(first.getId(), USER_ID);

            // Now requesting 150 should fail (400 approved + 150 new = 550 > 500)
            assertThatThrownBy(() ->
                    collectionRequestService.createManual(
                            fillerId, AssetType.PALLET, 150, USER_ID, poolOperatorId))
                    .isInstanceOf(BusinessRuleViolationException.class);
        }
    }

    @Nested
    @DisplayName("Request merging (regression)")
    class RequestMerging {

        @Test
        @DisplayName("second PENDING request merges quantity into existing PENDING request")
        void mergesIntoPendingRequest() {
            // First request
            collectionRequestService.createManual(fillerId, AssetType.PALLET, 100, USER_ID, poolOperatorId);

            // Second request — should merge into first
            CollectionRequest merged = collectionRequestService.createManual(
                    fillerId, AssetType.PALLET, 50, USER_ID, poolOperatorId);

            assertThat(merged.getEstimatedQuantity()).isEqualTo(150);
            assertThat(collectionRequestRepository.findAll()).hasSize(1);
        }

        @Test
        @DisplayName("merge is rejected when total would exceed stock")
        void rejectsMergeWhenTotalExceedsStock() {
            collectionRequestService.createManual(fillerId, AssetType.PALLET, 400, USER_ID, poolOperatorId);

            assertThatThrownBy(() ->
                    collectionRequestService.createManual(
                            fillerId, AssetType.PALLET, 200, USER_ID, poolOperatorId))
                    .isInstanceOf(BusinessRuleViolationException.class);

            assertThat(collectionRequestRepository.findAll()).hasSize(1);
        }
    }
}
