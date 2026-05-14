package com.example.demo.modules.dashboard.services;

import com.example.demo.modules.dashboard.dto.*;
import com.example.demo.modules.monitor.repositories.MonitorRepository;
import com.example.demo.modules.uptimeLogs.repositories.UptimeLogsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminDashboardServiceImpl implements IAdminDashboardService {

    private final MonitorRepository monitorRepository;
    private final UptimeLogsRepository uptimeLogsRepository;

    private static final String CACHE_ADMIN_DASHBOARD = "admin:dashboard";

    @Override
    @Cacheable(value = CACHE_ADMIN_DASHBOARD, key = "'stats'")
    public AdminDashboardStatsResponse getGlobalStats() {
        List<Object[]> monitorStatsList = monitorRepository.countGlobalMonitorStats();
        if (monitorStatsList.isEmpty() || monitorStatsList.get(0) == null) {
            return AdminDashboardStatsResponse.builder().build();
        }

        Object[] monitorStats = monitorStatsList.get(0);
        long total = monitorStats[0] != null ? ((Number) monitorStats[0]).longValue() : 0L;
        long healthy = monitorRepository.countByUserIdAndLastStatus(null,
                com.example.demo.modules.monitor.enums.MonitorStatus.HEALTHY); // Assuming countByUserId handles null as
                                                                               // global
        // Wait, countByUserIdAndLastStatus might not handle null userId if it's not
        // implemented that way.
        // Let's use custom counts if needed.

        // Actually, the card HEALTHY/WARNING/DOWN should be based on lastStatus.
        long down = ((Number) monitorStats[2]).longValue();

        // Let's get Healthy and Warning explicitly
        long healthyCount = monitorRepository
                .countByLastStatus(com.example.demo.modules.monitor.enums.MonitorStatus.HEALTHY);
        long warningCount = monitorRepository
                .countByLastStatus(com.example.demo.modules.monitor.enums.MonitorStatus.WARNING);

        Double avgLatency = uptimeLogsRepository.getAvgLatencyGlobal(LocalDateTime.now().minusDays(1));
        Double checksPerMin = monitorStats[5] != null ? ((Number) monitorStats[5]).doubleValue() : 0.0;

        return AdminDashboardStatsResponse.builder()
                .totalApis(total)
                .healthy(healthyCount)
                .warning(warningCount)
                .down(down)
                .avgLatencyMs(avgLatency != null ? avgLatency : 0.0)
                .checksPerMin(checksPerMin != null ? checksPerMin : 0.0)
                .build();
    }

    @Override
    @Cacheable(value = CACHE_ADMIN_DASHBOARD, key = "'trend:' + #range")
    public ResponseTimeChartResponse getGlobalResponseTimeTrend(String range) {
        LocalDateTime since = parseRange(range);
        int seconds = calculateBucketSeconds(range);

        List<Object[]> results = uptimeLogsRepository.getGlobalResponseTimeTrend(since, seconds);

        List<ResponseTimePointResponse> points = results.stream()
                .map(res -> {
                    LocalDateTime time;
                    if (res[0] instanceof java.sql.Timestamp) {
                        time = ((java.sql.Timestamp) res[0]).toLocalDateTime();
                    } else if (res[0] instanceof java.time.Instant) {
                        time = LocalDateTime.ofInstant((java.time.Instant) res[0], java.time.ZoneId.systemDefault());
                    } else {
                        time = LocalDateTime.parse(res[0].toString());
                    }

                    return ResponseTimePointResponse.builder()
                            .time(time)
                            .avgLatencyMs(res[1] != null ? ((Number) res[1]).doubleValue() : 0.0)
                            .build();
                })
                .collect(Collectors.toList());

        return ResponseTimeChartResponse.builder()
                .range(range)
                .bucket(String.valueOf(seconds) + "s")
                .points(points)
                .build();
    }

    @Override
    @Cacheable(value = CACHE_ADMIN_DASHBOARD, key = "'uptime:' + #range")
    public UptimeGaugeResponse getGlobalUptime(String range) {
        LocalDateTime since = parseRange(range);
        List<Object[]> results = uptimeLogsRepository.getGlobalUptimeStats(since);
        if (results.isEmpty() || results.get(0) == null) {
            return UptimeGaugeResponse.builder().range(range).uptimePercentage(100.0).build();
        }

        Object[] row = results.get(0);
        long total = row[0] != null ? ((Number) row[0]).longValue() : 0L;
        long up = row[1] != null ? ((Number) row[1]).longValue() : 0L;
        double percentage = total > 0 ? (double) up / total * 100 : 100.0;

        return UptimeGaugeResponse.builder()
                .range(range)
                .uptimePercentage(percentage)
                .totalChecks(total)
                .successfulChecks(up)
                .build();
    }

    @Override
    @Cacheable(value = CACHE_ADMIN_DASHBOARD, key = "'methods:' + #range")
    public MethodDistributionResponse getGlobalMethodDistribution(String range) {
        LocalDateTime since = parseRange(range);
        List<Object[]> results = uptimeLogsRepository.getMethodDistribution(since);

        long totalCount = results.stream().mapToLong(res -> ((Number) res[1]).longValue()).sum();

        List<MethodDistributionResponse.MethodCount> distributions = results.stream()
                .map(res -> {
                    long count = ((Number) res[1]).longValue();
                    double percentage = totalCount > 0 ? (double) count / totalCount * 100 : 0.0;
                    return new MethodDistributionResponse.MethodCount(res[0].toString(), count, percentage);
                })
                .collect(Collectors.toList());

        return MethodDistributionResponse.builder()
                .distributions(distributions)
                .build();
    }

    private LocalDateTime parseRange(String range) {
        if (range == null)
            return LocalDateTime.now().minusDays(1);
        switch (range.toLowerCase()) {
            case "1h":
                return LocalDateTime.now().minusHours(1);
            case "6h":
                return LocalDateTime.now().minusHours(6);
            case "7d":
                return LocalDateTime.now().minusDays(7);
            case "30d":
                return LocalDateTime.now().minusDays(30);
            case "1d":
            default:
                return LocalDateTime.now().minusDays(1);
        }
    }

    private int calculateBucketSeconds(String range) {
        if (range == null)
            return 3600; // 1 hour
        switch (range.toLowerCase()) {
            case "1h":
                return 300; // 5 min
            case "6h":
                return 1800; // 30 min
            case "7d":
                return 86400; // 1 day
            case "30d":
                return 86400 * 3; // 3 days
            case "1d":
            default:
                return 3600;
        }
    }
}
