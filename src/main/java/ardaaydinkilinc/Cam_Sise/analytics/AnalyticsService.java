package ardaaydinkilinc.Cam_Sise.analytics;

import ardaaydinkilinc.Cam_Sise.core.repository.FillerRepository;
import ardaaydinkilinc.Cam_Sise.inventory.domain.vo.AssetType;
import ardaaydinkilinc.Cam_Sise.inventory.repository.FillerStockRepository;
import ardaaydinkilinc.Cam_Sise.logistics.repository.CollectionPlanRepository;
import ardaaydinkilinc.Cam_Sise.logistics.repository.CollectionRequestRepository;
import ardaaydinkilinc.Cam_Sise.notification.domain.Notification;
import ardaaydinkilinc.Cam_Sise.notification.domain.vo.NotificationSeverity;
import ardaaydinkilinc.Cam_Sise.notification.domain.vo.NotificationType;
import ardaaydinkilinc.Cam_Sise.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalyticsService {

    private final CollectionRequestRepository requestRepo;
    private final CollectionPlanRepository planRepo;
    private final FillerStockRepository stockRepo;
    private final FillerRepository fillerRepo;
    private final NotificationRepository notificationRepo;

    public AnalyticsSummary getSummary(Long poolOperatorId) {
        var requests = requestRepo.findByPoolOperatorId(poolOperatorId);
        var plans = planRepo.findByPoolOperatorId(poolOperatorId);
        var stocks = stockRepo.findByPoolOperatorId(poolOperatorId);

        var requestsByStatus = requests.stream()
                .collect(Collectors.groupingBy(r -> r.getStatus().name(), Collectors.counting()));

        long palletRequests = requests.stream()
                .filter(r -> r.getAssetType() == AssetType.PALLET).count();
        long separatorRequests = requests.stream()
                .filter(r -> r.getAssetType() == AssetType.SEPARATOR).count();

        var plansByStatus = plans.stream()
                .collect(Collectors.groupingBy(p -> p.getStatus().name(), Collectors.counting()));

        DescriptiveStatistics distanceStats = new DescriptiveStatistics();
        plans.stream()
                .filter(p -> p.getTotalDistance() != null)
                .mapToDouble(p -> p.getTotalDistance().kilometers())
                .forEach(distanceStats::addValue);

        double avgDistanceKm = distanceStats.getN() > 0 ? distanceStats.getMean() : 0;
        double medianDistanceKm = distanceStats.getN() > 0 ? distanceStats.getPercentile(50) : 0;
        double p95DistanceKm = distanceStats.getN() > 0 ? distanceStats.getPercentile(95) : 0;
        double stdDevDistanceKm = distanceStats.getN() > 1 ? distanceStats.getStandardDeviation() : 0;

        double avgDurationMinutes = plans.stream()
                .filter(p -> p.getEstimatedDuration() != null)
                .mapToDouble(p -> p.getEstimatedDuration().minutes())
                .average().orElse(0);

        long totalPalletStock = stocks.stream()
                .filter(s -> s.getAssetType() == AssetType.PALLET)
                .mapToLong(s -> s.getCurrentQuantity()).sum();

        long totalSeparatorStock = stocks.stream()
                .filter(s -> s.getAssetType() == AssetType.SEPARATOR)
                .mapToLong(s -> s.getCurrentQuantity()).sum();

        long fillersWithLowPalletStock = stocks.stream()
                .filter(s -> s.getAssetType() == AssetType.PALLET
                        && s.getCurrentQuantity() >= s.getThresholdQuantity())
                .count();

        long fillersWithLowSeparatorStock = stocks.stream()
                .filter(s -> s.getAssetType() == AssetType.SEPARATOR
                        && s.getCurrentQuantity() >= s.getThresholdQuantity())
                .count();

        DescriptiveStatistics palletStockStats = new DescriptiveStatistics();
        stocks.stream()
                .filter(s -> s.getAssetType() == AssetType.PALLET)
                .mapToInt(s -> s.getCurrentQuantity())
                .forEach(palletStockStats::addValue);

        double medianPalletStock = palletStockStats.getN() > 0 ? palletStockStats.getPercentile(50) : 0;
        double p95PalletStock = palletStockStats.getN() > 0 ? palletStockStats.getPercentile(95) : 0;
        double stdDevPalletStock = palletStockStats.getN() > 1 ? palletStockStats.getStandardDeviation() : 0;

        long totalFillers = fillerRepo.findByPoolOperatorId(poolOperatorId).size();

        LocalDateTime since24h = LocalDateTime.now().minusHours(24);
        List<Notification> recentAnomalies = notificationRepo.findByPoolOperatorIdAndReadOrderByCreatedAtDesc(
                        poolOperatorId, false).stream()
                .filter(n -> n.getType() == NotificationType.STOCK_ANOMALY)
                .filter(n -> n.getCreatedAt() != null && n.getCreatedAt().isAfter(since24h))
                .toList();
        long anomalyCount24h = recentAnomalies.size();
        long criticalAnomalyCount24h = recentAnomalies.stream()
                .filter(n -> n.getSeverity() == NotificationSeverity.CRITICAL)
                .count();

        long unreadNotificationCount = notificationRepo.findByPoolOperatorIdAndReadOrderByCreatedAtDesc(
                poolOperatorId, false).size();

        return new AnalyticsSummary(
                requests.size(),
                requestsByStatus,
                palletRequests,
                separatorRequests,
                plans.size(),
                plansByStatus,
                avgDistanceKm,
                avgDurationMinutes,
                medianDistanceKm,
                p95DistanceKm,
                stdDevDistanceKm,
                totalFillers,
                totalPalletStock,
                totalSeparatorStock,
                fillersWithLowPalletStock,
                fillersWithLowSeparatorStock,
                medianPalletStock,
                p95PalletStock,
                stdDevPalletStock,
                anomalyCount24h,
                criticalAnomalyCount24h,
                unreadNotificationCount
        );
    }
}
