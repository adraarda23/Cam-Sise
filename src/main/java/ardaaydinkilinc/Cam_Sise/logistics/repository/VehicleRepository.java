package ardaaydinkilinc.Cam_Sise.logistics.repository;

import ardaaydinkilinc.Cam_Sise.logistics.domain.Vehicle;
import ardaaydinkilinc.Cam_Sise.logistics.domain.vo.VehicleStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    Optional<Vehicle> findByPlateNumber(String plateNumber);

    List<Vehicle> findByDepotId(Long depotId);

    List<Vehicle> findByStatus(VehicleStatus status);

    List<Vehicle> findByDepotIdAndStatus(Long depotId, VehicleStatus status);

    boolean existsByPlateNumber(String plateNumber);

    @Query("SELECT v FROM Vehicle v WHERE v.depotId IN (SELECT d.id FROM Depot d WHERE d.poolOperatorId = :poolOperatorId)")
    List<Vehicle> findByPoolOperatorId(@Param("poolOperatorId") Long poolOperatorId);

    @Query("SELECT v FROM Vehicle v WHERE v.depotId IN (SELECT d.id FROM Depot d WHERE d.poolOperatorId = :poolOperatorId) " +
           "AND (:status IS NULL OR v.status = :status) " +
           "AND ('' = :search OR v.plateNumber ILIKE CONCAT('%', :search, '%'))")
    Page<Vehicle> findByPoolOperatorIdFiltered(
            @Param("poolOperatorId") Long poolOperatorId,
            @Param("status") VehicleStatus status,
            @Param("search") String search,
            Pageable pageable
    );
}
