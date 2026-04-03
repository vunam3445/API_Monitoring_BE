package com.example.demo.modules.monitor.services;

import com.example.demo.modules.monitor.dto.*;
import com.example.demo.modules.monitor.enums.MonitorEventType;
import com.example.demo.modules.monitor.enums.MonitorStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.UUID;

public interface IMonitoringService {
    MonitoringSummaryResponse getSummary(UUID userId);
    List<MonitoringChartPoint> getCharts(UUID userId, UUID monitorId, String range);
    List<KeyHealthResponse> getKeyHealth(UUID userId);
 
    List<MonitoringEventResponse> getRecentEvents(UUID userId, int limit);
 
    Page<MonitorLogRow> getMonitorLogs(UUID userId, UUID monitorId, MonitorStatus status, MonitorEventType eventType, Pageable pageable);
 
    MonitorOverviewResponse getMonitorOverview(UUID userId, UUID monitorId);
 
    MonitorTrendResponse getMonitorTrend(UUID userId, UUID monitorId, String range);
 
    MonitorUptimeResponse getMonitorUptimeStats(UUID userId, UUID monitorId, String range);
 
    MonitoringSearchResponse search(UUID userId, String keyword);
}
