package com.example.demo.modules.monitor.controllers;

import com.example.demo.common.security.ISecurityContextService;
import com.example.demo.modules.monitor.dto.*;
import com.example.demo.modules.monitor.enums.MonitorEventType;
import com.example.demo.modules.monitor.enums.MonitorStatus;
import com.example.demo.modules.monitor.services.IMonitoringService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/monitoring")
@RequiredArgsConstructor
public class MonitoringController {

    private final IMonitoringService monitoringService;
    private final ISecurityContextService securityContextService;

    @GetMapping("/summary")
    public ResponseEntity<MonitoringSummaryResponse> getSummary() {
        UUID userId = securityContextService.getCurrentUserId()
                .orElseThrow(() -> new RuntimeException("Bạn cần đăng nhập để thực hiện thao tác này."));
        
        MonitoringSummaryResponse summary = monitoringService.getSummary(userId);
        return ResponseEntity.ok(summary);
    }
 
    @GetMapping("/charts/response-time")
    public ResponseEntity<List<MonitoringChartPoint>> getResponseTimeChart(
            @RequestParam(defaultValue = "24h") String range,
            @RequestParam(required = false) UUID monitorId) {
        UUID userId = securityContextService.getCurrentUserId()
                .orElseThrow(() -> new RuntimeException("Bạn cần đăng nhập để thực hiện thao tác này."));
        
        List<MonitoringChartPoint> chartData = monitoringService.getCharts(userId, monitorId, range);
        return ResponseEntity.ok(chartData);
    }
 
    @GetMapping("/charts/error-rate")
    public ResponseEntity<List<MonitoringChartPoint>> getErrorRateChart(
            @RequestParam(defaultValue = "24h") String range,
            @RequestParam(required = false) UUID monitorId) {
        UUID userId = securityContextService.getCurrentUserId()
                .orElseThrow(() -> new RuntimeException("Bạn cần đăng nhập để thực hiện thao tác này."));
        
        List<MonitoringChartPoint> chartData = monitoringService.getCharts(userId, monitorId, range);
        return ResponseEntity.ok(chartData);
    }
 
    @GetMapping("/key-health")
    public ResponseEntity<List<KeyHealthResponse>> getKeyHealth() {
        UUID userId = securityContextService.getCurrentUserId()
                .orElseThrow(() -> new RuntimeException("Bạn cần đăng nhập để thực hiện thao tác này."));
        
        List<KeyHealthResponse> healthData = monitoringService.getKeyHealth(userId);
        return ResponseEntity.ok(healthData);
    }
 
    @GetMapping("/events")
    public ResponseEntity<List<MonitoringEventResponse>> getRecentEvents(@RequestParam(defaultValue = "10") int limit) {
        UUID userId = securityContextService.getCurrentUserId()
                .orElseThrow(() -> new RuntimeException("Bạn cần đăng nhập để thực hiện thao tác này."));
        
        return ResponseEntity.ok(monitoringService.getRecentEvents(userId, limit));
    }
 
    @GetMapping("/logs")
    public ResponseEntity<Page<MonitorLogRow>> getMonitorLogs(
            @RequestParam(required = false) UUID monitorId,
            @RequestParam(required = false) MonitorStatus status,
            @RequestParam(required = false) MonitorEventType eventType,
            Pageable pageable) {
        UUID userId = securityContextService.getCurrentUserId()
                .orElseThrow(() -> new RuntimeException("Bạn cần đăng nhập để thực hiện thao tác này."));
        
        return ResponseEntity.ok(monitoringService.getMonitorLogs(userId, monitorId, status, eventType, pageable));
    }
 
    @GetMapping("/{id}/overview")
    public ResponseEntity<MonitorOverviewResponse> getMonitorOverview(@PathVariable UUID id) {
        UUID userId = securityContextService.getCurrentUserId()
                .orElseThrow(() -> new RuntimeException("Bạn cần đăng nhập để thực hiện thao tác này."));
        
        return ResponseEntity.ok(monitoringService.getMonitorOverview(userId, id));
    }
 
    @GetMapping("/{id}/trend")
    public ResponseEntity<MonitorTrendResponse> getMonitorTrend(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "1h") String range) {
        UUID userId = securityContextService.getCurrentUserId()
                .orElseThrow(() -> new RuntimeException("Bạn cần đăng nhập để thực hiện thao tác này."));
        
        return ResponseEntity.ok(monitoringService.getMonitorTrend(userId, id, range));
    }
 
    @GetMapping("/{id}/uptime")
    public ResponseEntity<MonitorUptimeResponse> getMonitorUptime(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "24h") String range) {
        UUID userId = securityContextService.getCurrentUserId()
                .orElseThrow(() -> new RuntimeException("Bạn cần đăng nhập để thực hiện thao tác này."));
        
        return ResponseEntity.ok(monitoringService.getMonitorUptimeStats(userId, id, range));
    }
 
    @GetMapping("/search")
    public ResponseEntity<MonitoringSearchResponse> search(@RequestParam String keyword) {
        UUID userId = securityContextService.getCurrentUserId()
                .orElseThrow(() -> new RuntimeException("Bạn cần đăng nhập để thực hiện thao tác này."));
        
        return ResponseEntity.ok(monitoringService.search(userId, keyword));
    }
}
