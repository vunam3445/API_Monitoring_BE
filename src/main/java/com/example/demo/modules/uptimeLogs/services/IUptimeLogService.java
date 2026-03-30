package com.example.demo.modules.uptimeLogs.services;

import com.example.demo.modules.uptimeLogs.dto.UptimeLogResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Interface service cho UptimeLogs.
 *
 * UptimeLogs là read-only từ phía API (Worker ghi trực tiếp entity).
 * Áp dụng ISP: chỉ expose các method đọc, không có create/update/delete.
 */
public interface IUptimeLogService {

    /**
     * Lấy lịch sử uptime logs theo monitorId (phân trang, mới nhất trước).
     */
    Page<UptimeLogResponse> findByMonitorId(UUID monitorId, Pageable pageable);

    /**
     * Lấy lịch sử uptime logs theo monitorId trong khoảng thời gian.
     */
    Page<UptimeLogResponse> findByMonitorIdAndDateRange(
            UUID monitorId, LocalDateTime from, LocalDateTime to, Pageable pageable);

    /**
     * Tính phần trăm uptime của một monitor.
     * Formula: (số lần UP / tổng số check) * 100
     */
    Double calculateUptimePercentage(UUID monitorId);

    /**
     * Lấy lịch sử uptime logs của user (search theo name, statusCode, method).
     */
    Page<UptimeLogResponse> findLogsByUser(
            UUID userId, String search, Integer statusCode, String method, Pageable pageable);
}
