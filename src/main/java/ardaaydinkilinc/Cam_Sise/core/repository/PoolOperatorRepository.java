package ardaaydinkilinc.Cam_Sise.core.repository;

import ardaaydinkilinc.Cam_Sise.core.domain.PoolOperator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PoolOperatorRepository extends JpaRepository<PoolOperator, Long> {

    Optional<PoolOperator> findByTaxId_Value(String taxId);

    List<PoolOperator> findByActive(Boolean active);

    boolean existsByTaxId_Value(String taxId);
}
