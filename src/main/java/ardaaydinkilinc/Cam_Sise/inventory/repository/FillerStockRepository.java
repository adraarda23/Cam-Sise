package ardaaydinkilinc.Cam_Sise.inventory.repository;

import ardaaydinkilinc.Cam_Sise.inventory.domain.FillerStock;
import ardaaydinkilinc.Cam_Sise.inventory.domain.vo.AssetType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FillerStockRepository extends JpaRepository<FillerStock, Long> {

    Optional<FillerStock> findByFillerIdAndAssetType(Long fillerId, AssetType assetType);

    List<FillerStock> findByFillerId(Long fillerId);

    List<FillerStock> findByAssetType(AssetType assetType);

    @Query("SELECT s FROM FillerStock s WHERE s.fillerId IN (SELECT f.id FROM Filler f WHERE f.poolOperatorId = :poolOperatorId)")
    List<FillerStock> findByPoolOperatorId(@Param("poolOperatorId") Long poolOperatorId);

    @Query("SELECT s FROM FillerStock s WHERE s.fillerId IN " +
           "(SELECT f.id FROM Filler f WHERE f.poolOperatorId = :poolOperatorId " +
           "AND ('' = :search OR f.name ILIKE CONCAT('%', :search, '%')))")
    Page<FillerStock> findByPoolOperatorIdFiltered(
            @Param("poolOperatorId") Long poolOperatorId,
            @Param("search") String search,
            Pageable pageable
    );
}
