package com.example.demo.modules.dashboard.controllers;

import com.example.demo.modules.dashboard.dto.SystemHealthResponse;
import com.example.demo.modules.dashboard.usecase.GetSystemHealthUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class SystemHealthController {

    private final GetSystemHealthUseCase getSystemHealthUseCase;
    private final List<byte[]> memoryStressor = new ArrayList<>();

    @GetMapping("/system-health")
    public ResponseEntity<SystemHealthResponse> getSystemHealth() {
        SystemHealthResponse response = getSystemHealthUseCase.execute();
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint để ép CPU lên cao phục vụ testing.
     */
    @GetMapping("/test/stress-cpu")
    public ResponseEntity<String> stressCpu(@RequestParam(defaultValue = "4") int threads) {
        for (int i = 0; i < threads; i++) {
            new Thread(() -> {
                while (true) {
                    // Thực hiện các phép tính toán phức tạp để chiếm CPU
                    Math.tan(Math.atan(Math.tan(Math.atan(Math.random()))));
                }
            }).start();
        }
        return ResponseEntity.ok("Đang ép CPU với " + threads + " luồng...");
    }

    /**
     * Endpoint để ép RAM (Heap) lên cao phục vụ testing.
     */
    @GetMapping("/test/stress-ram")
    public ResponseEntity<String> stressRam() {
        new Thread(() -> {
            try {
                while (true) {
                    byte[] b = new byte[1024 * 1024 * 100]; // Thêm 100MB mỗi giây
                    memoryStressor.add(b);
                    Thread.sleep(1000);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
        return ResponseEntity.ok("Đang ép RAM (100MB/s)...");
    }

    /**
     * Giải phóng bộ nhớ đã ép.
     */
    @GetMapping("/test/clear-ram")
    public ResponseEntity<String> clearRam() {
        memoryStressor.clear();
        System.gc(); // Gợi ý JVM giải phóng bộ nhớ
        return ResponseEntity.ok("Đã xóa bộ nhớ stress test.");
    }
}
