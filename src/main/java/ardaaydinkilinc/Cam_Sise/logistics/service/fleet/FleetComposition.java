package ardaaydinkilinc.Cam_Sise.logistics.service.fleet;

import ardaaydinkilinc.Cam_Sise.logistics.domain.vo.Capacity;

import java.util.List;

/**
 * A concrete proposal for the fleet that should serve a given demand.
 *
 * <p>Each {@link FleetComposition} is one candidate the user can pick from on
 * the "Filo Önerisi" UI. Typical responses contain 2-3 alternatives
 * (cheapest, fastest, fewest vehicles) so the operator can pick the
 * trade-off they care about today.
 */
public record FleetComposition(
        String label,
        String reason,
        List<VehicleAssignment> assignments,
        Capacity totalCapacity,
        Capacity totalDemand,
        double estimatedCostTRY,
        double slackPercent,
        int vehicleCount
) {

    public record VehicleAssignment(
            Long vehicleTypeId,
            String vehicleTypeName,
            int count,
            Capacity capacityPerVehicle
    ) {}
}
