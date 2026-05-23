package com.example.demo.modules.monitor.controllers;

import com.example.demo.common.security.annotations.IsAdmin;
import com.example.demo.modules.monitor.dto.AdminUserMonitorStatsDto;
import com.example.demo.modules.monitor.services.IMonitorService;
import com.example.demo.modules.monitor.services.IMonitoringService;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/users/{userId}/monitors")
@IsAdmin
public class AdminManagerMonitorController {
    private final IMonitoringService service;

    @GetMapping
    public ResponseEntity<AdminUserMonitorStatsDto> getStats(@PathVariable UUID userId) {
        return ResponseEntity.ok(service.getMonitorStats(userId));
    }
}
