package ardaaydinkilinc.Cam_Sise.core.repository;

import ardaaydinkilinc.Cam_Sise.core.domain.Filler;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    Page<Filler> findByPoolOperatorId(Long poolOperatorId, Pageable pageable);

    Page<Filler> findByPoolOperatorIdAndActive(Long poolOperatorId, Boolean active, Pageable pageable);

    @Query("SELECT f FROM Filler f WHERE f.poolOperatorId = :poolOperatorId " +
           "AND (:active IS NULL OR f.active = :active) " +
           "AND ('' = :search OR f.name ILIKE CONCAT('%', :search, '%'))")
    Page<Filler> findByPoolOperatorIdFiltered(
            @Param("poolOperatorId") Long poolOperatorId,
            @Param("active") Boolean active,
            @Param("search") String search,
            Pageable pageable
    );
}
