package ardaaydinkilinc.Cam_Sise.inventory.api;

import ardaaydinkilinc.Cam_Sise.inventory.application.service.LossRecordService;
import ardaaydinkilinc.Cam_Sise.inventory.domain.LossRecord;
import ardaaydinkilinc.Cam_Sise.inventory.domain.vo.AssetType;
import ardaaydinkilinc.Cam_Sise.inventory.domain.vo.Period;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * REST API for LossRecord management.
 * Allows tracking and managing loss rates for assets at filler locations.
 */
@RestController
@RequestMapping("/api/inventory/loss-records")
@RequiredArgsConstructor
public class LossRecordController {

    private final LossRecordService lossRecordService;

    /**
     * Create a new loss record with estimated rate
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_STAFF')")
    public ResponseEntity<LossRecord> createLossRecord(@RequestBody CreateLossRecordRequest request) {
        Period period = new Period(request.periodStartDate, request.periodEndDate);
        LossRecord record = lossRecordService.createWithEstimate(
                request.fillerId,
                request.assetType,
                request.estimatedRatePercentage,
                period
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(record);
    }

    /**
     * Update actual loss rate (reported by filler)
     */
    @PutMapping("/{fillerId}/{assetType}/actual-rate")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_STAFF', 'CUSTOMER')")
    public ResponseEntity<LossRecord> updateActualRate(
            @PathVariable Long fillerId,
            @PathVariable AssetType assetType,
            @RequestBody UpdateActualRateRequest request
    ) {
        LossRecord record = lossRecordService.updateActualRate(
                fillerId,
                assetType,
                request.actualRatePercentage
        );
        return ResponseEntity.ok(record);
    }

    /**
     * Recalculate estimated rate
     */
    @PutMapping("/{fillerId}/{assetType}/estimated-rate")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_STAFF')")
    public ResponseEntity<LossRecord> recalculateEstimatedRate(
            @PathVariable Long fillerId,
            @PathVariable AssetType assetType,
            @RequestBody RecalculateEstimatedRateRequest request
    ) {
        Period period = new Period(request.periodStartDate, request.periodEndDate);
        LossRecord record = lossRecordService.recalculateEstimatedRate(
                fillerId,
                assetType,
                request.newEstimatedRatePercentage,
                period
        );
        return ResponseEntity.ok(record);
    }

    /**
     * Get loss record for a filler and asset type
     */
    @GetMapping("/{fillerId}/{assetType}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_STAFF', 'CUSTOMER')")
    public ResponseEntity<LossRecord> getLossRecord(
            @PathVariable Long fillerId,
            @PathVariable AssetType assetType
    ) {
        LossRecord record = lossRecordService.getLossRecord(fillerId, assetType);
        return ResponseEntity.ok(record);
    }

    /**
     * Get all loss records for a filler
     */
    @GetMapping("/filler/{fillerId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_STAFF', 'CUSTOMER')")
    public ResponseEntity<List<LossRecord>> getLossRecordsByFiller(@PathVariable Long fillerId) {
        List<LossRecord> records = lossRecordService.getLossRecordsByFiller(fillerId);
        return ResponseEntity.ok(records);
    }

    /**
     * Get all loss records for an asset type
     */
    @GetMapping("/asset-type/{assetType}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_STAFF')")
    public ResponseEntity<List<LossRecord>> getLossRecordsByAssetType(@PathVariable AssetType assetType) {
        List<LossRecord> records = lossRecordService.getLossRecordsByAssetType(assetType);
        return ResponseEntity.ok(records);
    }

    // ===== DTOs =====

    public record CreateLossRecordRequest(
            Long fillerId,
            AssetType assetType,
            double estimatedRatePercentage,
            LocalDate periodStartDate,
            LocalDate periodEndDate
    ) {}

    public record UpdateActualRateRequest(
            double actualRatePercentage
    ) {}

    public record RecalculateEstimatedRateRequest(
            double newEstimatedRatePercentage,
            LocalDate periodStartDate,
            LocalDate periodEndDate
    ) {}
}
