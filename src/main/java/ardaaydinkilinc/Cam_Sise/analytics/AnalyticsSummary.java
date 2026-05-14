package ardaaydinkilinc.Cam_Sise.analytics;

import java.util.Map;

public record AnalyticsSummary(
        int totalRequests,
        Map<String, Long> requestsByStatus,
        long palletRequests,
        long separatorRequests,

        int totalPlans,
        Map<String, Long> plansByStatus,
        double avgDistanceKm,
        double avgDurationMinutes,
        double medianDistanceKm,
        double p95DistanceKm,
        double stdDevDistanceKm,

        long totalFillers,
        long totalPalletStock,
        long totalSeparatorStock,
        long fillersWithLowPalletStock,
        long fillersWithLowSeparatorStock,
        double medianFillerStockPallet,
        double p95FillerStockPallet,
        double stdDevFillerStockPallet,

        long anomalyCount24h,
        long criticalAnomalyCount24h,
        long unreadNotificationCount
) {}
