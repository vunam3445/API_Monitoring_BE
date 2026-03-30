package com.example.demo.modules.uptimeLogs.repositories;

import com.example.demo.modules.uptimeLogs.entities.UptimeLogs;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface UptimeLogsRepository extends JpaRepository<UptimeLogs, Long>, JpaSpecificationExecutor<UptimeLogs> {

    /**
     * Tìm uptime logs theo monitorId, sắp xếp theo thời gian mới nhất (phân trang).
     */
    Page<UptimeLogs> findByMonitorIdOrderByCheckedAtDesc(UUID monitorId, Pageable pageable);

    /**
     * Tìm uptime logs theo monitorId trong khoảng thời gian (cho biểu đồ).
     */
    Page<UptimeLogs> findByMonitorIdAndCheckedAtBetweenOrderByCheckedAtDesc(
            UUID monitorId, LocalDateTime from, LocalDateTime to, Pageable pageable);

    /**
     * Đếm số lần check thành công (isUp = true) theo monitorId.
     * Dùng để tính uptime percentage.
     */
    long countByMonitorIdAndIsUp(UUID monitorId, Boolean isUp);

    /**
     * Đếm tổng số lần check theo monitorId.
     */
    long countByMonitorId(UUID monitorId);
}
