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

    /**
     * Pages by FILLER (not by stock record), so a single page corresponds to N fillers
     * and each filler's PALLET + SEPARATOR rows come together. Avoids the situation
     * where a filler's two stock rows split across pages.
     */
    @Query("SELECT f.id FROM Filler f WHERE f.poolOperatorId = :poolOperatorId " +
           "AND ('' = :search OR f.name ILIKE CONCAT('%', :search, '%'))")
    Page<Long> findFillerIdsByPoolOperatorIdFiltered(
            @Param("poolOperatorId") Long poolOperatorId,
            @Param("search") String search,
            Pageable pageable
    );

    List<FillerStock> findByFillerIdIn(List<Long> fillerIds);

    /**
     * Backfill için tek sorguda stok kaydı OLAN tüm dolumcu id'lerini döndürür.
     * Eski N+1 (dolumcu başına 2 sorgu) yerine tek sorgu.
     */
    @Query("SELECT DISTINCT s.fillerId FROM FillerStock s")
    List<Long> findAllDistinctFillerIds();
}
