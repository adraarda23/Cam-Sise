package ardaaydinkilinc.Cam_Sise.logistics.repository;

import ardaaydinkilinc.Cam_Sise.logistics.domain.CollectionPlan;
import ardaaydinkilinc.Cam_Sise.logistics.domain.vo.PlanStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @Query("SELECT p FROM CollectionPlan p WHERE p.depotId IN (SELECT d.id FROM Depot d WHERE d.poolOperatorId = :poolOperatorId)")
    List<CollectionPlan> findByPoolOperatorId(@Param("poolOperatorId") Long poolOperatorId);

    @Query("SELECT p FROM CollectionPlan p WHERE p.depotId IN (SELECT d.id FROM Depot d WHERE d.poolOperatorId = :poolOperatorId) AND (:status IS NULL OR p.status = :status) AND (:startDate IS NULL OR p.plannedDate >= :startDate) AND (:endDate IS NULL OR p.plannedDate <= :endDate)")
    Page<CollectionPlan> findByPoolOperatorIdFiltered(
            @Param("poolOperatorId") Long poolOperatorId,
            @Param("status") PlanStatus status,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable
    );
}
