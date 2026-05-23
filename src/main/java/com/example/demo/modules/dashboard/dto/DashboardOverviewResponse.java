package com.example.demo.modules.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardOverviewResponse {
    private DashboardSummaryResponse summary;
    private ResponseTimeChartResponse responseTimeChart;
    private ErrorRateResponse errorRate;
    private UptimeGaugeResponse uptimeGauge;
    private UnstableMonitorsResponse unstableMonitors;
    private PlanUsageResponse planUsage;
    private DashboardSuggestionResponse suggestion;
}
