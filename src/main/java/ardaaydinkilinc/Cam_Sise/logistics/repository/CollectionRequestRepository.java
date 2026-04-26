package ardaaydinkilinc.Cam_Sise.logistics.repository;

import ardaaydinkilinc.Cam_Sise.inventory.domain.vo.AssetType;
import ardaaydinkilinc.Cam_Sise.logistics.domain.CollectionRequest;
import ardaaydinkilinc.Cam_Sise.logistics.domain.vo.RequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CollectionRequestRepository extends JpaRepository<CollectionRequest, Long> {

    List<CollectionRequest> findByFillerId(Long fillerId);

    List<CollectionRequest> findByStatus(RequestStatus status);

    List<CollectionRequest> findByFillerIdAndStatus(Long fillerId, RequestStatus status);

    List<CollectionRequest> findByAssetType(AssetType assetType);

    List<CollectionRequest> findByCollectionPlanId(Long collectionPlanId);

    List<CollectionRequest> findByFillerIdAndAssetTypeAndStatusIn(Long fillerId, AssetType assetType, List<RequestStatus> statuses);

    @Query("SELECT r FROM CollectionRequest r WHERE r.fillerId IN (SELECT f.id FROM Filler f WHERE f.poolOperatorId = :poolOperatorId)")
    List<CollectionRequest> findByPoolOperatorId(@Param("poolOperatorId") Long poolOperatorId);

    @Query("SELECT r FROM CollectionRequest r WHERE r.fillerId IN (SELECT f.id FROM Filler f WHERE f.poolOperatorId = :poolOperatorId) AND (:status IS NULL OR r.status = :status) AND (:assetType IS NULL OR r.assetType = :assetType)")
    Page<CollectionRequest> findByPoolOperatorIdFiltered(
            @Param("poolOperatorId") Long poolOperatorId,
            @Param("status") RequestStatus status,
            @Param("assetType") AssetType assetType,
            Pageable pageable
    );
}
