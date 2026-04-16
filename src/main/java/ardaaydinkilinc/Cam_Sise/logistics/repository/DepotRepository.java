package ardaaydinkilinc.Cam_Sise.logistics.repository;

import ardaaydinkilinc.Cam_Sise.logistics.domain.Depot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DepotRepository extends JpaRepository<Depot, Long> {

    List<Depot> findByPoolOperatorId(Long poolOperatorId);

    List<Depot> findByPoolOperatorIdAndActive(Long poolOperatorId, Boolean active);

    List<Depot> findByActive(Boolean active);
}
