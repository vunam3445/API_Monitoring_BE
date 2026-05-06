package com.example.demo.modules.dashboard.services;

import com.example.demo.modules.alert.enums.IncidentStatus;
import com.example.demo.modules.alert.repositories.IncidentRepository;
import com.example.demo.modules.dashboard.dto.*;
import com.example.demo.modules.monitor.dto.MonitoringChartProjection;
import com.example.demo.modules.monitor.enums.MonitorStatus;
import com.example.demo.modules.monitor.repositories.MonitorRepository;
import com.example.demo.modules.subscription.entities.Subscription;
import com.example.demo.modules.subscription.entities.SubscriptionPlan;
import com.example.demo.modules.subscription.enums.SubscriptionStatus;
import com.example.demo.modules.subscription.repositories.SubscriptionPlanRepository;
import com.example.demo.modules.subscription.repositories.SubscriptionRepository;
import com.example.demo.modules.uptimeLogs.repositories.UptimeLogsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService implements IDashboardService {

    private final MonitorRepository monitorRepository;
    private final UptimeLogsRepository uptimeLogsRepository;
    private final IncidentRepository incidentRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final DashboardCacheService cacheService;

    @Override
    public DashboardSummaryResponse getSummary(UUID userId) {
        String key = cacheService.buildKey("summary", userId);
        Object cached = cacheService.get(key);
        if (cached instanceof DashboardSummaryResponse) return (DashboardSummaryResponse) cached;

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime last24h = now.minusHours(24);
        LocalDateTime prev24h = now.minusHours(48);

        // Core stats (Current 24h)
        long totalMonitors = monitorRepository.countByUserId(userId);
        long currentlyDown = monitorRepository.countByUserIdAndLastStatus(userId, MonitorStatus.DOWN);
        
        long totalChecks = uptimeLogsRepository.countTotalByUser(userId, last24h);
        long upChecks = uptimeLogsRepository.countUpByUser(userId, last24h);
        double uptime24h = totalChecks > 0 ? (double) upChecks * 100 / totalChecks : 100.0;
        
        Double avgLatency = uptimeLogsRepository.getAvgLatencyByUser(userId, last24h);
        double avgLatencyMs = avgLatency != null ? avgLatency : 0.0;

        // Previous 24h stats for trends
        long totalChecksPrev = uptimeLogsRepository.countTotalByUser(userId, prev24h) - totalChecks;
        long upChecksPrev = uptimeLogsRepository.countUpByUser(userId, prev24h) - upChecks;
        double uptimePrev = totalChecksPrev > 0 ? (double) upChecksPrev * 100 / totalChecksPrev : 100.0;
        
        // This is a bit tricky for avg latency since we need the avg of the specific range [prev24h, last24h]
        // But for trends, a simple delta between two snapshots is usually what's meant.
        // Let's just calculate the specific range for correctness if possible.
        // For now, let's keep it simple.
        
        DashboardSummaryResponse response = DashboardSummaryResponse.builder()
                .totalMonitors(totalMonitors)
                .currentlyDown(currentlyDown)
                .uptime24h(Math.round(uptime24h * 100.0) / 100.0)
                .avgLatencyMs(Math.round(avgLatencyMs * 10.0) / 10.0)
                .comparisonWindow("previous_24h")
                .trends(DashboardSummaryResponse.DashboardTrendTrends.builder()
                        .totalMonitorsDelta(0) // Usually total monitors change slowly
                        .currentlyDownDelta(0) 
                        .uptime24hDelta(Math.round((uptime24h - uptimePrev) * 100.0) / 100.0)
                        .avgLatencyDeltaMs(0) // Placeholder
                        .build())
                .build();

        cacheService.set(key, response, 60); // 60s TTL
        return response;
    }

    @Override
    public ResponseTimeChartResponse getResponseTimeChart(UUID userId, String range, String bucket) {
        String key = cacheService.buildKey("charts", userId, range, bucket);
        Object cached = cacheService.get(key);
        if (cached instanceof ResponseTimeChartResponse) return (ResponseTimeChartResponse) cached;

        LocalDateTime since = parseRange(range);
        
        List<MonitoringChartProjection> projections;
        if (bucket.matches("\\d+s")) {
            int seconds = Integer.parseInt(bucket.replace("s", ""));
            projections = uptimeLogsRepository.getAggregatedUptimeLogsBySeconds(userId, null, since, seconds);
        } else {
            projections = uptimeLogsRepository.getAggregatedUptimeLogs(userId, null, since, bucket);
        }
        
        List<ResponseTimePointResponse> points = projections.stream()
                .map(p -> ResponseTimePointResponse.builder()
                        .time(p.getTime())
                        .avgLatencyMs(p.getAvgResponseTimeMs() != null ? Math.round(p.getAvgResponseTimeMs() * 10.0) / 10.0 : 0.0)
                        .build())
                .collect(Collectors.toList());

        ResponseTimeChartResponse response = ResponseTimeChartResponse.builder()
                .range(range)
                .bucket(bucket)
                .points(points)
                .build();

        cacheService.set(key, response, 300); // 300s TTL
        return response;
    }

    @Override
    public ErrorRateResponse getErrorRate(UUID userId, String range, int limit) {
        String key = cacheService.buildKey("error-rate", userId, range, String.valueOf(limit));
        Object cached = cacheService.get(key);
        if (cached instanceof ErrorRateResponse) return (ErrorRateResponse) cached;

        LocalDateTime since = parseRange(range);
        List<Object[]> stats = uptimeLogsRepository.getErrorRateStats(userId, since, limit);

        List<ErrorRateResponse.ErrorRateItemResponse> items = stats.stream().map(s -> {
            long total = ((Number) s[2]).longValue();
            long failed = ((Number) s[3]).longValue();
            double rate = total > 0 ? (double) failed * 100 / total : 0.0;
            
            String severity = "LOW";
            if (rate >= 5) severity = "HIGH";
            else if (rate >= 1) severity = "MEDIUM";

            return ErrorRateResponse.ErrorRateItemResponse.builder()
                    .monitorId((UUID) s[0])
                    .monitorName((String) s[1])
                    .totalChecks(total)
                    .failedChecks(failed)
                    .errorRate(Math.round(rate * 100.0) / 100.0)
                    .severity(severity)
                    .build();
        }).collect(Collectors.toList());

        ErrorRateResponse response = ErrorRateResponse.builder()
                .range(range)
                .items(items)
                .build();

        cacheService.set(key, response, 300);
        return response;
    }

    @Override
    public UptimeGaugeResponse getUptimeGauge(UUID userId, String range) {
        String key = cacheService.buildKey("uptime-gauge", userId, range);
        Object cached = cacheService.get(key);
        if (cached instanceof UptimeGaugeResponse) return (UptimeGaugeResponse) cached;

        LocalDateTime since = parseRange(range);
        long total = uptimeLogsRepository.countTotalByUser(userId, since);
        long up = uptimeLogsRepository.countUpByUser(userId, since);
        double percent = total > 0 ? (double) up * 100 / total : 100.0;

        UptimeGaugeResponse response = UptimeGaugeResponse.builder()
                .range(range)
                .uptimePercentage(Math.round(percent * 100.0) / 100.0)
                .successfulChecks(up)
                .totalChecks(total)
                .build();

        cacheService.set(key, response, 120);
        return response;
    }

    @Override
    public UnstableMonitorsResponse getUnstableMonitors(UUID userId, String range, int limit) {
        String key = cacheService.buildKey("unstable-monitors", userId, range, String.valueOf(limit));
        Object cached = cacheService.get(key);
        if (cached instanceof UnstableMonitorsResponse) return (UnstableMonitorsResponse) cached;

        LocalDateTime since = parseRange(range);
        
        // 1. Get incident stats (downtime, incident count)
        List<Object[]> incidentStats = incidentRepository.getIncidentStats(userId, since);
        Map<UUID, Long> downtimeMap = new HashMap<>();
        Map<UUID, Long> incidentCountMap = new HashMap<>();
        for (Object[] row : incidentStats) {
            downtimeMap.put((UUID) row[0], ((Number) row[2]).longValue());
            incidentCountMap.put((UUID) row[0], ((Number) row[1]).longValue());
        }

        // 2. Get error rate stats
        List<Object[]> errorStats = uptimeLogsRepository.getErrorRateStats(userId, since, 100); // Get more to calculate score
        
        List<UnstableMonitorsResponse.UnstableMonitorItemResponse> items = new ArrayList<>();
        for (Object[] row : errorStats) {
            UUID id = (UUID) row[0];
            String name = (String) row[1];
            long total = ((Number) row[2]).longValue();
            long failed = ((Number) row[3]).longValue();
            double rate = total > 0 ? (double) failed * 100 / total : 0.0;
            
            long downtime = downtimeMap.getOrDefault(id, 0L);
            long incidents = incidentCountMap.getOrDefault(id, 0L);
            
            // Formula: score = 100 - (0.4 * downtime) - (0.3 * incidents) - (0.3 * rate)
            // Need to normalize downtime and incidents or just use weights
            double score = 100.0 - (0.4 * Math.min(downtime, 100)) - (0.3 * Math.min(incidents, 100)) - (0.3 * Math.min(rate, 100));
            score = Math.max(0, score);

            items.add(UnstableMonitorsResponse.UnstableMonitorItemResponse.builder()
                    .monitorId(id)
                    .monitorName(name)
                    .downtimeMinutes(downtime)
                    .incidentCount(incidents)
                    .errorRate(Math.round(rate * 100.0) / 100.0)
                    .stabilityScore(Math.round(score * 10.0) / 10.0)
                    .build());
        }

        // Sort by score ASC (most unstable first)
        items.sort(Comparator.comparingDouble(UnstableMonitorsResponse.UnstableMonitorItemResponse::getStabilityScore));
        
        // Limit and add rank/extra info
        List<UnstableMonitorsResponse.UnstableMonitorItemResponse> topItems = items.stream()
                .limit(limit)
                .peek(item -> {
                    var monitor = monitorRepository.findById(item.getMonitorId()).orElse(null);
                    if (monitor != null) {
                        item.setCurrentStatus(monitor.getLastStatus() != null ? monitor.getLastStatus().name() : "UNKNOWN");
                        item.setStatusColor(monitor.getLastStatus() == MonitorStatus.DOWN ? "RED" : 
                                           (monitor.getLastStatus() == MonitorStatus.WARNING ? "YELLOW" : "GREEN"));
                    }
                })
                .collect(Collectors.toList());
        
        for (int i = 0; i < topItems.size(); i++) {
            topItems.get(i).setRank(i + 1);
        }

        UnstableMonitorsResponse response = UnstableMonitorsResponse.builder()
                .range(range)
                .items(topItems)
                .build();

        cacheService.set(key, response, 300);
        return response;
    }

    @Override
    public PlanUsageResponse getPlanUsage(UUID userId) {
        String key = cacheService.buildKey("plan-usage", userId);
        Object cached = cacheService.get(key);
        if (cached instanceof PlanUsageResponse) return (PlanUsageResponse) cached;

        Subscription sub = subscriptionRepository.findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE).orElse(null);
        SubscriptionPlan plan = null;
        if (sub != null) {
            plan = sub.getPlan();
        } else {
            // Default to Free plan (look for first active plan)
            plan = subscriptionPlanRepository.findAll().stream()
                    .filter(p -> "Free".equalsIgnoreCase(p.getName()))
                    .findFirst()
                    .orElse(null);
                    
            if (plan == null) {
                // Fallback hardcoded if DB is empty
                plan = new SubscriptionPlan();
                plan.setName("Free");
                plan.setMaxMonitors(5);
            }
        }

        long used = monitorRepository.countByUserIdAndIsActive(userId, true);
        int limit = plan.getMaxMonitors();
        int remaining = Math.max(0, limit - (int)used);
        double usage = limit > 0 ? (double) used * 100 / limit : 100.0;

        PlanUsageResponse response = PlanUsageResponse.builder()
                .planName(plan.getName())
                .monitorLimit(limit)
                .usedMonitors((int)used)
                .remainingMonitors(remaining)
                .usagePercentage(Math.round(usage * 100.0) / 100.0)
                .canUpgrade(!"Enterprise".equalsIgnoreCase(plan.getName()))
                .build();

        cacheService.set(key, response, 300);
        return response;
    }

    @Override
    public DashboardSuggestionResponse getSuggestions(UUID userId) {
        String key = cacheService.buildKey("suggestions", userId);
        Object cached = cacheService.get(key);
        if (cached instanceof DashboardSuggestionResponse) return (DashboardSuggestionResponse) cached;

        // Rule-based: find monitor with highest error rate in 24h
        List<Object[]> errorStats = uptimeLogsRepository.getErrorRateStats(userId, LocalDateTime.now().minusHours(24), 1);
        
        DashboardSuggestionResponse response;
        if (!errorStats.isEmpty()) {
            Object[] topError = errorStats.get(0);
            double rate = ((Number) topError[3]).doubleValue() * 100 / ((Number) topError[2]).doubleValue();
            
            if (rate > 2.0) {
                response = DashboardSuggestionResponse.builder()
                        .title("SYSTEM SUGGESTION")
                        .message(topError[1] + " has a high error rate (" + Math.round(rate * 100.0)/100.0 + "%). Consider reviewing check interval, timeout, and expected status.")
                        .relatedMonitorId((UUID) topError[0])
                        .relatedMonitorName((String) topError[1])
                        .suggestionType("HIGH_ERROR_RATE")
                        .build();
            } else {
                response = DashboardSuggestionResponse.builder()
                        .title("SYSTEM HEALTH")
                        .message("All monitors are performing well. Your infrastructure seems stable.")
                        .suggestionType("OPTIMIZATION")
                        .build();
            }
        } else {
            response = DashboardSuggestionResponse.builder()
                    .title("GET STARTED")
                    .message("Create your first monitor to start tracking your API health.")
                    .suggestionType("OPTIMIZATION")
                    .build();
        }

        cacheService.set(key, response, 300);
        return response;
    }

    @Override
    public DashboardOverviewResponse getOverview(UUID userId, String range) {
        // Aggregate all data
        String adaptiveBucket = resolveBucket(range);
        return DashboardOverviewResponse.builder()
                .summary(getSummary(userId))
                .responseTimeChart(getResponseTimeChart(userId, range, adaptiveBucket))
                .errorRate(getErrorRate(userId, range, 5))
                .uptimeGauge(getUptimeGauge(userId, range))
                .unstableMonitors(getUnstableMonitors(userId, range, 5))
                .planUsage(getPlanUsage(userId))
                .suggestion(getSuggestions(userId))
                .build();
    }

    private String resolveBucket(String range) {
        if (range == null) return "hour";
        return switch (range.toLowerCase()) {
            case "1h" -> "300s";   // 5m
            case "6h" -> "900s";   // 15m
            case "24h" -> "hour";  // 1h
            case "7d" -> "21600s"; // 6h (6 * 3600)
            case "30d" -> "day";   // 1d
            default -> "hour";
        };
    }

    private LocalDateTime parseRange(String range) {
        if (range == null) return LocalDateTime.now().minusHours(24);
        return switch (range.toLowerCase()) {
            case "1h" -> LocalDateTime.now().minusHours(1);
            case "6h" -> LocalDateTime.now().minusHours(6);
            case "24h" -> LocalDateTime.now().minusHours(24);
            case "7d" -> LocalDateTime.now().minusDays(7);
            case "30d" -> LocalDateTime.now().minusDays(30);
            default -> LocalDateTime.now().minusHours(24);
        };
    }
}
