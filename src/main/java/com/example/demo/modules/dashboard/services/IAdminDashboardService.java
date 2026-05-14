package com.example.demo.modules.dashboard.services;

import com.example.demo.modules.dashboard.dto.AdminDashboardStatsResponse;
import com.example.demo.modules.dashboard.dto.MethodDistributionResponse;
import com.example.demo.modules.dashboard.dto.ResponseTimeChartResponse;
import com.example.demo.modules.dashboard.dto.UptimeGaugeResponse;

public interface IAdminDashboardService {
    AdminDashboardStatsResponse getGlobalStats();
    ResponseTimeChartResponse getGlobalResponseTimeTrend(String range);
    UptimeGaugeResponse getGlobalUptime(String range);
    MethodDistributionResponse getGlobalMethodDistribution(String range);
}
