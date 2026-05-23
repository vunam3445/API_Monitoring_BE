package com.example.demo.modules.revenue.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SubscriptionAnalyticsDTO {
    private UsersComparisonDTO usersComparison;
    private UpgradeTrendsDTO upgradeTrends;
    private ChurnMetricsDTO churnMetrics;

    @Data
    @Builder
    public static class UsersComparisonDTO {
        private Long free;
        private Long paid;
    }

    @Data
    @Builder
    public static class UpgradeTrendsDTO {
        private Long count;
        private Double growth;
    }

    @Data
    @Builder
    public static class ChurnMetricsDTO {
        private Double rate;
        private String status;
    }
}
