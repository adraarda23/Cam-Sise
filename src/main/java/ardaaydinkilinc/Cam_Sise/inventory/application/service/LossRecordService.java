package ardaaydinkilinc.Cam_Sise.inventory.application.service;

import ardaaydinkilinc.Cam_Sise.inventory.domain.LossRecord;
import ardaaydinkilinc.Cam_Sise.inventory.domain.vo.AssetType;
import ardaaydinkilinc.Cam_Sise.inventory.domain.vo.LossRate;
import ardaaydinkilinc.Cam_Sise.inventory.domain.vo.Period;
import ardaaydinkilinc.Cam_Sise.inventory.repository.LossRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Application service for LossRecord aggregate.
 * Manages loss rate tracking and calculations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class LossRecordService {

    private final LossRecordRepository lossRecordRepository;

    /**
     * Create a new loss record with estimated rate.
     */
    public LossRecord createWithEstimate(
            Long fillerId,
            AssetType assetType,
            double estimatedRatePercentage,
            Period period
    ) {
        log.info("Creating loss record: fillerId={}, assetType={}, estimatedRate={}%",
                fillerId, assetType, estimatedRatePercentage);

        // Check if record already exists
        lossRecordRepository.findByFillerIdAndAssetType(fillerId, assetType)
                .ifPresent(existing -> {
                    throw new IllegalArgumentException(
                            "Loss record already exists for filler: " + fillerId + ", asset: " + assetType);
                });

        LossRecord record = LossRecord.createWithEstimate(
                fillerId,
                assetType,
                new LossRate(estimatedRatePercentage),
                period
        );

        record = lossRecordRepository.save(record);

        log.info("Loss record created successfully: id={}", record.getId());

        return record;
    }

    /**
     * Update actual loss rate (reported by filler).
     */
    public LossRecord updateActualRate(
            Long fillerId,
            AssetType assetType,
            double actualRatePercentage
    ) {
        log.info("Updating actual loss rate: fillerId={}, assetType={}, actualRate={}%",
                fillerId, assetType, actualRatePercentage);

        LossRecord record = lossRecordRepository.findByFillerIdAndAssetType(fillerId, assetType)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Loss record not found for filler: " + fillerId + ", asset: " + assetType));

        record.updateActualRate(new LossRate(actualRatePercentage));
        record = lossRecordRepository.save(record);

        log.info("Actual loss rate updated successfully: id={}", record.getId());

        return record;
    }

    /**
     * Recalculate estimated rate using moving average.
     */
    public LossRecord recalculateEstimatedRate(
            Long fillerId,
            AssetType assetType,
            double newEstimatedRatePercentage,
            Period newPeriod
    ) {
        log.info("Recalculating estimated loss rate: fillerId={}, assetType={}, newRate={}%",
                fillerId, assetType, newEstimatedRatePercentage);

        LossRecord record = lossRecordRepository.findByFillerIdAndAssetType(fillerId, assetType)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Loss record not found for filler: " + fillerId + ", asset: " + assetType));

        record.recalculateEstimatedRate(new LossRate(newEstimatedRatePercentage), newPeriod);
        record = lossRecordRepository.save(record);

        log.info("Estimated loss rate recalculated successfully: id={}", record.getId());

        return record;
    }

    /**
     * Get loss record for a filler and asset type.
     */
    @Transactional(readOnly = true)
    public LossRecord getLossRecord(Long fillerId, AssetType assetType) {
        return lossRecordRepository.findByFillerIdAndAssetType(fillerId, assetType)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Loss record not found for filler: " + fillerId + ", asset: " + assetType));
    }

    /**
     * Get all loss records for a filler.
     */
    @Transactional(readOnly = true)
    public List<LossRecord> getLossRecordsByFiller(Long fillerId) {
        return lossRecordRepository.findByFillerId(fillerId);
    }

    /**
     * Get all loss records for an asset type.
     */
    @Transactional(readOnly = true)
    public List<LossRecord> getLossRecordsByAssetType(AssetType assetType) {
        return lossRecordRepository.findByAssetType(assetType);
    }
}
