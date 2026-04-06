package com.example.demo.modules.dashboard.services;

import com.example.demo.modules.dashboard.dto.*;

import java.util.UUID;

public interface IDashboardService {
    DashboardSummaryResponse getSummary(UUID userId);
    ResponseTimeChartResponse getResponseTimeChart(UUID userId, String range, String bucket);
    ErrorRateResponse getErrorRate(UUID userId, String range, int limit);
    UptimeGaugeResponse getUptimeGauge(UUID userId, String range);
    UnstableMonitorsResponse getUnstableMonitors(UUID userId, String range, int limit);
    PlanUsageResponse getPlanUsage(UUID userId);
    DashboardSuggestionResponse getSuggestions(UUID userId);
    DashboardOverviewResponse getOverview(UUID userId, String range);
}
