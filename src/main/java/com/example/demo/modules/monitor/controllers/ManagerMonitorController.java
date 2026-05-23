package com.example.demo.modules.monitor.controllers;

import com.example.demo.common.security.annotations.IsAdmin;
import com.example.demo.modules.monitor.dto.ApiResponse;
import com.example.demo.modules.monitor.dto.MonitorFilterCriteria;
import com.example.demo.modules.monitor.dto.MonitorStatisticsDTO;
import com.example.demo.modules.monitor.services.IManagerMonitorService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/monitors")
@RequiredArgsConstructor
@IsAdmin
public class ManagerMonitorController {

    private final IManagerMonitorService managerMonitorService;

    /**
     * Lấy danh sách tất cả các monitor dành cho admin (hỗ trợ search và filter
     * động)
     */
    @GetMapping
    public ResponseEntity<Page<ApiResponse>> getAllMonitors(
            MonitorFilterCriteria criteria,
            Pageable pageable) {
        return ResponseEntity.ok(managerMonitorService.getAllMonitors(criteria, pageable));
    }

    @GetMapping("/stats")
    public ResponseEntity<MonitorStatisticsDTO> getStatistics() {
        return ResponseEntity.ok(managerMonitorService.getMonitorStatistics());
    }

    @PutMapping("/{monitorId}/block")
    public ResponseEntity<Boolean> blockMonitor(@PathVariable UUID monitorId) {
        return ResponseEntity.ok(managerMonitorService.blockMonitor(monitorId));
    }
}
