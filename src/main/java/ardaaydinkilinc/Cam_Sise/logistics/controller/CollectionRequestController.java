package ardaaydinkilinc.Cam_Sise.logistics.controller;

import ardaaydinkilinc.Cam_Sise.inventory.domain.vo.AssetType;
import ardaaydinkilinc.Cam_Sise.logistics.service.CollectionRequestService;
import ardaaydinkilinc.Cam_Sise.logistics.domain.CollectionRequest;
import ardaaydinkilinc.Cam_Sise.logistics.domain.vo.RequestStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API for CollectionRequest management.
 */
@RestController
@RequestMapping("/api/logistics/collection-requests")
@RequiredArgsConstructor
public class CollectionRequestController {

    private final CollectionRequestService collectionRequestService;

    /**
     * Create a manual collection request (initiated by customer)
     */
    @PostMapping("/manual")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_STAFF', 'CUSTOMER')")
    public ResponseEntity<CollectionRequest> createManualRequest(@RequestBody CreateManualRequestRequest request) {
        // Use null for requestingUserId if not provided (can be enhanced with @AuthenticationPrincipal later)
        Long requestingUserId = request.requestingUserId != null ? request.requestingUserId : 1L;

        CollectionRequest collectionRequest = collectionRequestService.createManual(
                request.fillerId,
                request.assetType,
                request.estimatedQuantity,
                requestingUserId
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(collectionRequest);
    }

    /**
     * Approve a collection request
     */
    @PostMapping("/{requestId}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_STAFF')")
    public ResponseEntity<CollectionRequest> approveRequest(
            @PathVariable Long requestId,
            @RequestBody ApproveRequestRequest request
    ) {
        CollectionRequest collectionRequest = collectionRequestService.approve(requestId, request.approvingUserId);
        return ResponseEntity.ok(collectionRequest);
    }

    /**
     * Reject a collection request
     */
    @PostMapping("/{requestId}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_STAFF')")
    public ResponseEntity<CollectionRequest> rejectRequest(
            @PathVariable Long requestId,
            @RequestBody RejectRequestRequest request
    ) {
        CollectionRequest collectionRequest = collectionRequestService.reject(requestId, request.reason);
        return ResponseEntity.ok(collectionRequest);
    }

    /**
     * Cancel a collection request
     */
    @PostMapping("/{requestId}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_STAFF', 'CUSTOMER')")
    public ResponseEntity<CollectionRequest> cancelRequest(@PathVariable Long requestId) {
        CollectionRequest collectionRequest = collectionRequestService.cancel(requestId);
        return ResponseEntity.ok(collectionRequest);
    }

    /**
     * Get collection request by ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_STAFF', 'CUSTOMER')")
    public ResponseEntity<CollectionRequest> getRequest(@PathVariable Long id) {
        CollectionRequest collectionRequest = collectionRequestService.findById(id);
        return ResponseEntity.ok(collectionRequest);
    }

    /**
     * Get all collection requests (with optional filters)
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_STAFF')")
    public ResponseEntity<List<CollectionRequest>> getAllRequests(
            @RequestParam(required = false) Long fillerId,
            @RequestParam(required = false) RequestStatus status,
            @RequestParam(required = false) AssetType assetType
    ) {
        List<CollectionRequest> requests;
        if (fillerId != null) {
            requests = collectionRequestService.findByFiller(fillerId, status);
        } else if (status != null) {
            requests = collectionRequestService.findByStatus(status);
        } else if (assetType != null) {
            requests = collectionRequestService.findByAssetType(assetType);
        } else {
            requests = collectionRequestService.findAll();
        }
        return ResponseEntity.ok(requests);
    }

    /**
     * Get requests for a specific filler
     */
    @GetMapping("/filler/{fillerId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_STAFF', 'CUSTOMER')")
    public ResponseEntity<List<CollectionRequest>> getRequestsByFiller(
            @PathVariable Long fillerId,
            @RequestParam(required = false) RequestStatus status
    ) {
        List<CollectionRequest> requests = collectionRequestService.findByFiller(fillerId, status);
        return ResponseEntity.ok(requests);
    }

    // ===== DTOs =====

    public record CreateManualRequestRequest(
            Long fillerId,
            AssetType assetType,
            Integer estimatedQuantity,
            Long requestingUserId
    ) {}

    public record ApproveRequestRequest(
            Long approvingUserId
    ) {}

    public record RejectRequestRequest(
            String reason
    ) {}
}
