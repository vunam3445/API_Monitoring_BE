package com.example.demo.modules.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryResponse {
    private long totalMonitors;
    private long currentlyDown;
    private double uptime24h;
    private double avgLatencyMs;
    private DashboardTrendTrends trends;
    private String comparisonWindow;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DashboardTrendTrends {
        private long totalMonitorsDelta;
        private long currentlyDownDelta;
        private double uptime24hDelta;
        private double avgLatencyDeltaMs;
    }
}
