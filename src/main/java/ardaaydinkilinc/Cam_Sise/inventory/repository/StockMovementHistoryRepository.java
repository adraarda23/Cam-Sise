package ardaaydinkilinc.Cam_Sise.inventory.repository;

import ardaaydinkilinc.Cam_Sise.inventory.domain.StockMovementHistory;
import ardaaydinkilinc.Cam_Sise.inventory.domain.vo.AssetType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StockMovementHistoryRepository extends JpaRepository<StockMovementHistory, Long> {

    List<StockMovementHistory> findByFillerIdAndAssetTypeOrderByOccurredAtAsc(Long fillerId, AssetType assetType);

    List<StockMovementHistory> findByFillerIdAndAssetTypeAndOccurredAtBetweenOrderByOccurredAtAsc(
            Long fillerId,
            AssetType assetType,
            LocalDateTime from,
            LocalDateTime to
    );

    @Query("""
        SELECT m FROM StockMovementHistory m
        WHERE m.fillerId = :fillerId
          AND m.assetType = :assetType
          AND m.occurredAt >= :since
        ORDER BY m.occurredAt ASC
    """)
    List<StockMovementHistory> findRecent(
            @Param("fillerId") Long fillerId,
            @Param("assetType") AssetType assetType,
            @Param("since") LocalDateTime since
    );

    @Query("""
        SELECT m FROM StockMovementHistory m
        WHERE m.fillerId IN (
            SELECT f.id FROM Filler f WHERE f.poolOperatorId = :poolOperatorId
        )
          AND m.occurredAt >= :since
        ORDER BY m.occurredAt ASC
    """)
    List<StockMovementHistory> findByPoolOperatorIdSince(
            @Param("poolOperatorId") Long poolOperatorId,
            @Param("since") LocalDateTime since
    );

    long countByFillerIdAndAssetType(Long fillerId, AssetType assetType);
}
