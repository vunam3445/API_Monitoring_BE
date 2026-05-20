package com.example.demo.modules.revenue.services;

import com.example.demo.modules.revenue.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface IRevenueService {
    RevenueStatsDTO getRevenueStats();
    RevenueChartDTO getRevenueCharts(String period);
    SubscriptionAnalyticsDTO getSubscriptionAnalytics();
    List<PlanBreakdownDTO> getPlanBreakdown();
    Page<RecentTransactionDTO> getRecentTransactions(Pageable pageable);
}
