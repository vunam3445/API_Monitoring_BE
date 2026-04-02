package com.example.demo.modules.alert.controllers;

import com.example.demo.modules.alert.dto.AlertConfigResponse;
import com.example.demo.modules.alert.dto.CreateAlertConfigRequest;
import com.example.demo.modules.alert.services.IAlertConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/monitors")
@RequiredArgsConstructor
public class AlertConfigController {

    private final IAlertConfigService alertConfigService;

    @GetMapping("/{monitorId}/alert-configs")
    public ResponseEntity<List<AlertConfigResponse>> getAll(@PathVariable UUID monitorId) {
        return ResponseEntity.ok(alertConfigService.findAllByMonitorId(monitorId));
    }

    @PostMapping("/{monitorId}/alert-configs")
    public ResponseEntity<AlertConfigResponse> create(
            @PathVariable UUID monitorId, 
            @Valid @RequestBody CreateAlertConfigRequest request) {
        return ResponseEntity.ok(alertConfigService.create(monitorId, request));
    }

    @PutMapping("/alert-configs/{id}")
    public ResponseEntity<AlertConfigResponse> update(
            @PathVariable UUID id, 
            @Valid @RequestBody CreateAlertConfigRequest request) {
        return ResponseEntity.ok(alertConfigService.update(id, request));
    }

    @PatchMapping("/alert-configs/{id}/toggle")
    public ResponseEntity<Void> toggle(@PathVariable UUID id) {
        alertConfigService.toggle(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/alert-configs/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        alertConfigService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
