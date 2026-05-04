package ardaaydinkilinc.Cam_Sise.logistics.integration;

import ardaaydinkilinc.Cam_Sise.core.domain.Filler;
import ardaaydinkilinc.Cam_Sise.core.domain.PoolOperator;
import ardaaydinkilinc.Cam_Sise.core.repository.FillerRepository;
import ardaaydinkilinc.Cam_Sise.core.repository.PoolOperatorRepository;
import ardaaydinkilinc.Cam_Sise.inventory.domain.vo.AssetType;
import ardaaydinkilinc.Cam_Sise.logistics.domain.CollectionPlan;
import ardaaydinkilinc.Cam_Sise.logistics.domain.CollectionRequest;
import ardaaydinkilinc.Cam_Sise.logistics.domain.Depot;
import ardaaydinkilinc.Cam_Sise.logistics.domain.VehicleType;
import ardaaydinkilinc.Cam_Sise.logistics.domain.vo.Capacity;
import ardaaydinkilinc.Cam_Sise.logistics.domain.vo.RequestStatus;
import ardaaydinkilinc.Cam_Sise.logistics.repository.CollectionPlanRepository;
import ardaaydinkilinc.Cam_Sise.logistics.repository.CollectionRequestRepository;
import ardaaydinkilinc.Cam_Sise.logistics.repository.DepotRepository;
import ardaaydinkilinc.Cam_Sise.logistics.repository.VehicleTypeRepository;
import ardaaydinkilinc.Cam_Sise.logistics.service.RouteOptimizationService;
import ardaaydinkilinc.Cam_Sise.shared.domain.vo.Address;
import ardaaydinkilinc.Cam_Sise.shared.domain.vo.ContactInfo;
import ardaaydinkilinc.Cam_Sise.shared.domain.vo.GeoCoordinates;
import ardaaydinkilinc.Cam_Sise.shared.domain.vo.TaxId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Route Optimization Integration Tests")
class RouteOptimizationIntegrationTest {

    @Autowired private RouteOptimizationService routeOptimizationService;
    @Autowired private CollectionRequestRepository collectionRequestRepository;
    @Autowired private CollectionPlanRepository collectionPlanRepository;
    @Autowired private DepotRepository depotRepository;
    @Autowired private FillerRepository fillerRepository;
    @Autowired private VehicleTypeRepository vehicleTypeRepository;
    @Autowired private PoolOperatorRepository poolOperatorRepository;

    private PoolOperator poolOperator;
    private Depot depot;
    private VehicleType vehicleType;
    private List<Filler> fillers;
    private int taxIdCounter = 1000000000;

    @BeforeEach
    void setUp() {
        collectionPlanRepository.deleteAll();
        collectionRequestRepository.deleteAll();
        vehicleTypeRepository.deleteAll();
        depotRepository.deleteAll();
        fillerRepository.deleteAll();
        poolOperatorRepository.deleteAll();

        poolOperator = PoolOperator.register(
                "Test Havuz A.Ş.",
                new TaxId("1234567890"),
                new ContactInfo("05321234567", "test@havuz.com", "Test Contact")
        );
        poolOperator = poolOperatorRepository.save(poolOperator);

        depot = Depot.create(
                poolOperator.getId(),
                "Test Depot",
                new Address("Depo Caddesi No:1", "Gemlik", "Bursa", "16600", "Türkiye"),
                new GeoCoordinates(40.4333, 29.1667)
        );
        depot = depotRepository.save(depot);

        vehicleType = VehicleType.create(
                poolOperator.getId(),
                "Test Truck",
                "Large capacity truck",
                new Capacity(1500, 1500)
        );
        vehicleType = vehicleTypeRepository.save(vehicleType);

        fillers = List.of(
                createFiller("Filler 1", 40.19, 29.37),
                createFiller("Filler 2", 40.21, 29.38),
                createFiller("Filler 3", 40.18, 29.35),
                createFiller("Filler 4", 40.22, 29.40),
                createFiller("Filler 5", 40.20, 29.36)
        );
        fillers = fillerRepository.saveAll(fillers);
    }

    private Filler createFiller(String name, double latitude, double longitude) {
        return Filler.register(
                poolOperator.getId(),
                name,
                new Address("Street", "City", "Province", "12345", "Türkiye"),
                new GeoCoordinates(latitude, longitude),
                new ContactInfo("0532000000", "filler@test.com", "Contact"),
                new TaxId(String.valueOf(taxIdCounter++))
        );
    }

