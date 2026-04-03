package com.example.demo.modules.monitor.controllers;

import com.example.demo.common.base.BaseController;
import com.example.demo.common.security.annotations.IsOwnerOrAdmin;
import com.example.demo.modules.monitor.dto.ApiResponse;
import com.example.demo.modules.monitor.dto.CreateApiRequest;
import com.example.demo.modules.monitor.dto.UpdateApiRequest;

import com.example.demo.modules.monitor.services.IMonitorService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/Apis")
public class MonitorController extends BaseController<CreateApiRequest, UpdateApiRequest, ApiResponse, UUID> {

    private final IMonitorService monitorService;

    public MonitorController (IMonitorService service) { 
        super(service);
        this.monitorService = service;
    }

    @GetMapping("/user/{id}")
    public ResponseEntity<Page<ApiResponse>> getApisByUser(
            @PathVariable UUID id,
            @RequestParam(required = false) String lastStatus,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) String search,
            Pageable pageable) {
        Page<ApiResponse> result = monitorService.findAllByUserId(id, lastStatus, isActive, search, pageable);
        return ResponseEntity.ok(result);
    }

    @PutMapping("/{id}/status")
    @IsOwnerOrAdmin(entityName = "Monitor")
    public ResponseEntity<Boolean> updateMonitorStatus(@PathVariable UUID id) {
        return ResponseEntity.ok(monitorService.updateAPIStatus(id));
    }

    @PostMapping("/{id}/retry")
    @IsOwnerOrAdmin(entityName = "Monitor")
    public ResponseEntity<Boolean> retryMonitor(@PathVariable UUID id) {
        return ResponseEntity.ok(monitorService.retry(id));
    }
}
