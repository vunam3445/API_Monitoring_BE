package com.example.demo.modules.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminCardStatsResponse {
    private StatItem revenue;
    private StatItem totalUsers;
    private StatItem apisMonitored;
    private StatItem apisDown;
    private StatItem alertsToday;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatItem {
        private String value;
        private String subValue;
        private String trend;
        private boolean trendUp;
    }
}
