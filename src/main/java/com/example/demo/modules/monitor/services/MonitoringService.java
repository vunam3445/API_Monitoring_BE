package com.example.demo.modules.monitor.services;

import com.example.demo.common.cache.ICacheService;
import com.example.demo.modules.monitor.dto.MonitoringSummaryResponse;
import com.example.demo.modules.monitor.enums.MonitorStatus;
import com.example.demo.modules.monitor.repositories.MonitorRepository;
import com.example.demo.modules.uptimeLogs.repositories.UptimeLogsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.example.demo.modules.monitor.dto.*;
import com.example.demo.modules.monitor.enums.MonitorEventType;
import com.example.demo.modules.monitor.entities.Monitor;
import com.example.demo.modules.uptimeLogs.entities.UptimeLogs;
import com.example.demo.modules.uptimeLogs.repositories.UptimeLogsSpecification;
import com.example.demo.common.base.RestPageImpl;
import com.example.demo.modules.monitor.mappers.MonitorMapper;
import com.example.demo.common.exceptions.ResourceNotFoundException;
import com.example.demo.common.exceptions.ForbidenException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MonitoringService implements IMonitoringService {

    private final MonitorRepository monitorRepository;
    private final UptimeLogsRepository uptimeLogsRepository;
    private final ICacheService cacheService;
    private final MonitorMapper monitorMapper;

    private static final String CACHE_PREFIX = "monitoring:summary:";
    private static final long CACHE_TTL = 60; // 60 seconds

    @Override
    public MonitorOverviewResponse getMonitorOverview(UUID userId, UUID monitorId) {
        String cacheKey = "monitoring:overview:" + monitorId;
        
        Object cached = cacheService.get(cacheKey);
        if (cached instanceof MonitorOverviewResponse) return (MonitorOverviewResponse) cached;

        Monitor monitor = monitorRepository.findById(monitorId)
                .orElseThrow(() -> new ResourceNotFoundException("Monitor not found: " + monitorId));
                
        if (!monitor.getUserId().equals(userId)) {
            throw new ForbidenException("Bạn không có quyền truy cập dữ liệu này.");
        }

        // Fetch recent logs (10)
        List<MonitoringEventResponse> recentLogs = getRecentEventsForMonitor(monitor, 10);
        
        // Fetch chart data (24h)
        List<MonitoringChartPoint> chartData = getCharts(userId, monitorId, "24h");
        
        // Calculate uptime percentage (30d)
        LocalDateTime since30d = LocalDateTime.now().minusDays(30);
        long total30 = uptimeLogsRepository.countByMonitorIdAndSince(monitorId, since30d);
        long up30 = uptimeLogsRepository.countByMonitorIdAndIsUpAndSince(monitorId, true, since30d);
        double uptimePercent = (total30 > 0) ? (double) up30 * 100 / total30 : 100.0;

        MonitorOverviewResponse overview = MonitorOverviewResponse.builder()
                .baseInfo(monitorMapper.toResponse(monitor))
                .currentStatus(monitor.getLastStatus())
                .lastCheckTime(monitor.getLastCheckAt())
                .latestLatency(monitor.getLastLatencyMs())
                .uptimePercent(Math.round(uptimePercent * 100.0) / 100.0)
                .recentLogs(recentLogs)
                .chartData(chartData)
                .build();

        cacheService.set(cacheKey, overview, CACHE_TTL);
        return overview;
    }

    private List<MonitoringEventResponse> getRecentEventsForMonitor(Monitor monitor, int limit) {
        Page<UptimeLogs> logs = uptimeLogsRepository.findByMonitorIdOrderByCheckedAtDesc(monitor.getId(), PageRequest.of(0, limit));
        return logs.getContent().stream().map(l -> {
            MonitorStatus status = l.getIsUp() ? 
                ("WARNING".equals(l.getAssertionStatus()) ? MonitorStatus.WARNING : MonitorStatus.HEALTHY) : 
                MonitorStatus.DOWN;
            return MonitoringEventResponse.builder()
                    .time(l.getCheckedAt())
                    .apiName(monitor.getName())
                    .eventType(l.getEventType())
                    .responseTime(l.getResponseTimeMs())
                    .status(status)
                    .message(l.getAssertionMessage())
                    .build();
        }).collect(Collectors.toList());
    }

    @Override
    public MonitorTrendResponse getMonitorTrend(UUID userId, UUID monitorId, String range) {
        // Trend is basically the latency points from getCharts
        List<MonitoringChartPoint> charts = getCharts(userId, monitorId, range);
        List<Integer> points = charts.stream()
                .map(p -> p.getAvgResponseTimeMs().intValue())
                .collect(Collectors.toList());
                
        return MonitorTrendResponse.builder()
                .monitorId(monitorId)
                .range(range)
                .points(points)
                .build();
    }

    @Override
    public List<MonitoringEventResponse> getRecentEvents(UUID userId, int limit) {
        String cacheKey = "monitoring:recent-events:" + userId + ":" + limit;
        
        Object cached = cacheService.get(cacheKey);
        if (cached instanceof List) return (List<MonitoringEventResponse>) cached;

        Page<UptimeLogs> logs = uptimeLogsRepository.findByMonitorUserIdOrderByCheckedAtDesc(userId, PageRequest.of(0, limit));
        
        List<MonitoringEventResponse> events = logs.getContent().stream().map(l -> {
            MonitorStatus status = l.getIsUp() ? 
                    ("WARNING".equals(l.getAssertionStatus()) ? MonitorStatus.WARNING : MonitorStatus.HEALTHY) : 
                    MonitorStatus.DOWN;
                    
            return MonitoringEventResponse.builder()
                    .time(l.getCheckedAt())
                    .apiName(l.getMonitor().getName())
                    .eventType(l.getEventType())
                    .responseTime(l.getResponseTimeMs())
                    .status(status)
                    .message(l.getAssertionMessage())
                    .build();
        }).collect(Collectors.toList());

        cacheService.set(cacheKey, events, CACHE_TTL);
        return events;
    }

    @Override
    public Page<MonitorLogRow> getMonitorLogs(UUID userId, UUID monitorId, MonitorStatus status, MonitorEventType eventType, Pageable pageable) {
        Specification<UptimeLogs> spec = Specification.where(UptimeLogsSpecification.hasUserId(userId))
                .and(UptimeLogsSpecification.hasMonitorId(monitorId))
                .and(UptimeLogsSpecification.hasStatus(status))
                .and(UptimeLogsSpecification.hasEventType(eventType));

        Page<UptimeLogs> entityPage = uptimeLogsRepository.findAll(spec, pageable);
        
        List<MonitorLogRow> dtos = entityPage.getContent().stream().map(l -> {
            MonitorStatus logStatus = l.getIsUp() ? 
                    ("WARNING".equals(l.getAssertionStatus()) ? MonitorStatus.WARNING : MonitorStatus.HEALTHY) : 
                    MonitorStatus.DOWN;

            return MonitorLogRow.builder()
                    .logId(l.getId())
                    .monitorId(l.getMonitor().getId())
                    .monitorName(l.getMonitor().getName())
                    .checkedAt(l.getCheckedAt())
                    .responseTimeMs(l.getResponseTimeMs())
                    .statusCode(l.getStatusCode())
                    .status(logStatus)
                    .eventType(l.getEventType())
                    .message(l.getAssertionMessage())
                    .errorMessage(l.getErrorMessage())
                    .build();
        }).collect(Collectors.toList());

        return new RestPageImpl<>(dtos, entityPage.getNumber(), entityPage.getSize(), entityPage.getTotalElements(), null, false, 0, null, false, 0);
    }

    @Override
    public List<KeyHealthResponse> getKeyHealth(UUID userId) {
        String cacheKey = "monitoring:key-health:" + userId;
        
        // 1. Check cache
        Object cached = cacheService.get(cacheKey);
        if (cached instanceof List) {
            return (List<KeyHealthResponse>) cached;
        }

        // 2. Fetch monitors
        List<Monitor> monitors = monitorRepository.findAllByUserId(userId);
        if (monitors.isEmpty()) return Collections.emptyList();

        // 3. Tính uptime cho từng monitor (24h)
        LocalDateTime since = LocalDateTime.now().minusHours(24);
        
        // Mocking uptime and trend data for now to focus on structure
        // In real production, we'd use aggregation query
        List<KeyHealthResponse> responses = monitors.stream().map(m -> {
            
            // Lấy 20 điểm latency gần nhất cho mini trend
            List<Integer> trend = uptimeLogsRepository.findRecentLatency(m.getId(), 20);
            
            // Tính uptime
            long total = uptimeLogsRepository.countByMonitorIdAndSince(m.getId(), since);
            long up = uptimeLogsRepository.countByMonitorIdAndIsUpAndSince(m.getId(), true, since);
            double uptimePercent = (total > 0) ? (double) up * 100 / total : 100.0;

            return KeyHealthResponse.builder()
                    .monitorId(m.getId())
                    .monitorName(m.getName())
                    .endpoint(m.getUrl())
                    .currentStatus(m.getLastStatus())
                    .latencyMs(m.getLastLatencyMs())
                    .uptimePercent(Math.round(uptimePercent * 10.0) / 10.0)
                    .miniTrendData(trend)
                    .build();
        }).collect(Collectors.toList());

        // 4. Cache
        cacheService.set(cacheKey, responses, CACHE_TTL);

        return responses;
    }

    @Override
    public MonitoringSummaryResponse getSummary(UUID userId) {
        String cacheKey = CACHE_PREFIX + userId.toString();
        
        // 1. Check cache
        Object cached = cacheService.get(cacheKey);
        if (cached instanceof MonitoringSummaryResponse) {
            log.debug("Cache hit for monitoring summary: {}", userId);
            return (MonitoringSummaryResponse) cached;
        }

        log.debug("Cache miss for monitoring summary: {}", userId);

        // 2. Query DB
        long totalMonitors = monitorRepository.countByUserId(userId);
        long healthyCount = monitorRepository.countByUserIdAndLastStatus(userId, MonitorStatus.HEALTHY);
        long warningCount = monitorRepository.countByUserIdAndLastStatus(userId, MonitorStatus.WARNING);
        long downCount = monitorRepository.countByUserIdAndLastStatus(userId, MonitorStatus.DOWN);
        long pausedCount = monitorRepository.countByUserIdAndIsActive(userId, false);
        
        long upCount = healthyCount + warningCount;

        // Tính uptime tổng quan trong 24h qua
        LocalDateTime since = LocalDateTime.now().minusHours(24);
        long totalLogs = uptimeLogsRepository.countTotalByUser(userId, since);
        long upLogs = uptimeLogsRepository.countUpByUser(userId, since);
        
        double uptimePercentOverall = (totalLogs > 0) ? (double) upLogs * 100 / totalLogs : 100.0;

        MonitoringSummaryResponse summary = MonitoringSummaryResponse.builder()
                .totalMonitors(totalMonitors)
                .healthyCount(healthyCount)
                .warningCount(warningCount)
                .downCount(downCount)
                .pausedCount(pausedCount)
                .upCount(upCount)
                .uptimePercentOverall(Math.round(uptimePercentOverall * 100.0) / 100.0)
                .build();

        // 3. Cache result
        cacheService.set(cacheKey, summary, CACHE_TTL);
 
        return summary;
    }
 
    @Override
    public List<MonitoringChartPoint> getCharts(UUID userId, UUID monitorId, String range) {
        String cacheKey = "monitoring:chart:" + userId + ":" + (monitorId != null ? monitorId : "all") + ":" + range;
        
        // 1. Check cache
        Object cached = cacheService.get(cacheKey);
        if (cached instanceof List) {
            return (List<MonitoringChartPoint>) cached;
        }

        // 2. Determine timeframe and bucket
        LocalDateTime since;
        String bucket;
        switch (range.toLowerCase()) {
            case "1h" -> {
                since = LocalDateTime.now().minusHours(1);
                bucket = "minute";
            }
            case "24h" -> {
                since = LocalDateTime.now().minusHours(24);
                bucket = "hour";
            }
            case "7d" -> {
                since = LocalDateTime.now().minusDays(7);
                bucket = "hour";
            }
            case "30d" -> {
                since = LocalDateTime.now().minusDays(30);
                bucket = "day";
            }
            default -> {
                since = LocalDateTime.now().minusHours(24);
                bucket = "hour";
            }
        }

        // 3. Query DB
        List<com.example.demo.modules.monitor.dto.MonitoringChartProjection> projections = 
            uptimeLogsRepository.getAggregatedUptimeLogs(userId, monitorId, since, bucket);

        // 4. Map to DTO
        List<MonitoringChartPoint> points = projections.stream().map(p -> {
            double errorRate = (p.getTotalChecks() != null && p.getTotalChecks() > 0) 
                    ? (double) p.getFailedChecks() * 100 / p.getTotalChecks() 
                    : 0.0;
            
            return MonitoringChartPoint.builder()
                    .time(p.getTime())
                    .avgResponseTimeMs(p.getAvgResponseTimeMs() != null ? Math.round(p.getAvgResponseTimeMs() * 10.0) / 10.0 : 0.0)
                    .totalChecks(p.getTotalChecks())
                    .failedChecks(p.getFailedChecks())
                    .errorRatePercent(Math.round(errorRate * 100.0) / 100.0)
                    .build();
        }).toList();

        // 5. Cache result
        cacheService.set(cacheKey, points, CACHE_TTL);
 
        return points;
    }
 
    @Override
    public MonitorUptimeResponse getMonitorUptimeStats(UUID userId, UUID monitorId, String range) {
        LocalDateTime since;
        int bucketSeconds;

        switch (range.toUpperCase()) {
            case "1H" -> {
                since = LocalDateTime.now().minusHours(1);
                bucketSeconds = 60; // 1 min (60 points)
            }
            case "6H" -> {
                since = LocalDateTime.now().minusHours(6);
                bucketSeconds = 300; // 5 min (72 points)
            }
            case "24H" -> {
                since = LocalDateTime.now().minusHours(24);
                bucketSeconds = 900; // 15 min (96 points)
            }
            case "7D" -> {
                since = LocalDateTime.now().minusDays(7);
                bucketSeconds = 3600; // 1 hour (168 points)
            }
            default -> {
                since = LocalDateTime.now().minusHours(24);
                bucketSeconds = 900;
            }
        }

        // Fetch aggregated status buckets
        List<com.example.demo.modules.monitor.dto.UptimeStatusProjection> buckets = 
            uptimeLogsRepository.getUptimeStatusBuckets(monitorId, since, bucketSeconds);
        
        List<MonitorUptimeResponse.UptimeStatusPoint> history = buckets.stream()
                .map(b -> MonitorUptimeResponse.UptimeStatusPoint.builder()
                        .time(b.getTime())
                        .isUp(b.getIsUp())
                        .latencyMs(b.getLatencyMs())
                        .build())
                .collect(Collectors.toList());
        
        // Calculate total uptime% (based on selected range)
        long total = uptimeLogsRepository.countByMonitorIdAndSince(monitorId, since);
        long up = uptimeLogsRepository.countByMonitorIdAndIsUpAndSince(monitorId, true, since);
        double uptimePercent = (total > 0) ? (double) up * 100 / total : 100.0;

        return MonitorUptimeResponse.builder()
                .monitorId(monitorId)
                .range(range)
                .uptimePercent(Math.round(uptimePercent * 10.0) / 10.0)
                .statusHistory(history)
                .build();
    }
 
    @Override
    public MonitoringSearchResponse search(UUID userId, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return MonitoringSearchResponse.builder()
                    .monitors(Collections.emptyList())
                    .recentLogs(Collections.emptyList())
                    .build();
        }

        // 1. Search monitors (name, URL)
        Specification<Monitor> monitorSpec = Specification.where((root, query, cb) -> cb.equal(root.get("userId"), userId));
        monitorSpec = monitorSpec.and((root, query, cb) -> cb.or(
            cb.like(cb.lower(root.get("name")), "%" + keyword.toLowerCase() + "%"),
            cb.like(cb.lower(root.get("url")), "%" + keyword.toLowerCase() + "%")
        ));
        
        List<Monitor> monitors = monitorRepository.findAll(monitorSpec, PageRequest.of(0, 10)).getContent();

        // 2. Search logs (assertion message, error message, monitor name)
        Specification<UptimeLogs> logSpec = Specification.where(UptimeLogsSpecification.hasUserId(userId))
            .and((root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("assertionMessage")), "%" + keyword.toLowerCase() + "%"),
                cb.like(cb.lower(root.get("errorMessage")), "%" + keyword.toLowerCase() + "%"),
                cb.like(cb.lower(root.join("monitor").get("name")), "%" + keyword.toLowerCase() + "%")
            ));
            
        List<UptimeLogs> logs = uptimeLogsRepository.findAll(logSpec, PageRequest.of(0, 10, Sort.by("checkedAt").descending())).getContent();

        return MonitoringSearchResponse.builder()
                .monitors(monitors.stream().map(monitorMapper::toResponse).collect(Collectors.toList()))
                .recentLogs(logs.stream().map(l -> {
                    MonitorStatus logStatus = l.getIsUp() ? 
                            ("WARNING".equals(l.getAssertionStatus()) ? MonitorStatus.WARNING : MonitorStatus.HEALTHY) : 
                            MonitorStatus.DOWN;

                    return MonitorLogRow.builder()
                            .logId(l.getId())
                            .monitorId(l.getMonitor().getId())
                            .monitorName(l.getMonitor().getName())
                            .checkedAt(l.getCheckedAt())
                            .responseTimeMs(l.getResponseTimeMs())
                            .statusCode(l.getStatusCode())
                            .status(logStatus)
                            .eventType(l.getEventType())
                            .message(l.getAssertionMessage())
                            .errorMessage(l.getErrorMessage())
                            .build();
                }).collect(Collectors.toList()))
                .build();
    }
}

