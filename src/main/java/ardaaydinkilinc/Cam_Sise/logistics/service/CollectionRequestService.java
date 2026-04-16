package ardaaydinkilinc.Cam_Sise.logistics.service;

import ardaaydinkilinc.Cam_Sise.inventory.domain.vo.AssetType;
import ardaaydinkilinc.Cam_Sise.logistics.domain.CollectionRequest;
import ardaaydinkilinc.Cam_Sise.logistics.domain.vo.RequestStatus;
import ardaaydinkilinc.Cam_Sise.logistics.repository.CollectionRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Application service for CollectionRequest aggregate.
 * Manages collection requests from fillers.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CollectionRequestService {

    private final CollectionRequestRepository collectionRequestRepository;

    /**
     * Create an automatic collection request (triggered by threshold).
     */
    public CollectionRequest createAutomatic(
            Long fillerId,
            AssetType assetType,
            Integer estimatedQuantity
    ) {
        log.info("Creating automatic collection request: fillerId={}, assetType={}, estimatedQuantity={}",
                fillerId, assetType, estimatedQuantity);

        CollectionRequest request = CollectionRequest.createAutomatic(fillerId, assetType, estimatedQuantity);
        request = collectionRequestRepository.save(request);

        log.info("Automatic collection request created: id={}, fillerId={}", request.getId(), fillerId);

        return request;
    }

    /**
     * Create a manual collection request (initiated by customer).
     */
    public CollectionRequest createManual(
            Long fillerId,
            AssetType assetType,
            Integer estimatedQuantity,
            Long requestingUserId
    ) {
        log.info("Creating manual collection request: fillerId={}, assetType={}, userId={}",
                fillerId, assetType, requestingUserId);

        CollectionRequest request = CollectionRequest.createManual(
                fillerId, assetType, estimatedQuantity, requestingUserId);
        request = collectionRequestRepository.save(request);

        log.info("Manual collection request created: id={}, fillerId={}", request.getId(), fillerId);

        return request;
    }

    /**
     * Approve a collection request.
     */
    public CollectionRequest approve(Long requestId, Long approvingUserId) {
        log.info("Approving collection request: requestId={}, approvingUserId={}", requestId, approvingUserId);

        CollectionRequest request = collectionRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Collection request not found: " + requestId));

        request.approve(approvingUserId);
        request = collectionRequestRepository.save(request);

        log.info("Collection request approved: requestId={}", requestId);

        return request;
    }

    /**
     * Reject a collection request.
     */
    public CollectionRequest reject(Long requestId, String reason) {
        log.info("Rejecting collection request: requestId={}, reason={}", requestId, reason);

        CollectionRequest request = collectionRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Collection request not found: " + requestId));

        request.reject(reason);
        request = collectionRequestRepository.save(request);

        log.info("Collection request rejected: requestId={}", requestId);

        return request;
    }

    /**
     * Cancel a collection request.
     */
    public CollectionRequest cancel(Long requestId) {
        log.info("Cancelling collection request: requestId={}", requestId);

        CollectionRequest request = collectionRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Collection request not found: " + requestId));

        request.cancel();
        request = collectionRequestRepository.save(request);

        log.info("Collection request cancelled: requestId={}", requestId);

        return request;
    }

    /**
     * Schedule a collection request (include in collection plan).
     */
    public CollectionRequest schedule(Long requestId, Long planId) {
        log.info("Scheduling collection request: requestId={}, planId={}", requestId, planId);

        CollectionRequest request = collectionRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Collection request not found: " + requestId));

        request.schedule(planId);
        request = collectionRequestRepository.save(request);

        log.info("Collection request scheduled: requestId={}, planId={}", requestId, planId);

        return request;
    }

    /**
     * Mark a collection request as completed.
     */
    public CollectionRequest complete(Long requestId) {
        log.info("Completing collection request: requestId={}", requestId);

        CollectionRequest request = collectionRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Collection request not found: " + requestId));

        request.complete();
        request = collectionRequestRepository.save(request);

        log.info("Collection request completed: requestId={}", requestId);

        return request;
    }

    /**
     * Find collection request by ID.
     */
    @Transactional(readOnly = true)
    public CollectionRequest findById(Long requestId) {
        return collectionRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Collection request not found: " + requestId));
    }

    /**
     * Find all collection requests for a filler.
     */
    @Transactional(readOnly = true)
    public List<CollectionRequest> findByFiller(Long fillerId, RequestStatus status) {
        if (status != null) {
            return collectionRequestRepository.findByFillerIdAndStatus(fillerId, status);
        }
        return collectionRequestRepository.findByFillerId(fillerId);
    }

    /**
     * Find all collection requests by status.
     */
    @Transactional(readOnly = true)
    public List<CollectionRequest> findByStatus(RequestStatus status) {
        return collectionRequestRepository.findByStatus(status);
    }

    /**
     * Find all collection requests by asset type.
     */
    @Transactional(readOnly = true)
    public List<CollectionRequest> findByAssetType(AssetType assetType) {
        return collectionRequestRepository.findByAssetType(assetType);
    }

    /**
     * Find all collection requests.
     */
    @Transactional(readOnly = true)
    public List<CollectionRequest> findAll() {
        return collectionRequestRepository.findAll();
    }
}
