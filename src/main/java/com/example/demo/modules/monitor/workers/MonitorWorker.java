package com.example.demo.modules.monitor.workers;

import com.example.demo.modules.monitor.entities.Monitor;
import com.example.demo.modules.monitor.enums.MonitorStatus;
import com.example.demo.modules.monitor.enums.MonitorEventType;
import com.example.demo.modules.monitor.execution.ApiExecutionService;
import com.example.demo.modules.monitor.lock.DistributedLockService;
import com.example.demo.modules.monitor.messaging.MonitorExecutionMessage;
import com.example.demo.modules.monitor.messaging.MonitorMQConfig;
import com.example.demo.modules.monitor.repositories.MonitorRepository;
import com.example.demo.modules.uptimeLogs.entities.UptimeLogs;
import com.example.demo.modules.uptimeLogs.repositories.UptimeLogsRepository;
import com.example.demo.modules.alert.services.IIncidentService;
import com.example.demo.modules.user.repositories.UserSettingRepository;
import com.example.demo.modules.dashboard.services.DashboardCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Worker nhận job từ RabbitMQ và thực thi kiểm tra API.
 *
 * Luồng xử lý:
 * 1. Nhận message chứa monitorId từ queue.
 * 2. Query Monitor entity từ DB (lấy config mới nhất).
 * 3. Gọi ApiExecutionService để thực thi HTTP request.
 * 4. Lưu kết quả vào bảng UptimeLogs.
 * 5. Cập nhật trạng thái gần nhất (last_status, last_latency, next_check_at)
 * trên Monitor.
 * 6. Giải phóng Redis lock.
 *
 * Single Responsibility: Worker CHỈ orchestrate luồng, logic gọi API nằm trong
 * ApiExecutionService.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MonitorWorker {

    private final MonitorRepository monitorRepository;
    private final UptimeLogsRepository uptimeLogsRepository;
    private final ApiExecutionService apiExecutionService;
    private final DistributedLockService lockService;
    private final UserSettingRepository userSettingRepository;
    private final com.example.demo.common.cache.ICacheService cacheService;
    private final IIncidentService incidentService;
    private final DashboardCacheService dashboardCacheService;

    @RabbitListener(queues = MonitorMQConfig.QUEUE_NAME)
    public void processMonitorJob(MonitorExecutionMessage message) {
        String monitorId = message.getMonitorId();
        log.info("Received execution job for monitor: {} (scheduled at: {})",
                monitorId, message.getScheduledAt());

        try {
            // 1. Lấy Monitor từ DB (dữ liệu mới nhất)
            Optional<Monitor> optionalMonitor = monitorRepository.findById(UUID.fromString(monitorId));
            if (optionalMonitor.isEmpty()) {
                log.warn("Monitor not found: {}. Skipping.", monitorId);
                return;
            }

            Monitor monitor = optionalMonitor.get();

            // Kiểm tra monitor vẫn active và không bị khóa (có thể bị tắt giữa lúc schedule và execute)
            if (!Boolean.TRUE.equals(monitor.getIsActive()) || Boolean.TRUE.equals(monitor.getIsBlock())) {
                log.info("Monitor {} is no longer active or is blocked. Skipping.", monitorId);
                return;
            }

            // 2. Lấy UserSetting để lấy timeout & failCount
            com.example.demo.modules.user.entities.UserSetting setting = userSettingRepository
                    .findById(monitor.getUserId()).orElse(null);
            Integer timeoutMs = (setting != null) ? setting.getDefaultTimeoutMs() : 5000;

            // 3. Thực thi gọi API (có retry if configured)
            int retryAttempts = (setting != null && setting.getRetryAttempts() != null) ? setting.getRetryAttempts()
                    : 0;
            UptimeLogs result = null;

            for (int i = 0; i <= retryAttempts; i++) {
                result = apiExecutionService.execute(monitor, timeoutMs);
                if (Boolean.TRUE.equals(result.getIsUp())) {
                    break; // Thành công thì thoát loop
                }
                if (i < retryAttempts) {
                    log.info("API check failed for {}, retrying... (Attempt {}/{})", monitor.getName(), i + 1,
                            retryAttempts);
                }
            }

            // 4. Lưu kết quả vào bảng uptime_logs
            uptimeLogsRepository.save(result);
            log.info("Saved uptime log for monitor: {} | isUp={} | statusCode={} | responseTime={}ms",
                    monitor.getName(), result.getIsUp(), result.getStatusCode(), result.getResponseTimeMs());

            // 5. Cập nhật trạng thái gần nhất trên Monitor entity
            updateMonitorStatus(monitor, result, setting);

        } catch (Exception e) {
            log.error("Error processing monitor job: {}", monitorId, e);
        } finally {
            // 5. Luôn giải phóng lock sau khi xử lý xong
            lockService.unlock(monitorId);
        }
    }

    /**
     * Cập nhật các trường trạng thái gần nhất trên Monitor.
     * Giúp dashboard hiển thị nhanh mà không cần query bảng uptime_logs.
     */
    @Transactional
    private void updateMonitorStatus(Monitor monitor, UptimeLogs result,
            com.example.demo.modules.user.entities.UserSetting setting) {
        int defaultFailCount = (setting != null) ? setting.getDefaultFailCount() : 3;

        // Cập nhật trạng thái gần nhất
        if (!result.getIsUp()) {
            // Chỉ đặt là "Down" nếu số lần lỗi liên tiếp vượt quá ngưỡng cấu hình
            int currentFailures = (monitor.getConsecutiveFailures() != null ? monitor.getConsecutiveFailures() : 0) + 1;
            if (currentFailures >= defaultFailCount) {
                monitor.setLastStatus(MonitorStatus.DOWN);
            } else {
                // Đang lỗi nhưng chưa đủ số lần để confirm Down -> hiển thị cảnh báo Warning
                // hoặc giữ nguyên
                monitor.setLastStatus(MonitorStatus.WARNING);
            }
        } else if ("WARNING".equals(result.getAssertionStatus())) {
            monitor.setLastStatus(MonitorStatus.WARNING);
        } else {
            monitor.setLastStatus(MonitorStatus.HEALTHY);
        }

        monitor.setLastLatencyMs(result.getResponseTimeMs());
        monitor.setLastCheckAt(LocalDateTime.now());
        monitor.setLastErrorMessage(result.getIsUp() ? null : result.getErrorMessage());

        // Cập nhật consecutive failures
        if (Boolean.TRUE.equals(result.getIsUp())) {
            // Nếu trước đó đang fail mà giờ thành công -> RECOVERED
            if (monitor.getConsecutiveFailures() != null && monitor.getConsecutiveFailures() > 0) {
                result.setEventType(MonitorEventType.RECOVERED);
            }
            // Bao gồm cả Healthy và Warning đều tính là Up, reset số lần fail liên tiếp
            monitor.setConsecutiveFailures(0);

            // Trigger incident resolution check
            incidentService.processCheckResult(monitor, result);
        } else {
            int current = (monitor.getConsecutiveFailures() != null ? monitor.getConsecutiveFailures() : 0) + 1;
            monitor.setConsecutiveFailures(current);

            // CHỈ kích hoạt incident khi số lần lỗi liên tiếp vượt ngưỡng (Threshold)
            if (current >= defaultFailCount) {
                incidentService.processCheckResult(monitor, result);
            }
        }

        // Tính nextCheckAt dựa trên checkInterval
        int intervalSeconds = monitor.getCheckInterval() != null ? monitor.getCheckInterval() : 300; // default 5 phút
        monitor.setNextCheckAt(LocalDateTime.now().plusSeconds(intervalSeconds));

        monitorRepository.save(monitor);

        // Xóa Cache để hệ thống biết bảng dữ liệu đã có thay đổi (chống stale cache)
        cacheService.evictByPrefix("api-monitoring:api:list::");
        cacheService.evict("api-monitoring:api:object::monitor:" + monitor.getId());
        cacheService.evictByPrefix("api-monitoring:uptime-logs::");
        cacheService.evict("monitoring:summary:" + monitor.getUserId());
        cacheService.evict("monitoring:key-health:" + monitor.getUserId());
        cacheService.evictByPrefix("monitoring:recent-events:" + monitor.getUserId());
        cacheService.evict("monitoring:overview:" + monitor.getId());
        dashboardCacheService.clearUserDashboardCache(monitor.getUserId());
    }
}
