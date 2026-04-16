package ardaaydinkilinc.Cam_Sise.logistics.repository;

import ardaaydinkilinc.Cam_Sise.logistics.domain.CollectionPlan;
import ardaaydinkilinc.Cam_Sise.logistics.domain.vo.PlanStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface CollectionPlanRepository extends JpaRepository<CollectionPlan, Long> {

    List<CollectionPlan> findByDepotId(Long depotId);

    List<CollectionPlan> findByStatus(PlanStatus status);

    List<CollectionPlan> findByDepotIdAndStatus(Long depotId, PlanStatus status);

    List<CollectionPlan> findByAssignedVehicleId(Long vehicleId);

    List<CollectionPlan> findByPlannedDate(LocalDate plannedDate);

    List<CollectionPlan> findByPlannedDateBetween(LocalDate startDate, LocalDate endDate);
}
