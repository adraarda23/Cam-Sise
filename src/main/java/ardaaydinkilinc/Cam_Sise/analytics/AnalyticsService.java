package ardaaydinkilinc.Cam_Sise.analytics;

import ardaaydinkilinc.Cam_Sise.core.repository.FillerRepository;
import ardaaydinkilinc.Cam_Sise.inventory.domain.vo.AssetType;
import ardaaydinkilinc.Cam_Sise.inventory.repository.FillerStockRepository;
import ardaaydinkilinc.Cam_Sise.logistics.repository.CollectionPlanRepository;
import ardaaydinkilinc.Cam_Sise.logistics.repository.CollectionRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalyticsService {

    private final CollectionRequestRepository requestRepo;
    private final CollectionPlanRepository planRepo;
    private final FillerStockRepository stockRepo;
    private final FillerRepository fillerRepo;

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

        double avgDistanceKm = plans.stream()
                .filter(p -> p.getTotalDistance() != null)
                .mapToDouble(p -> p.getTotalDistance().kilometers())
                .average().orElse(0);

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

        long totalFillers = fillerRepo.findByPoolOperatorId(poolOperatorId).size();

        return new AnalyticsSummary(
                requests.size(),
                requestsByStatus,
                palletRequests,
                separatorRequests,
                plans.size(),
                plansByStatus,
                avgDistanceKm,
                avgDurationMinutes,
                totalFillers,
                totalPalletStock,
                totalSeparatorStock,
                fillersWithLowPalletStock,
                fillersWithLowSeparatorStock
        );
    }
}
