package com.example.demo.modules.dashboard.services;

import com.example.demo.modules.dashboard.dto.*;
import com.example.demo.modules.monitor.repositories.MonitorRepository;
import com.example.demo.modules.uptimeLogs.repositories.UptimeLogsRepository;
import com.example.demo.modules.alert.repositories.IncidentRepository;
import com.example.demo.modules.revenue.services.IRevenueService;
import com.example.demo.modules.user.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminDashboardServiceImpl implements IAdminDashboardService {

    private final MonitorRepository monitorRepository;
    private final UptimeLogsRepository uptimeLogsRepository;
    private final UserRepository userRepository;
    private final IncidentRepository incidentRepository;
    private final IRevenueService revenueService;
    private final com.example.demo.modules.system.services.IAdminSystemService adminSystemService;

    private static final String CACHE_ADMIN_DASHBOARD = "admin:dashboard";

    @Override
    @Cacheable(value = CACHE_ADMIN_DASHBOARD, key = "'v2:stats:' + #range")
    public AdminDashboardV2StatsResponse getV2Stats(String range) {
        LocalDateTime since = parseRange(range);
        int minutes = calculateBucketMinutes(range);
        LocalDateTime prevSince = since.minusMinutes(minutes);

        // 1. Total APIs
        long totalApis = monitorRepository.count();
        long newApis = monitorRepository.countByCreatedAtAfter(since);
        long prevNewApis = monitorRepository.countByCreatedAtAfter(prevSince) - newApis;
        String apiTrendStr = calculateGrowthStr(newApis, prevNewApis);

        var totalApiStat = AdminDashboardV2StatsResponse.StatItem.builder()
                .value(String.valueOf(totalApis))
                .subValue("Total Monitors")
                .trend(apiTrendStr)
                .trendUp(newApis >= prevNewApis)
                .build();

        // 2. Warning APIs
        long warningApis = monitorRepository.countByLastStatus(com.example.demo.modules.monitor.enums.MonitorStatus.WARNING);
        var warningApiStat = AdminDashboardV2StatsResponse.StatItem.builder()
                .value(String.valueOf(warningApis))
                .subValue("Needs Attention")
                .trend("")
                .trendUp(false)
                .build();

        // 3. Down APIs
        long downApis = monitorRepository.countByLastStatus(com.example.demo.modules.monitor.enums.MonitorStatus.DOWN);
        var downApiStat = AdminDashboardV2StatsResponse.StatItem.builder()
                .value(String.valueOf(downApis))
                .subValue("Critical Issues")
                .trend("")
                .trendUp(false)
                .build();

        // 4. Avg Latency
        Double avgLatency = uptimeLogsRepository.getAvgLatencyGlobal(since);
        double currentLatency = avgLatency != null ? avgLatency : 0.0;
        
        var latencyStat = AdminDashboardV2StatsResponse.StatItem.builder()
                .value(String.format("%.0fms", currentLatency))
                .subValue("Response Time")
                .trend("")
                .trendUp(false)
                .build();

        // 5. Checks/min
        List<Object[]> uptimeStats = uptimeLogsRepository.getGlobalUptimeStats(since);
        long totalChecks = 0;
        if (!uptimeStats.isEmpty() && uptimeStats.get(0) != null) {
            totalChecks = uptimeStats.get(0)[0] != null ? ((Number) uptimeStats.get(0)[0]).longValue() : 0;
        }
        
        double checksPerMin = minutes > 0 ? (double) totalChecks / minutes : 0;
        
        var checksStat = AdminDashboardV2StatsResponse.StatItem.builder()
                .value(String.format("%.1f", checksPerMin))
                .subValue("Checks/Min")
                .trend("")
                .trendUp(true)
                .build();

        return AdminDashboardV2StatsResponse.builder()
                .totalApis(totalApiStat)
                .warningApis(warningApiStat)
                .downApis(downApiStat)
                .avgLatency(latencyStat)
                .checksPerMin(checksStat)
                .build();
    }

    @Override
    @Cacheable(value = CACHE_ADMIN_DASHBOARD, key = "'v2:stats-cards'")
    public AdminCardStatsResponse getCardStats() {
        // 1. Revenue
        var revStats = revenueService.getRevenueStats();
        BigDecimal mrr = revStats.getMrr();
        Double mrrGrowth = revStats.getMrrGrowth();
        
        String revenueValue = "$" + (mrr != null ? mrr.setScale(0, java.math.RoundingMode.HALF_UP).toString() : "0");
        String revenueTrendStr = mrrGrowth != null ? String.format("%+.1f%%", mrrGrowth) : "0%";
        boolean revenueTrendUp = mrrGrowth == null || mrrGrowth >= 0;

        var revenueStat = AdminCardStatsResponse.StatItem.builder()
                .value(revenueValue)
                .subValue("Monthly Recurring Revenue")
                .trend(revenueTrendStr)
                .trendUp(revenueTrendUp)
                .build();

        // 2. Total Users
        long totalUsersCount = userRepository.count();
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        LocalDateTime sixtyDaysAgo = LocalDateTime.now().minusDays(60);
        
        long newUsers = userRepository.countByCreatedAtAfter(thirtyDaysAgo);
        long prevNewUsers = userRepository.countByCreatedAtAfter(sixtyDaysAgo) - newUsers;
        
        String usersTrendStr = calculateGrowthStr(newUsers, prevNewUsers);
        boolean usersTrendUp = newUsers >= prevNewUsers;

        var usersStat = AdminCardStatsResponse.StatItem.builder()
                .value(String.valueOf(totalUsersCount))
                .subValue("Total Registered")
                .trend(usersTrendStr)
                .trendUp(usersTrendUp)
                .build();

        // 3. APIs Monitored
        long totalApis = monitorRepository.count();
        long newApis = monitorRepository.countByCreatedAtAfter(thirtyDaysAgo);
        long prevNewApis = monitorRepository.countByCreatedAtAfter(sixtyDaysAgo) - newApis;
        
        String apiTrendStr = calculateGrowthStr(newApis, prevNewApis);
        boolean apiTrendUp = newApis >= prevNewApis;

        var apisMonitoredStat = AdminCardStatsResponse.StatItem.builder()
                .value(String.valueOf(totalApis))
                .subValue("Active Monitors")
                .trend(apiTrendStr)
                .trendUp(apiTrendUp)
                .build();

        // 4. APIs Down
        long downApis = monitorRepository.countByLastStatus(com.example.demo.modules.monitor.enums.MonitorStatus.DOWN);
        String downTrendStr = downApis == 0 ? "Stable" : String.valueOf(downApis);
        boolean downTrendUp = downApis == 0;

        var apisDownStat = AdminCardStatsResponse.StatItem.builder()
                .value(String.valueOf(downApis))
                .subValue("Critical Issues")
                .trend(downTrendStr)
                .trendUp(downTrendUp)
                .build();

        // 5. Alerts Today
        LocalDateTime startOfToday = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime startOfYesterday = startOfToday.minusDays(1);
        
        long alertsTodayCount = incidentRepository.countByTriggeredAtAfter(startOfToday);
        long alertsYesterdayCount = incidentRepository.countByTriggeredAtAfter(startOfYesterday) - alertsTodayCount;
        
        String alertsTrendStr = calculateGrowthStr(alertsTodayCount, alertsYesterdayCount);
        boolean alertsTrendUp = alertsTodayCount <= alertsYesterdayCount;

        var alertsTodayStat = AdminCardStatsResponse.StatItem.builder()
                .value(String.valueOf(alertsTodayCount))
                .subValue("Triggered Today")
                .trend(alertsTrendStr)
                .trendUp(alertsTrendUp)
                .build();

        return AdminCardStatsResponse.builder()
                .revenue(revenueStat)
                .totalUsers(usersStat)
                .apisMonitored(apisMonitoredStat)
                .apisDown(apisDownStat)
                .alertsToday(alertsTodayStat)
                .build();
    }

    @Override
    @Cacheable(value = CACHE_ADMIN_DASHBOARD, key = "'v2:performance:' + #range")
    public AdminPerformanceResponse getPerformance(String range) {
        LocalDateTime since = parseRange(range);
        
        Double avgLatency = uptimeLogsRepository.getAvgLatencyGlobal(since);
        UptimeGaugeResponse uptimeStats = getGlobalUptime(range);

        // Tính Error Rate thực từ DB: số lần check thất bại / tổng số lần check
        List<Object[]> errorStats = uptimeLogsRepository.getGlobalErrorRateStats(since);
        double errorRate = 0.0;
        if (!errorStats.isEmpty() && errorStats.get(0) != null) {
            Object[] row = errorStats.get(0);
            long total = row[0] != null ? ((Number) row[0]).longValue() : 0L;
            long failed = row[1] != null ? ((Number) row[1]).longValue() : 0L;
            errorRate = total > 0 ? (double) failed / total * 100 : 0.0;
        }

        // Generate Chart Data (15-20 points)
        List<Double> chartData = new ArrayList<>();
        ResponseTimeChartResponse trend = getGlobalResponseTimeTrend(range);
        if (trend.getPoints() != null) {
            chartData = trend.getPoints().stream()
                    .map(p -> p.getAvgLatencyMs())
                    .limit(20)
                    .collect(Collectors.toList());
        }

        return AdminPerformanceResponse.builder()
                .avgResponseTime(String.format("%.0fms", avgLatency != null ? avgLatency : 0.0))
                .uptimePercentage(String.format("%.2f%%", uptimeStats.getUptimePercentage()))
                .errorRate(String.format("%.2f%%", errorRate))
                .chartData(chartData)
                .build();
    }

    @Override
    public AdminInfrastructureResponse getInfrastructure() {
        int activeWorkers = adminSystemService.getActiveWorkerCount();
        int totalWorkers = adminSystemService.getTotalWorkerCount();
        double dbLoadPercent = adminSystemService.getDbLoadPercent();
        long uptimeMs = adminSystemService.getServerUptimeMs();

        // Lấy số lượng message thực trong queue để đánh giá trạng thái
        int queueDepth = adminSystemService.getQueueMessageCount();
        String queueLabel;
        String queueType;
        if (queueDepth == 0) {
            queueLabel = "Healthy";
            queueType = "HEALTHY";
        } else if (queueDepth < 50) {
            queueLabel = "Busy (" + queueDepth + " pending)";
            queueType = "BUSY";
        } else {
            queueLabel = "Overloaded (" + queueDepth + " pending)";
            queueType = "OVERLOADED";
        }

        return AdminInfrastructureResponse.builder()
                .workers(AdminInfrastructureResponse.WorkerStatus.builder()
                        .active(activeWorkers)
                        .total(totalWorkers)
                        .build())
                .dbLoad(String.format("%.1f%%", dbLoadPercent))
                .serverUptime(formatUptime(uptimeMs))
                .queueStatus(AdminInfrastructureResponse.QueueStatus.builder()
                        .label(queueLabel)
                        .type(queueType)
                        .build())
                .build();
    }

    @Override
    @Cacheable(value = CACHE_ADMIN_DASHBOARD, key = "'v2:activity'")
    public List<AdminActivityResponse> getLatestActivity() {
        return uptimeLogsRepository.findLatestLogsGlobal(PageRequest.of(0, 10)).stream()
                .map(log -> {
                    String status = "HEALTHY";
                    if (!log.getIsUp()) {
                        status = "TIMEOUT".equalsIgnoreCase(log.getErrorType()) ? "TIMEOUT" : "ERROR";
                    }

                    return AdminActivityResponse.builder()
                            .apiName(log.getMonitor().getName())
                            .owner(log.getMonitor().getUser() != null ? log.getMonitor().getUser().getEmail() : "System")
                            .endpoint(log.getMonitor().getUrl())
                            .responseTime(log.getResponseTimeMs() != null ? log.getResponseTimeMs() + "ms" : "N/A")
                            .status(status)
                            .lastCheck(calculateRelativeTime(log.getCheckedAt()))
                            .build();
                })
                .collect(Collectors.toList());
    }

    private String calculateRelativeTime(LocalDateTime checkedAt) {
        if (checkedAt == null) return "Unknown";
        long seconds = java.time.Duration.between(checkedAt, LocalDateTime.now()).getSeconds();
        if (seconds < 60) return seconds + "s ago";
        if (seconds < 3600) return (seconds / 60) + " mins ago";
        if (seconds < 86400) return (seconds / 3600) + " hours ago";
        return (seconds / 86400) + " days ago";
    }

    /**
     * Tính chuỗi % tăng trưởng giữa giá trị hiện tại và giá trị kỳ trước.
     * Ví dụ: current=10, previous=8 → "+25.0%"
     */
    private String calculateGrowthStr(long current, long previous) {
        if (previous == 0) {
            return current > 0 ? "+100%" : "0%";
        }
        double growth = (double) (current - previous) / previous * 100;
        return String.format("%+.1f%%", growth);
    }

    /**
     * Chuyển đổi thời gian uptime từ milliseconds sang chuỗi dễ đọc.
     * Ví dụ: 123456789 ms → "1 Days 10 Hrs" hoặc "5 Hrs 30 Mins".
     */
    private String formatUptime(long uptimeMs) {
        long totalSeconds = uptimeMs / 1000;
        long days = totalSeconds / 86400;
        long hours = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;

        if (days > 0) {
            return days + " Days " + hours + " Hrs";
        } else if (hours > 0) {
            return hours + " Hrs " + minutes + " Mins";
        } else {
            return minutes + " Mins";
        }
    }

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

    private int calculateBucketMinutes(String range) {
        if (range == null) return 1440; // 1 day
        switch (range.toLowerCase()) {
            case "1h": return 60;
            case "6h": return 360;
            case "7d": return 10080;
            case "30d": return 43200;
            case "1d":
            default: return 1440;
        }
    }
}
