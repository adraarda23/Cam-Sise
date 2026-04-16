package ardaaydinkilinc.Cam_Sise.inventory.repository;

import ardaaydinkilinc.Cam_Sise.inventory.domain.LossRecord;
import ardaaydinkilinc.Cam_Sise.inventory.domain.vo.AssetType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LossRecordRepository extends JpaRepository<LossRecord, Long> {

    Optional<LossRecord> findByFillerIdAndAssetType(Long fillerId, AssetType assetType);

    List<LossRecord> findByFillerId(Long fillerId);

    List<LossRecord> findByAssetType(AssetType assetType);
}
