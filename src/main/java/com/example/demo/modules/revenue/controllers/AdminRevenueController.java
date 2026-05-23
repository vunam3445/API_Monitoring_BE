package com.example.demo.modules.revenue.controllers;

import com.example.demo.modules.revenue.dto.*;
import com.example.demo.modules.revenue.services.IRevenueService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/revenue")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminRevenueController {

    private final IRevenueService revenueService;

    @GetMapping("/stats")
    public ResponseEntity<RevenueStatsDTO> getStats() {
        return ResponseEntity.ok(revenueService.getRevenueStats());
    }

    @GetMapping("/charts")
    public ResponseEntity<RevenueChartDTO> getCharts(@RequestParam(defaultValue = "last_30_days") String period) {
        return ResponseEntity.ok(revenueService.getRevenueCharts(period));
    }

    @GetMapping("/subscription-analytics")
    public ResponseEntity<SubscriptionAnalyticsDTO> getSubscriptionAnalytics() {
        return ResponseEntity.ok(revenueService.getSubscriptionAnalytics());
    }

    @GetMapping("/plan-breakdown")
    public ResponseEntity<List<PlanBreakdownDTO>> getPlanBreakdown() {
        return ResponseEntity.ok(revenueService.getPlanBreakdown());
    }

    @GetMapping("/recent-transactions")
    public ResponseEntity<Page<RecentTransactionDTO>> getRecentTransactions(
            @PageableDefault(size = 10, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(revenueService.getRecentTransactions(pageable));
    }
}
