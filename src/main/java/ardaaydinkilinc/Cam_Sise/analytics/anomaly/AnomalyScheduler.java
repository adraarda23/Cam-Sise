package ardaaydinkilinc.Cam_Sise.analytics.anomaly;

import ardaaydinkilinc.Cam_Sise.inventory.domain.FillerStock;
import ardaaydinkilinc.Cam_Sise.inventory.repository.FillerStockRepository;
import ardaaydinkilinc.Cam_Sise.shared.domain.event.DomainEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Proactive daily anomaly scan.
 *
 * <p>At {@code app.anomaly.cron} (default 06:00 every morning) walks every
 * filler stock record and asks {@link AnomalyDetectionService} whether
 * yesterday's net inflow falls outside the seasonal baseline. Anomalies are
 * published as {@link StockAnomalyDetected} domain events.
 *
 * <p>Disabled by default — turn on with {@code app.anomaly.enabled=true} so
 * unit tests and dev profiles don't accidentally fire long-running scans.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.anomaly.enabled", havingValue = "true", matchIfMissing = false)
public class AnomalyScheduler {

    private final AnomalyDetectionService anomalyDetectionService;
    private final FillerStockRepository fillerStockRepository;
    private final DomainEventPublisher eventPublisher;

    @Value("${app.anomaly.max-fillers-per-run:500}")
    private int maxFillersPerRun;

    @Scheduled(cron = "${app.anomaly.cron:0 0 6 * * *}")
    public void runDailyScan() {
        log.info("🔎 Starting daily anomaly scan");

        List<FillerStock> stocks = fillerStockRepository.findAll();
        int scanned = 0;
        int alarmed = 0;
        for (FillerStock stock : stocks) {
            if (scanned >= maxFillersPerRun) {
                log.warn("Reached max-fillers-per-run cap ({}), aborting remainder", maxFillersPerRun);
                break;
            }
            try {
                AnomalyResult result = anomalyDetectionService.checkLatestDay(
                        stock.getFillerId(), stock.getAssetType());
                if (result.isAnomaly()) {
                    eventPublisher.publish(StockAnomalyDetected.from(result));
                    alarmed++;
                    log.warn("⚠️ Anomaly: fillerId={}, asset={}, severity={}, z={}",
                            result.fillerId(), result.assetType(), result.severity(), result.zScore());
                }
            } catch (Exception e) {
                log.error("Anomaly scan failed for stock {}: {}", stock.getId(), e.getMessage(), e);
            }
            scanned++;
        }
        log.info("✅ Daily anomaly scan complete: {} scanned, {} alarms raised", scanned, alarmed);
    }
}
