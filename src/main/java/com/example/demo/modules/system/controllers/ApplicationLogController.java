package com.example.demo.modules.system.controllers;

import com.example.demo.modules.system.entities.ApplicationLog;
import com.example.demo.modules.system.services.IApplicationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller cung cấp 3 API RESTful để Admin giám sát, thống kê
 * và dọn dẹp Application Logs (log lỗi nội bộ của hệ thống Backend).
 *
 * KHÔNG liên quan đến UptimeLogs (log giám sát API của người dùng).
 */
@RestController
@RequestMapping("/api/v1/admin/system-logs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ApplicationLogController {

    private final IApplicationLogService applicationLogService;

    /**
     * API 1: Lấy danh sách Application Logs với phân trang và bộ lọc.
     * GET /api/v1/admin/system-logs
     */
    @GetMapping
    public ResponseEntity<Page<ApplicationLog>> getLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(defaultValue = "ALL") String level,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "ALL") String timeRange) {
        return ResponseEntity.ok(applicationLogService.getLogs(page, size, level, keyword, timeRange));
    }

    /**
     * API 2: Lấy số liệu thống kê logs trong ngày hôm nay.
     * GET /api/v1/admin/system-logs/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getTodayStats() {
        return ResponseEntity.ok(applicationLogService.getTodayStats());
    }

    /**
     * API 3: Dọn dẹp log thủ công — xóa các log cũ hơn retentionDays.
     * DELETE /api/v1/admin/system-logs/clear
     */
    @DeleteMapping("/clear")
    public ResponseEntity<Map<String, Object>> clearLogs(
            @RequestParam(defaultValue = "30") int retentionDays) {
        return ResponseEntity.ok(applicationLogService.clearOldLogs(retentionDays));
    }

    /**
     * API 4: [Endpoint Kiểm thử] Sinh log thử nghiệm để kiểm tra luồng RabbitMQ và DB.
     * GET /api/v1/admin/system-logs/test-generate
     */
    @GetMapping("/test-generate")
    public ResponseEntity<String> testGenerateLogs() {
        org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ApplicationLogController.class);

        // 1. Log nội bộ của com.example.demo
        log.warn("TEST INTERNAL WARNING: Đây là log cảnh báo giả lập của com.example.demo");
        log.error("TEST INTERNAL ERROR: Đây là log lỗi giả lập của com.example.demo", 
                new RuntimeException("Simulated internal error exception"));

        // 2. Log giả lập từ package bên ngoài (ví dụ Spring Data Redis)
        org.slf4j.Logger redisLogger = org.slf4j.LoggerFactory.getLogger("org.springframework.data.redis.RedisConnectionFailureException");
        redisLogger.warn("TEST EXTERNAL WARNING: Giả lập lỗi Redis server không phản hồi!");
        redisLogger.error("TEST EXTERNAL ERROR: Giả lập lỗi Redis connection pool bị cạn kiệt!");

        // 3. Log giả lập từ RabbitMQ (Cần bị LOẠI TRỪ để tránh vòng lặp đệ quy vô hạn)
        org.slf4j.Logger rabbitLogger = org.slf4j.LoggerFactory.getLogger("org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer");
        rabbitLogger.error("TEST RABBIT ERROR: Giả lập lỗi kết nối RabbitMQ (Log này tuyệt đối không được ghi vào DB!)");

        return ResponseEntity.ok("Simulated logs generated successfully! Hãy gọi lại API lấy danh sách để kiểm tra kết quả.");
    }
}