    @Test
    @DisplayName("Should generate multi-vehicle routes for approved requests")
    void testMultiVehicleOptimization_WithApprovedRequests() {
        for (Filler filler : fillers) {
            CollectionRequest request = CollectionRequest.createManual(filler.getId(), AssetType.PALLET, 35, 1L);
            request.approve(1L);
            collectionRequestRepository.save(request);
        }

        LocalDate plannedDate = LocalDate.now().plusDays(1);

        List<CollectionPlan> plans = routeOptimizationService.generateMultiVehiclePlan(
                depot.getId(), plannedDate, 5);

        assertThat(plans).isNotEmpty();
        assertThat(plans.size()).isGreaterThan(0).isLessThanOrEqualTo(5);

        for (CollectionPlan plan : plans) {
            assertThat(plan.getDepotId()).isEqualTo(depot.getId());
            assertThat(plan.getPlannedDate()).isEqualTo(plannedDate);
            assertThat(plan.getStatus()).isEqualTo(ardaaydinkilinc.Cam_Sise.logistics.domain.vo.PlanStatus.GENERATED);
            assertThat(plan.getTotalDistance().kilometers()).isGreaterThan(0);
            assertThat(plan.getTotalDistance().kilometers()).isLessThanOrEqualTo(800.0);
            assertThat(plan.getEstimatedDuration().minutes()).isLessThanOrEqualTo(600);
        }

        int totalPallets = plans.stream().mapToInt(CollectionPlan::getTotalCapacityPallets).sum();
        assertThat(totalPallets).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should enforce 800km distance constraint")
    void testDistanceConstraintEnforcement() {
        for (Filler filler : fillers) {
            CollectionRequest request = CollectionRequest.createManual(filler.getId(), AssetType.PALLET, 30, 1L);
            request.approve(1L);
            collectionRequestRepository.save(request);
        }

        List<CollectionPlan> plans = routeOptimizationService.generateMultiVehiclePlan(
                depot.getId(), LocalDate.now().plusDays(1), 10);

        for (CollectionPlan plan : plans) {
            assertThat(plan.getTotalDistance().kilometers())
                    .isLessThanOrEqualTo(800.0)
                    .withFailMessage("Route exceeds 800km constraint: " + plan.getTotalDistance().kilometers());
        }
    }

    @Test
    @DisplayName("Should enforce 10-hour duration constraint")
    void testDurationConstraintEnforcement() {
        for (Filler filler : fillers) {
            CollectionRequest request = CollectionRequest.createManual(filler.getId(), AssetType.SEPARATOR, 40, 1L);
            request.approve(1L);
            collectionRequestRepository.save(request);
        }

        List<CollectionPlan> plans = routeOptimizationService.generateMultiVehiclePlan(
                depot.getId(), LocalDate.now().plusDays(1), 10);

        for (CollectionPlan plan : plans) {
            assertThat(plan.getEstimatedDuration().minutes())
                    .isLessThanOrEqualTo(600)
                    .withFailMessage("Route exceeds 10-hour constraint: " + plan.getEstimatedDuration().minutes() + " minutes");
        }
    }

    @Test
    @DisplayName("Should create multi-stop routes when fillers are nearby")
    void testMultiStopRouteGeneration() {
        List<CollectionRequest> requests = fillers.subList(0, 3).stream()
                .map(filler -> {
                    CollectionRequest request = CollectionRequest.createManual(filler.getId(), AssetType.PALLET, 25, 1L);
                    request.approve(1L);
                    return request;
                })
                .toList();
        collectionRequestRepository.saveAll(requests);

        List<CollectionPlan> plans = routeOptimizationService.generateMultiVehiclePlan(
                depot.getId(), LocalDate.now().plusDays(1), 10);

        assertThat(plans.size()).isLessThan(requests.size());

        boolean hasMultiStopRoute = plans.stream()
                .anyMatch(plan -> plan.getRouteStopsJson().contains("sequence\":2"));
        assertThat(hasMultiStopRoute).isTrue();
    }

    @Test
    @DisplayName("Should handle capacity constraints")
    void testCapacityConstraintEnforcement() {
        for (Filler filler : fillers) {
            CollectionRequest request = CollectionRequest.createManual(filler.getId(), AssetType.PALLET, 300, 1L);
            request.approve(1L);
            collectionRequestRepository.save(request);
        }

        List<CollectionPlan> plans = routeOptimizationService.generateMultiVehiclePlan(
                depot.getId(), LocalDate.now().plusDays(1), 10);

        for (CollectionPlan plan : plans) {
            assertThat(plan.getTotalCapacityPallets()).isLessThanOrEqualTo(vehicleType.getCapacity().pallets());
            assertThat(plan.getTotalCapacitySeparators()).isLessThanOrEqualTo(vehicleType.getCapacity().separators());
        }

        int totalPlanned = plans.stream().mapToInt(CollectionPlan::getTotalCapacityPallets).sum();
        assertThat(totalPlanned).isEqualTo(1500);
    }

    @Test
    @DisplayName("Should throw exception when no approved requests exist")
    void testNoApprovedRequests_ThrowsException() {
        CollectionRequest pendingRequest = CollectionRequest.createManual(
                fillers.get(0).getId(), AssetType.PALLET, 30, 1L);
        collectionRequestRepository.save(pendingRequest);

        assertThatThrownBy(() ->
                routeOptimizationService.generateMultiVehiclePlan(
                        depot.getId(), LocalDate.now().plusDays(1), 10)
        ).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No approved collection requests found");
    }

    @Test
    @DisplayName("Should generate plans for specific approved requests only")
    void testCustomRequestOptimization() {
        List<CollectionRequest> allRequests = fillers.stream()
                .map(filler -> {
                    CollectionRequest request = CollectionRequest.createManual(filler.getId(), AssetType.PALLET, 30, 1L);
                    request.approve(1L);
                    return collectionRequestRepository.save(request);
                })
                .toList();

        List<Long> selectedIds = allRequests.subList(0, 2).stream()
                .map(CollectionRequest::getId)
                .toList();

        CollectionPlan plan = routeOptimizationService.generatePlanForRequests(
                depot.getId(), selectedIds, LocalDate.now().plusDays(1));

        assertThat(plan).isNotNull();
        assertThat(plan.getDepotId()).isEqualTo(depot.getId());

        String routeStops = plan.getRouteStopsJson();
        for (int i = 0; i < 2; i++) {
            Long fillerId = allRequests.get(i).getFillerId();
            assertThat(routeStops).contains("\"fillerId\":" + fillerId);
        }
    }

    @Test
    @DisplayName("Should persist plans to database")
    void testPlanPersistence() {
        for (Filler filler : fillers.subList(0, 3)) {
            CollectionRequest request = CollectionRequest.createManual(filler.getId(), AssetType.PALLET, 30, 1L);
            request.approve(1L);
            collectionRequestRepository.save(request);
        }

        List<CollectionPlan> plans = routeOptimizationService.generateMultiVehiclePlan(
                depot.getId(), LocalDate.now().plusDays(1), 10);

        assertThat(plans).allMatch(plan -> plan.getId() != null);

        List<CollectionPlan> retrievedPlans = collectionPlanRepository.findAll();
        assertThat(retrievedPlans).hasSameSizeAs(plans);
    }

    @Test
    @DisplayName("Should handle mixed asset types (pallets and separators)")
    void testMixedAssetTypes() {
        CollectionRequest palletRequest = CollectionRequest.createManual(fillers.get(0).getId(), AssetType.PALLET, 40, 1L);
        palletRequest.approve(1L);

        CollectionRequest separatorRequest = CollectionRequest.createManual(fillers.get(1).getId(), AssetType.SEPARATOR, 35, 1L);
        separatorRequest.approve(1L);

        collectionRequestRepository.saveAll(List.of(palletRequest, separatorRequest));

        List<CollectionPlan> plans = routeOptimizationService.generateMultiVehiclePlan(
                depot.getId(), LocalDate.now().plusDays(1), 10);

        assertThat(plans).isNotEmpty();

        int totalPallets = plans.stream().mapToInt(CollectionPlan::getTotalCapacityPallets).sum();
        int totalSeparators = plans.stream().mapToInt(CollectionPlan::getTotalCapacitySeparators).sum();

        assertThat(totalPallets).isGreaterThan(0);
        assertThat(totalSeparators).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should handle edge case with single approved request")
    void testSingleApprovedRequest() {
        CollectionRequest singleRequest = CollectionRequest.createManual(fillers.get(0).getId(), AssetType.PALLET, 50, 1L);
        singleRequest.approve(1L);
        collectionRequestRepository.save(singleRequest);

        List<CollectionPlan> plans = routeOptimizationService.generateMultiVehiclePlan(
                depot.getId(), LocalDate.now().plusDays(1), 10);

        assertThat(plans).hasSize(1);
        assertThat(plans.get(0).getTotalCapacityPallets()).isEqualTo(50);
    }

    @Test
    @DisplayName("Should calculate realistic distances using Haversine formula")
    void testRealisticDistanceCalculation() {
        for (Filler filler : fillers.subList(0, 3)) {
            CollectionRequest request = CollectionRequest.createManual(filler.getId(), AssetType.PALLET, 30, 1L);
            request.approve(1L);
            collectionRequestRepository.save(request);
        }

        List<CollectionPlan> plans = routeOptimizationService.generateMultiVehiclePlan(
                depot.getId(), LocalDate.now().plusDays(1), 10);

        for (CollectionPlan plan : plans) {
            double distance = plan.getTotalDistance().kilometers();
            assertThat(distance).isGreaterThan(0);
            assertThat(distance).isLessThan(200);
        }
    }
}
