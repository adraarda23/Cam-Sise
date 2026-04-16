package ardaaydinkilinc.Cam_Sise.core.repository;

import ardaaydinkilinc.Cam_Sise.core.domain.Filler;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FillerRepository extends JpaRepository<Filler, Long> {

    List<Filler> findByPoolOperatorId(Long poolOperatorId);

    List<Filler> findByPoolOperatorIdAndActive(Long poolOperatorId, Boolean active);

    List<Filler> findByActive(Boolean active);

    Optional<Filler> findByTaxId_Value(String taxId);

    boolean existsByTaxId_Value(String taxId);
}
