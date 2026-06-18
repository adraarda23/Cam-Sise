package ardaaydinkilinc.Cam_Sise.logistics.service.routing;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persists {@link DistanceCacheEntry} rows in an isolated transaction.
 *
 * <p>Cache writes happen in the middle of a long, read-heavy route
 * optimization that runs inside the caller's transaction. If a write fails
 * (e.g. the {@code uq_distance_cache_pair} unique constraint is hit because the
 * same coordinate pair is written concurrently), the failure must not poison
 * the caller's Hibernate session — otherwise the next query in the optimization
 * triggers an auto-flush of the broken entity and throws
 * {@code AssertionFailure: null identifier}.
 *
 * <p>{@link Propagation#REQUIRES_NEW} suspends the caller's transaction and runs
 * the save in its own session. On failure that inner transaction rolls back
 * cleanly and the exception propagates to the caller, which logs and continues;
 * the caller's session is never touched by the failed entity.
 */
@Component
public class DistanceCacheWriter {

    private final DistanceCacheRepository cacheRepository;

    public DistanceCacheWriter(DistanceCacheRepository cacheRepository) {
        this.cacheRepository = cacheRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void write(DistanceCacheEntry entry) {
        cacheRepository.save(entry);
    }
}