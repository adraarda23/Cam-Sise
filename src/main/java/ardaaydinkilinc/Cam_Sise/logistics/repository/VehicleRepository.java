package ardaaydinkilinc.Cam_Sise.logistics.repository;

import ardaaydinkilinc.Cam_Sise.logistics.domain.Vehicle;
import ardaaydinkilinc.Cam_Sise.logistics.domain.vo.VehicleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
