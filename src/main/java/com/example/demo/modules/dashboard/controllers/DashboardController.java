package com.example.demo.modules.dashboard.controllers;

import com.example.demo.common.security.ISecurityContextService;
import com.example.demo.modules.dashboard.dto.*;
import com.example.demo.modules.dashboard.services.IDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final IDashboardService dashboardService;
    private final ISecurityContextService securityContextService;

    private UUID getUserId() {
        return securityContextService.getCurrentUserId()
                .orElseThrow(() -> new RuntimeException("Bạn cần đăng nhập để thực hiện thao tác này."));
    }

    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryResponse> getSummary() {
        return ResponseEntity.ok(dashboardService.getSummary(getUserId()));
    }

    @GetMapping("/charts/response-time")
    public ResponseEntity<ResponseTimeChartResponse> getResponseTimeChart(
            @RequestParam(defaultValue = "24h") String range,
            @RequestParam(defaultValue = "hour") String bucket) {
        return ResponseEntity.ok(dashboardService.getResponseTimeChart(getUserId(), range, bucket));
    }

    @GetMapping("/error-rate")
    public ResponseEntity<ErrorRateResponse> getErrorRate(
            @RequestParam(defaultValue = "24h") String range,
            @RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(dashboardService.getErrorRate(getUserId(), range, limit));
    }

    @GetMapping("/uptime-gauge")
    public ResponseEntity<UptimeGaugeResponse> getUptimeGauge(
            @RequestParam(defaultValue = "24h") String range) {
        return ResponseEntity.ok(dashboardService.getUptimeGauge(getUserId(), range));
    }

    @GetMapping("/unstable-monitors")
    public ResponseEntity<UnstableMonitorsResponse> getUnstableMonitors(
            @RequestParam(defaultValue = "7d") String range,
            @RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(dashboardService.getUnstableMonitors(getUserId(), range, limit));
    }

    @GetMapping("/plan-usage")
    public ResponseEntity<PlanUsageResponse> getPlanUsage() {
        return ResponseEntity.ok(dashboardService.getPlanUsage(getUserId()));
    }

    @GetMapping("/suggestions")
    public ResponseEntity<DashboardSuggestionResponse> getSuggestions() {
        return ResponseEntity.ok(dashboardService.getSuggestions(getUserId()));
    }

    @GetMapping("/overview")
    public ResponseEntity<DashboardOverviewResponse> getOverview(
            @RequestParam(defaultValue = "24h") String range) {
        return ResponseEntity.ok(dashboardService.getOverview(getUserId(), range));
    }
}
