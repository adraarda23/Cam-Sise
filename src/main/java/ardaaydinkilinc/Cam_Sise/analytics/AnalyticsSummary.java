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

        long totalFillers,
        long totalPalletStock,
        long totalSeparatorStock,
        long fillersWithLowPalletStock,
        long fillersWithLowSeparatorStock
) {}
