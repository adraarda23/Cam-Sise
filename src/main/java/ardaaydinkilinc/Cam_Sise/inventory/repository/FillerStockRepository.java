package ardaaydinkilinc.Cam_Sise.inventory.repository;

import ardaaydinkilinc.Cam_Sise.inventory.domain.FillerStock;
import ardaaydinkilinc.Cam_Sise.inventory.domain.vo.AssetType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FillerStockRepository extends JpaRepository<FillerStock, Long> {

    Optional<FillerStock> findByFillerIdAndAssetType(Long fillerId, AssetType assetType);

    List<FillerStock> findByFillerId(Long fillerId);

    List<FillerStock> findByAssetType(AssetType assetType);
}
