package com.example.demo.modules.system.controllers;

import com.example.demo.modules.system.services.IAdminSystemService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final IAdminSystemService adminSystemService;
    private final com.example.demo.modules.dashboard.services.IAdminDashboardService adminDashboardService;

    @GetMapping("/stats")
    public ResponseEntity<?> getStats() {
        return ResponseEntity.ok(adminDashboardService.getGlobalStats());
    }

    @GetMapping("/charts/response-time")
    public ResponseEntity<?> getResponseTimeTrend(@RequestParam(defaultValue = "1d") String range) {
        return ResponseEntity.ok(adminDashboardService.getGlobalResponseTimeTrend(range));
    }

    @GetMapping("/charts/uptime")
    public ResponseEntity<?> getGlobalUptime(@RequestParam(defaultValue = "1d") String range) {
        return ResponseEntity.ok(adminDashboardService.getGlobalUptime(range));
    }

    @GetMapping("/charts/methods")
    public ResponseEntity<?> getMethodDistribution(@RequestParam(defaultValue = "1d") String range) {
        return ResponseEntity.ok(adminDashboardService.getGlobalMethodDistribution(range));
    }

    @PostMapping("/actions/pause")
    public ResponseEntity<?> togglePause(@RequestBody Map<String, Boolean> request) {
        boolean pause = request.getOrDefault("pause", false);
        adminSystemService.toggleGlobalPause(pause);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "isPaused", pause,
                "message", pause ? "Hệ thống đã tạm dừng giám sát toàn cục." : "Hệ thống đã tiếp tục giám sát."
        ));
    }

    @PostMapping("/actions/flush-queue")
    public ResponseEntity<?> flushQueue() {
        adminSystemService.flushMonitorQueue();
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Đã gửi lệnh xóa sạch hàng đợi công việc."
        ));
    }

    @GetMapping("/system-status")
    public ResponseEntity<?> getSystemStatus() {
        return ResponseEntity.ok(Map.of(
                "isGlobalPaused", adminSystemService.isGlobalPaused()
        ));
    }
}
