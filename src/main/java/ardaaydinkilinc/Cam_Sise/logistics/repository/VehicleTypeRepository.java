package ardaaydinkilinc.Cam_Sise.logistics.repository;

import ardaaydinkilinc.Cam_Sise.logistics.domain.VehicleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VehicleTypeRepository extends JpaRepository<VehicleType, Long> {

    List<VehicleType> findByPoolOperatorId(Long poolOperatorId);

    List<VehicleType> findByPoolOperatorIdAndActive(Long poolOperatorId, Boolean active);

    List<VehicleType> findByActive(Boolean active);
}
