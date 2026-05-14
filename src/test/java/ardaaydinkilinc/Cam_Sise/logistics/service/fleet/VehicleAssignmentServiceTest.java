package ardaaydinkilinc.Cam_Sise.logistics.service.fleet;

import ardaaydinkilinc.Cam_Sise.logistics.domain.VehicleType;
import ardaaydinkilinc.Cam_Sise.logistics.domain.vo.Capacity;
import ardaaydinkilinc.Cam_Sise.logistics.repository.VehicleTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class VehicleAssignmentServiceTest {

    @Mock private VehicleTypeRepository vehicleTypeRepository;

    @InjectMocks
    private VehicleAssignmentService service;

    private VehicleType small;
    private VehicleType medium;
    private VehicleType large;

    @BeforeEach
    void setUp() {
        small = VehicleType.create(1L, "Küçük", "S", new Capacity(500, 500));
        ReflectionTestUtils.setField(small, "id", 1L);
        medium = VehicleType.create(1L, "Orta", "M", new Capacity(1000, 1000));
        ReflectionTestUtils.setField(medium, "id", 2L);
        large = VehicleType.create(1L, "Büyük", "L", new Capacity(1500, 1500));
        ReflectionTestUtils.setField(large, "id", 3L);
    }

    @Test
    @DisplayName("Empty demand returns empty list")
    void emptyDemandEmptyResult() {
        List<FleetComposition> result = service.suggestForTypes(
                List.of(small, medium, large), new Capacity(0, 0), 0);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Tiny demand → one-vehicle composition with the smallest truck that fits")
    void tinyDemandUsesOneSmallTruck() {
        List<FleetComposition> result = service.suggestForTypes(
                List.of(small, medium, large), new Capacity(50, 50), 100);

        assertThat(result).isNotEmpty();
        // All strategies should fit demand in one truck; "Dengeli" picks the smallest.
        FleetComposition first = result.get(0);
        assertThat(first.vehicleCount()).isEqualTo(1);
        assertThat(first.totalCapacity().pallets()).isGreaterThanOrEqualTo(50);
        // The smallest truck (500) should be selected by at least one strategy
        assertThat(result.stream().anyMatch(c -> c.totalCapacity().pallets() == 500)).isTrue();
    }

    @Test
    @DisplayName("Large demand triggers multi-vehicle composition")
    void largeDemandTriggersMultipleVehicles() {
        // 2000 + 2000 demand cannot fit in a single 1500/1500 truck → at least 2 vehicles
        List<FleetComposition> result = service.suggestForTypes(
                List.of(small, medium, large), new Capacity(2000, 2000), 300);

        assertThat(result).isNotEmpty();
        for (FleetComposition c : result) {
            assertThat(c.vehicleCount()).isGreaterThanOrEqualTo(2);
            assertThat(c.totalCapacity().pallets()).isGreaterThanOrEqualTo(2000);
            assertThat(c.totalCapacity().separators()).isGreaterThanOrEqualTo(2000);
        }
    }

    @Test
    @DisplayName("Cheapest composition has the lowest estimatedCostTRY")
    void cheapestIsCheapest() {
        List<FleetComposition> result = service.suggestForTypes(
                List.of(small, medium, large), new Capacity(800, 800), 400);

        FleetComposition cheapest = pick(result, "En Ucuz");
        double cheapestCost = cheapest.estimatedCostTRY();
        for (FleetComposition c : result) {
            assertThat(cheapestCost).isLessThanOrEqualTo(c.estimatedCostTRY() + 0.01);
        }
    }

    @Test
    @DisplayName("Compositions report total capacity covering the demand")
    void everyCompositionCoversDemand() {
        Capacity demand = new Capacity(700, 700);
        List<FleetComposition> result = service.suggestForTypes(
                List.of(small, medium, large), demand, 200);

        for (FleetComposition c : result) {
            assertThat(c.totalCapacity().pallets()).isGreaterThanOrEqualTo(demand.pallets());
            assertThat(c.totalCapacity().separators()).isGreaterThanOrEqualTo(demand.separators());
        }
    }

    @Test
    @DisplayName("Slack percent is in [0, 100] and reflects unused capacity")
    void slackPercentReported() {
        List<FleetComposition> result = service.suggestForTypes(
                List.of(small, medium, large), new Capacity(100, 100), 50);

        assertThat(result).isNotEmpty();
        for (FleetComposition c : result) {
            assertThat(c.slackPercent()).isBetween(0.0, 100.0);
        }
        // A 200-unit demand in a 1000-unit truck → 80% slack
        FleetComposition first = result.get(0);
        if (first.totalCapacity().pallets() == 500) {
            // 200 demand / 1000 total = 80% slack
            assertThat(first.slackPercent()).isCloseTo(80.0, org.assertj.core.data.Offset.offset(0.1));
        }
    }

    private FleetComposition pick(List<FleetComposition> compositions, String label) {
        return compositions.stream()
                .filter(c -> c.label().equals(label))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing composition: " + label));
    }
}
