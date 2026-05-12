package com.example.demo.modules.monitor.scheduler;

import com.example.demo.modules.monitor.entities.Monitor;
import com.example.demo.modules.monitor.lock.DistributedLockService;
import com.example.demo.modules.monitor.messaging.MonitorProducer;
import com.example.demo.modules.monitor.repositories.MonitorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduler quét định kỳ database để tìm các Monitor đến hạn kiểm tra.
 *
 * Luồng hoạt động:
 * 1. Mỗi 30 giây, query danh sách monitor có nextCheckAt <= now hoặc chưa chạy lần nào.
 * 2. Với mỗi monitor, thử claim lock qua Redis (tránh trùng khi chạy multi-instance).
 * 3. Nếu lock thành công → đẩy monitorId vào RabbitMQ.
 * 4. Nếu lock thất bại → bỏ qua (instance khác đã claim).
 *
 * Single Responsibility: Scheduler CHỈ lo việc scan + dispatch, KHÔNG thực thi gọi API.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MonitorScheduler {

    private final MonitorRepository monitorRepository;
    private final DistributedLockService lockService;
    private final MonitorProducer monitorProducer;

    /**
     * Lock TTL = 120 giây.
     * Đủ thời gian cho Worker hoàn thành xử lý rồi unlock.
     * Nếu Worker chết giữa chừng, lock tự hết hạn sau 120s → monitor sẽ được retry ở lần scan tiếp.
     */
    private static final long LOCK_TTL_SECONDS = 120;

    /**
     * Quét database mỗi 30 giây.
     * fixedDelay đảm bảo lần scan mới chỉ bắt đầu sau khi lần trước kết thúc.
     */
    @Scheduled(fixedDelay = 30000)
    public void scanDueMonitors() {
        LocalDateTime now = LocalDateTime.now();
        List<Monitor> dueMonitors = monitorRepository.findDueMonitors(now);

        if (dueMonitors.isEmpty()) {
            log.debug("No monitors due for checking at {}", now);
            return;
        }

        log.info("Found {} monitors due for checking", dueMonitors.size());

        for (Monitor monitor : dueMonitors) {
            if (!Boolean.TRUE.equals(monitor.getIsActive()) || Boolean.TRUE.equals(monitor.getIsBlock())) {
                continue;
            }

            String monitorId = monitor.getId().toString();

            // Thử claim lock: chỉ 1 instance được xử lý monitor này
            if (lockService.tryLock(monitorId, LOCK_TTL_SECONDS)) {
                try {
                    monitorProducer.sendExecutionJob(monitorId);
                } catch (Exception e) {
                    // Nếu gửi message thất bại → giải phóng lock để instance khác retry
                    log.error("Failed to send execution job for monitor: {}. Releasing lock.", monitorId, e);
                    lockService.unlock(monitorId);
                }
            }
        }
    }
}
