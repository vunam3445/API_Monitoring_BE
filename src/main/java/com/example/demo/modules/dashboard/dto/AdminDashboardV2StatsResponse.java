package com.example.demo.modules.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardV2StatsResponse {
    private StatItem totalApis;
    private StatItem warningApis;
    private StatItem downApis;
    private StatItem avgLatency;
    private StatItem checksPerMin;

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
