package ardaaydinkilinc.Cam_Sise.logistics.repository;

import ardaaydinkilinc.Cam_Sise.inventory.domain.vo.AssetType;
import ardaaydinkilinc.Cam_Sise.logistics.domain.CollectionRequest;
import ardaaydinkilinc.Cam_Sise.logistics.domain.vo.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CollectionRequestRepository extends JpaRepository<CollectionRequest, Long> {

    List<CollectionRequest> findByFillerId(Long fillerId);

    List<CollectionRequest> findByStatus(RequestStatus status);

    List<CollectionRequest> findByFillerIdAndStatus(Long fillerId, RequestStatus status);

    List<CollectionRequest> findByAssetType(AssetType assetType);

    List<CollectionRequest> findByCollectionPlanId(Long collectionPlanId);
}
