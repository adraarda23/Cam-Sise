package ardaaydinkilinc.Cam_Sise.logistics.service.fleet;

import ardaaydinkilinc.Cam_Sise.logistics.domain.Vehicle;
import ardaaydinkilinc.Cam_Sise.logistics.domain.VehicleType;
import ardaaydinkilinc.Cam_Sise.logistics.domain.vo.Capacity;
import ardaaydinkilinc.Cam_Sise.logistics.domain.vo.VehicleStatus;
import ardaaydinkilinc.Cam_Sise.logistics.repository.VehicleRepository;
import ardaaydinkilinc.Cam_Sise.logistics.repository.VehicleTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Generates fleet-composition recommendations for a given total demand.
 *
 * <p>Three explicit strategies, each meaningfully different so the operator
 * sees a real trade-off:
 * <ul>
 *   <li><b>En Az Araç</b>: minimise vehicle count — pick the smallest single
 *       vehicle type that fits all demand, otherwise the largest available.</li>
 *   <li><b>En Ucuz</b>: minimise estimated cost — among vehicles that fit the
 *       demand, prefer the smallest one (less fixed cost, less idle capacity)
 *       and break ties by lower slack so demos don't show three identical cards.</li>
 *   <li><b>Dengeli</b>: pack from smallest upward; multi-vehicle layout when
 *       demand exceeds smallest vehicle's capacity. Highest packing density.</li>
 * </ul>
 *
 * <p>Two important fixes vs the previous implementation:
 * <ol>
 *   <li>Slack is now computed as <code>(capacity - demand) / capacity</code>,
 *       i.e. the fraction of the truck that goes empty. Bounded to [0, 100%]
 *       which makes physical sense, unlike the old formula which could yield
 *       650%.</li>
 *   <li>Availability of real {@link Vehicle} instances is respected: we never
 *       recommend more vehicles of a given type than are present in the
 *       fleet. If even the smallest type is short we still emit the
 *       composition but flag it via a clear reason text.</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class VehicleAssignmentService {

    private static final double FUEL_TRY_PER_KM = 12.0;
    private static final double FIXED_COST_PER_VEHICLE_TRY = 750.0;

    private final VehicleTypeRepository vehicleTypeRepository;
    private final VehicleRepository vehicleRepository;

    public List<FleetComposition> suggest(Long poolOperatorId, Capacity totalDemand, double estimatedRouteKm) {
        List<VehicleType> activeTypes = vehicleTypeRepository
                .findByPoolOperatorIdAndActive(poolOperatorId, true);
        if (activeTypes.isEmpty()) {
            log.warn("No active vehicle types for poolOperatorId={}", poolOperatorId);
            return Collections.emptyList();
        }

        Map<Long, Integer> available = countAvailableByType(activeTypes);
        return suggestForTypes(activeTypes, totalDemand, estimatedRouteKm, available);
    }

    public List<FleetComposition> suggestForTypes(
            List<VehicleType> types,
            Capacity totalDemand,
            double estimatedRouteKm
    ) {
        return suggestForTypes(types, totalDemand, estimatedRouteKm, Collections.emptyMap());
    }

    public List<FleetComposition> suggestForTypes(
            List<VehicleType> types,
            Capacity totalDemand,
            double estimatedRouteKm,
            Map<Long, Integer> availableByTypeId
    ) {
        if (totalDemand == null || (totalDemand.pallets() == 0 && totalDemand.separators() == 0)) {
            return Collections.emptyList();
        }

        List<VehicleType> sortedAsc = new ArrayList<>(types);
        sortedAsc.sort(Comparator.comparingInt(this::totalCapacityScore));
        List<VehicleType> sortedDesc = new ArrayList<>(sortedAsc);
        Collections.reverse(sortedDesc);

        FleetComposition cheapest = pickCheapestSingleType(sortedAsc, totalDemand, estimatedRouteKm, availableByTypeId);
        FleetComposition fewest = pickFewestVehicles(sortedDesc, totalDemand, estimatedRouteKm, availableByTypeId);
        FleetComposition balanced = pickBalanced(sortedAsc, totalDemand, estimatedRouteKm, availableByTypeId);

        List<FleetComposition> result = new ArrayList<>();
        if (cheapest != null) result.add(cheapest);
        if (fewest != null && !sameComposition(fewest, cheapest)) result.add(fewest);
        // Show "Dengeli" only when it genuinely lowers slack. Otherwise the
        // user sees two cards with identical capacity/slack — only the cost
        // differs, which is a misleading trade-off (more vehicles, same
        // packing density, higher cost = strictly worse).
        if (balanced != null
                && !sameComposition(balanced, cheapest)
                && !sameComposition(balanced, fewest)
                && balanced.slackPercent() + 0.5 < lowestSlackIn(result)) {
            result.add(balanced);
        }
        return result;
    }

    private double lowestSlackIn(List<FleetComposition> existing) {
        double min = 100.0;
        for (FleetComposition c : existing) {
            if (c.slackPercent() < min) min = c.slackPercent();
        }
        return min;
    }

    /**
     * Pick the cheapest single-type composition. Iterates types from smallest
     * to largest, computes how many of each are needed, scores by cost, and
     * picks the lowest. Smaller vehicles win on ties (lower idle capacity).
     */
    private FleetComposition pickCheapestSingleType(
            List<VehicleType> sortedAsc, Capacity demand, double estimatedKm,
            Map<Long, Integer> available) {

        FleetComposition best = null;
        for (VehicleType type : sortedAsc) {
            int needed = vehiclesNeededOfType(type, demand);
            if (needed == 0) continue;
            int cap = available.getOrDefault(type.getId(), Integer.MAX_VALUE);
            if (needed > cap) continue; // not enough vehicles of this type

            FleetComposition candidate = buildComposition(
                    sortedAsc,
                    Map.of(type.getId(), needed),
                    demand, estimatedKm,
                    "En Ucuz",
                    String.format("%d × %s — sadece bu tiple çözüm: en az atıl kapasite ve en düşük maliyet.",
                            needed, type.getName()));
            if (best == null
                    || candidate.estimatedCostTRY() < best.estimatedCostTRY()
                    || (candidate.estimatedCostTRY() == best.estimatedCostTRY()
                        && candidate.slackPercent() < best.slackPercent())) {
                best = candidate;
            }
        }
        return best;
    }

    /**
     * Pick the option with the fewest vehicles. Tries the largest type first;
     * if it can serve demand alone (and is in stock) that's the answer.
     * Otherwise greedily fills from largest downward.
     */
    private FleetComposition pickFewestVehicles(
            List<VehicleType> sortedDesc, Capacity demand, double estimatedKm,
            Map<Long, Integer> available) {

        // Single-type try: smallest type that can carry it all in 1 vehicle
        VehicleType singleVehicleSolution = null;
        for (int i = sortedDesc.size() - 1; i >= 0; i--) {
            VehicleType t = sortedDesc.get(i);
            if (canCarryAllInOne(t.getCapacity(), demand)
                    && available.getOrDefault(t.getId(), Integer.MAX_VALUE) >= 1) {
                singleVehicleSolution = t;
                break; // smallest such type — minimum slack with single vehicle
            }
        }
        if (singleVehicleSolution != null) {
            return buildComposition(
                    sortedDesc,
                    Map.of(singleVehicleSolution.getId(), 1),
                    demand, estimatedKm,
                    "En Az Araç",
                    String.format("1 × %s — talebi tek araçla karşılar, en düşük lojistik karmaşıklığı.",
                            singleVehicleSolution.getName()));
        }

        // Multi-vehicle: pack largest first
        Map<Long, Integer> counts = new LinkedHashMap<>();
        Capacity remaining = demand;
        for (VehicleType type : sortedDesc) {
            int cap = available.getOrDefault(type.getId(), Integer.MAX_VALUE);
            int used = 0;
            while (!remaining.isEmpty()
                    && used < cap
                    && canAtLeastPartiallyFill(type.getCapacity(), remaining)) {
                Capacity slice = capPerVehicle(type.getCapacity(), remaining);
                remaining = subtractClamped(remaining, slice);
                used++;
            }
            if (used > 0) counts.put(type.getId(), used);
            if (remaining.isEmpty()) break;
        }
        if (counts.isEmpty()) return null;

        String reason = remaining.isEmpty()
                ? "Mevcut filo ile en az sayıda araç kullanan kompozisyon."
                : "Filoda yeterli kapasite yok — eksik talep kalıyor. Yeni araç eklemeniz önerilir.";
        return buildComposition(sortedDesc, counts, demand, estimatedKm, "En Az Araç", reason);
    }

    /**
     * Balanced: pack from smallest upward. Multi-vehicle layout maximises
     * packing density (lowest slack) at the cost of more vehicles.
     */
    private FleetComposition pickBalanced(
            List<VehicleType> sortedAsc, Capacity demand, double estimatedKm,
            Map<Long, Integer> available) {

        Map<Long, Integer> counts = new LinkedHashMap<>();
        Capacity remaining = demand;
        for (VehicleType type : sortedAsc) {
            int cap = available.getOrDefault(type.getId(), Integer.MAX_VALUE);
            int used = 0;
            while (!remaining.isEmpty()
                    && used < cap
                    && canAtLeastPartiallyFill(type.getCapacity(), remaining)) {
                Capacity slice = capPerVehicle(type.getCapacity(), remaining);
                remaining = subtractClamped(remaining, slice);
                used++;
            }
            if (used > 0) counts.put(type.getId(), used);
            if (remaining.isEmpty()) break;
        }
        if (counts.isEmpty()) return null;
        return buildComposition(sortedAsc, counts, demand, estimatedKm,
                "Dengeli",
                "Küçük araçlardan başlayarak doldurur — en yüksek paketleme verimliliği, atıl kapasite minimum.");
    }

    private FleetComposition buildComposition(
            List<VehicleType> types, Map<Long, Integer> counts,
            Capacity demand, double estimatedKm,
            String label, String reason) {

        Map<Long, VehicleType> byId = new HashMap<>();
        types.forEach(t -> byId.put(t.getId(), t));

        List<FleetComposition.VehicleAssignment> assignments = new ArrayList<>();
        int totalPallets = 0, totalSeparators = 0, totalCount = 0;
        for (Map.Entry<Long, Integer> e : counts.entrySet()) {
            VehicleType t = byId.get(e.getKey());
            if (t == null) continue;
            int n = e.getValue();
            assignments.add(new FleetComposition.VehicleAssignment(
                    t.getId(), t.getName(), n, t.getCapacity()));
            totalPallets += t.getCapacity().pallets() * n;
            totalSeparators += t.getCapacity().separators() * n;
            totalCount += n;
        }
        Capacity totalCapacity = new Capacity(totalPallets, totalSeparators);

        double estimatedCost = totalCount * FIXED_COST_PER_VEHICLE_TRY + estimatedKm * FUEL_TRY_PER_KM;
        double slack = computeSlackPercent(totalCapacity, demand);

        return new FleetComposition(label, reason, assignments, totalCapacity, demand,
                estimatedCost, slack, totalCount);
    }

    /**
     * Slack as a fraction of total capacity that goes unused, in [0, 100].
     * Returns 0 when capacity is exactly demand (or less, which would be an
     * infeasible plan but we don't return negative numbers here).
     */
    private double computeSlackPercent(Capacity capacity, Capacity demand) {
        int capSum = capacity.pallets() + capacity.separators();
        int demandSum = demand.pallets() + demand.separators();
        if (capSum <= 0) return 0.0;
        int idle = Math.max(0, capSum - demandSum);
        return Math.min(100.0, 100.0 * idle / capSum);
    }

    private int totalCapacityScore(VehicleType t) {
        return t.getCapacity().pallets() + t.getCapacity().separators();
    }

    private boolean canCarryAllInOne(Capacity capacity, Capacity demand) {
        boolean palletsOk = demand.pallets() == 0 || (capacity.pallets() > 0 && capacity.pallets() >= demand.pallets());
        boolean separatorsOk = demand.separators() == 0 || (capacity.separators() > 0 && capacity.separators() >= demand.separators());
        return palletsOk && separatorsOk;
    }

    /**
     * Vehicles of this type required to carry the entire demand, treating
     * pallet and separator dimensions independently and taking the maximum.
     * Returns 0 if the type's capacity in any required dimension is zero.
     */
    private int vehiclesNeededOfType(VehicleType type, Capacity demand) {
        Capacity cap = type.getCapacity();
        if (demand.pallets() > 0 && cap.pallets() == 0) return 0;
        if (demand.separators() > 0 && cap.separators() == 0) return 0;
        int byPallets = demand.pallets() == 0 ? 0 : (int) Math.ceil((double) demand.pallets() / cap.pallets());
        int bySeparators = demand.separators() == 0 ? 0 : (int) Math.ceil((double) demand.separators() / cap.separators());
        return Math.max(byPallets, bySeparators);
    }

    private boolean canAtLeastPartiallyFill(Capacity capacity, Capacity remaining) {
        boolean palletsRoom = remaining.pallets() > 0 && capacity.pallets() > 0;
        boolean separatorsRoom = remaining.separators() > 0 && capacity.separators() > 0;
        return palletsRoom || separatorsRoom;
    }

    private Capacity capPerVehicle(Capacity capacity, Capacity remaining) {
        int p = Math.min(capacity.pallets(), remaining.pallets());
        int s = Math.min(capacity.separators(), remaining.separators());
        return new Capacity(p, s);
    }

    private Capacity subtractClamped(Capacity from, Capacity used) {
        int p = Math.max(0, from.pallets() - used.pallets());
        int s = Math.max(0, from.separators() - used.separators());
        return new Capacity(p, s);
    }

    /**
     * Count of currently-available vehicles per VehicleType, scoped to the
     * tenant's active types. Vehicles in AVAILABLE status only — others are busy.
     *
     * <p>Every given type gets an explicit entry (0 when none of its vehicles
     * are available). Without the explicit zero, a type whose vehicles are all
     * busy would be missing from the map and the
     * <code>getOrDefault(id, MAX_VALUE)</code> lookups in the strategy methods
     * would treat it as unlimited — recommending vehicles that don't exist.
     */
    private Map<Long, Integer> countAvailableByType(List<VehicleType> types) {
        Map<Long, Integer> result = new HashMap<>();
        types.forEach(t -> result.put(t.getId(), 0));
        for (Vehicle v : vehicleRepository.findByStatus(VehicleStatus.AVAILABLE)) {
            result.computeIfPresent(v.getVehicleTypeId(), (id, count) -> count + 1);
        }
        return result;
    }

    private boolean sameComposition(FleetComposition a, FleetComposition b) {
        if (a == null || b == null) return false;
        if (a.vehicleCount() != b.vehicleCount()) return false;
        if (a.assignments().size() != b.assignments().size()) return false;
        Map<Long, Integer> countsA = new HashMap<>();
        a.assignments().forEach(x -> countsA.put(x.vehicleTypeId(), x.count()));
        Map<Long, Integer> countsB = new HashMap<>();
        b.assignments().forEach(x -> countsB.put(x.vehicleTypeId(), x.count()));
        return countsA.equals(countsB);
    }
}
