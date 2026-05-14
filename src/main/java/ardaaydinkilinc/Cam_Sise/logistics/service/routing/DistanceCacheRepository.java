package ardaaydinkilinc.Cam_Sise.logistics.service.routing;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface DistanceCacheRepository extends JpaRepository<DistanceCacheEntry, Long> {

    Optional<DistanceCacheEntry> findByFromLatAndFromLonAndToLatAndToLon(
            double fromLat, double fromLon, double toLat, double toLon);

    @Modifying
    @Query("DELETE FROM DistanceCacheEntry e WHERE e.cachedAt < :cutoff")
    int deleteOlderThan(@Param("cutoff") LocalDateTime cutoff);

    long count();
}
