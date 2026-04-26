package ardaaydinkilinc.Cam_Sise.logistics.controller;

import ardaaydinkilinc.Cam_Sise.inventory.domain.vo.AssetType;
import ardaaydinkilinc.Cam_Sise.logistics.service.CollectionRequestService;
import ardaaydinkilinc.Cam_Sise.logistics.domain.CollectionRequest;
import ardaaydinkilinc.Cam_Sise.logistics.domain.vo.RequestStatus;
import ardaaydinkilinc.Cam_Sise.shared.dto.PageResponse;
import ardaaydinkilinc.Cam_Sise.shared.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Logistics - Collection Requests", description = "Toplama talepleri yönetimi API'leri")
@SecurityRequirement(name = "bearerAuth")
public class CollectionRequestController {

    private final CollectionRequestService collectionRequestService;
    private final JwtUtil jwtUtil;

    /**
     * Create a manual collection request (initiated by customer)
     */
    @Operation(
            summary = "Manuel toplama talebi oluştur",
            description = "Dolumcu tarafından manuel olarak toplama talebi oluşturur. CUSTOMER rolü kendi dolumcusu için talep oluşturabilir."
    )
    @ApiResponse(responseCode = "201", description = "Talep başarıyla oluşturuldu")
    @ApiResponse(responseCode = "400", description = "Geçersiz request parametreleri")
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
    @Operation(summary = "Toplama talebini onayla", description = "COMPANY_STAFF tarafından toplama talebini onaylar. Onaylanan talepler rota optimizasyonuna dahil edilir.")
    @ApiResponse(responseCode = "200", description = "Talep başarıyla onaylandı")
    @PostMapping("/{requestId}/approve")
    @PreAuthorize("hasRole('COMPANY_STAFF')")
    public ResponseEntity<CollectionRequest> approveRequest(
            @Parameter(description = "Collection request ID") @PathVariable Long requestId,
            @RequestBody ApproveRequestRequest request
    ) {
        CollectionRequest collectionRequest = collectionRequestService.approve(requestId, request.approvingUserId);
        return ResponseEntity.ok(collectionRequest);
    }

    /**
     * Reject a collection request
     */
    @PostMapping("/{requestId}/reject")
    @PreAuthorize("hasRole('COMPANY_STAFF')")
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
    @Operation(summary = "Tüm toplama taleplerini listele", description = "Filtrelemelerle tüm toplama taleplerini listeler. COMPANY_STAFF tüm talepleri görebilir.")
    @ApiResponse(responseCode = "200", description = "Talep listesi başarıyla döndürüldü")
    @GetMapping
    @PreAuthorize("hasRole('COMPANY_STAFF')")
    public ResponseEntity<PageResponse<CollectionRequest>> getAllRequests(
            @Parameter(description = "Duruma göre filtrele") @RequestParam(required = false) RequestStatus status,
            @Parameter(description = "Asset tipine göre filtrele") @RequestParam(required = false) AssetType assetType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest httpRequest
    ) {
        Long poolOperatorId = jwtUtil.extractPoolOperatorId(httpRequest.getHeader("Authorization").substring(7));
        return ResponseEntity.ok(collectionRequestService.findByPoolOperatorIdPaged(poolOperatorId, status, assetType, page, size));
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

    @Schema(description = "Manuel toplama talebi oluşturma request DTO")
    public record CreateManualRequestRequest(
            @Schema(description = "Dolumcu ID", example = "1", required = true)
            Long fillerId,

            @Schema(description = "Asset tipi (PALLET veya SEPARATOR)", example = "PALLET", required = true)
            AssetType assetType,

            @Schema(description = "Tahmini toplama miktarı", example = "50", required = true)
            Integer estimatedQuantity,

            @Schema(description = "Talep oluşturan kullanıcı ID (opsiyonel)", example = "1")
            Long requestingUserId
    ) {}

    @Schema(description = "Toplama talebi onaylama request DTO")
    public record ApproveRequestRequest(
            @Schema(description = "Onaylayan kullanıcı ID", example = "1", required = true)
            Long approvingUserId
    ) {}

    @Schema(description = "Toplama talebi reddetme request DTO")
    public record RejectRequestRequest(
            @Schema(description = "Red sebebi", example = "Dolumcu stok bilgisi hatalı", required = true)
            String reason
    ) {}
}
