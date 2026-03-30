package com.example.demo.modules.uptimeLogs.controllers;

import com.example.demo.modules.uptimeLogs.dto.UptimeLogResponse;
import com.example.demo.modules.uptimeLogs.services.IUptimeLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Controller cho UptimeLogs - chỉ expose các endpoint đọc.
 *
 * Không kế thừa BaseController vì UptimeLogs không cần CRUD đầy đủ.
 * Worker ghi dữ liệu tự động, frontend chỉ cần xem lịch sử.
 */
@RestController
@RequestMapping("/api/uptime-logs")
@RequiredArgsConstructor
public class UptimeLogController {

    private final IUptimeLogService uptimeLogService;

    /**
     * Lấy lịch sử uptime logs của một monitor (phân trang).
     *
     * GET /api/uptime-logs/monitor/{monitorId}?page=0&size=20
     */
    @GetMapping("/monitor/{monitorId}")
    public ResponseEntity<Page<UptimeLogResponse>> getLogsByMonitorId(
            @PathVariable UUID monitorId,
            Pageable pageable) {
        return ResponseEntity.ok(uptimeLogService.findByMonitorId(monitorId, pageable));
    }

    /**
     * Lấy lịch sử uptime logs theo khoảng thời gian (cho biểu đồ).
     *
     * GET /api/uptime-logs/monitor/{monitorId}/range?from=2026-03-01T00:00:00&to=2026-03-29T23:59:59&page=0&size=100
     */
    @GetMapping("/monitor/{monitorId}/range")
    public ResponseEntity<Page<UptimeLogResponse>> getLogsByDateRange(
            @PathVariable UUID monitorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            Pageable pageable) {
        return ResponseEntity.ok(
                uptimeLogService.findByMonitorIdAndDateRange(monitorId, from, to, pageable));
    }

    /**
     * Tính phần trăm uptime của một monitor.
     *
     * GET /api/uptime-logs/monitor/{monitorId}/uptime
     */
    @GetMapping("/monitor/{monitorId}/uptime")
    public ResponseEntity<Double> getUptimePercentage(@PathVariable UUID monitorId) {
        return ResponseEntity.ok(uptimeLogService.calculateUptimePercentage(monitorId));
    }

    /**
     * Lấy lịch sử uptime logs của user (search theo name, statusCode, method).
     *
     * GET /api/uptime-logs/user/{userId}?search=Auth&statusCode=200&method=GET&page=0&size=20&sort=checkedAt,desc
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<UptimeLogResponse>> getLogsByUserId(
            @PathVariable UUID userId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Integer statusCode,
            @RequestParam(required = false) String method,
            Pageable pageable) {
        return ResponseEntity.ok(
                uptimeLogService.findLogsByUser(userId, search, statusCode, method, pageable));
    }
}
