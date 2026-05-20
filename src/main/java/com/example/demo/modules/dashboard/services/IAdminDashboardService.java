package com.example.demo.modules.dashboard.services;

import com.example.demo.modules.dashboard.dto.*;

import java.util.List;

public interface IAdminDashboardService {
    AdminDashboardStatsResponse getGlobalStats();

    AdminDashboardV2StatsResponse getV2Stats(String range);

    AdminPerformanceResponse getPerformance(String range);

    AdminInfrastructureResponse getInfrastructure();

    List<AdminActivityResponse> getLatestActivity();

    ResponseTimeChartResponse getGlobalResponseTimeTrend(String range);

    UptimeGaugeResponse getGlobalUptime(String range);

    MethodDistributionResponse getGlobalMethodDistribution(String range);
}
