package com.example.demo.modules.system.services;

import com.example.demo.modules.system.entities.ApplicationLog;
import org.springframework.data.domain.Page;

import java.util.Map;

/**
 * Interface nghiệp vụ quản lý Application Log (log lỗi nội bộ của hệ thống Backend).
 * KHÔNG liên quan đến log giám sát API của người dùng.
 */
public interface IApplicationLogService {

    /**
     * Lấy danh sách Application Logs với phân trang và bộ lọc đa tiêu chí.
     *
     * @param page      Số trang (0-indexed)
     * @param size      Số dòng log mỗi trang
     * @param level     Cấp độ lọc: ALL, WARN, ERROR, FATAL
     * @param keyword   Từ khóa tìm kiếm (message, component, threadId)
     * @param timeRange Khoảng thời gian: ALL, 5m, 1h, 24h
     */
    Page<ApplicationLog> getLogs(int page, int size, String level, String keyword, String timeRange);

    /**
     * Lấy thống kê log phát sinh trong ngày hôm nay (từ 00:00:00 đến hiện tại).
     */
    Map<String, Object> getTodayStats();

    /**
     * Xóa các log cũ hơn số ngày quy định (dọn dẹp thủ công).
     *
     * @param retentionDays Số ngày giữ lại log. Log cũ hơn mức này sẽ bị xóa.
     */
    Map<String, Object> clearOldLogs(int retentionDays);
}
