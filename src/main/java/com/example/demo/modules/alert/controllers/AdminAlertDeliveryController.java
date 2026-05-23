package com.example.demo.modules.alert.controllers;

import com.example.demo.modules.alert.dto.AlertDeliveryLogDTO;
import com.example.demo.modules.alert.dto.AlertDeliveryStatsDTO;
import com.example.demo.modules.alert.enums.AlertChannelType;
import com.example.demo.modules.alert.enums.AlertDeliveryStatus;
import com.example.demo.modules.alert.services.IAlertDeliveryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/delivery")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminAlertDeliveryController {

    private final IAlertDeliveryService deliveryService;

    @GetMapping("/stats")
    public ResponseEntity<AlertDeliveryStatsDTO> getStats() {
        return ResponseEntity.ok(deliveryService.getStats());
    }

    @GetMapping("/logs")
    public ResponseEntity<Page<AlertDeliveryLogDTO>> getLogs(
            @PageableDefault(size = 20, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false) AlertChannelType channel,
            @RequestParam(required = false) AlertDeliveryStatus status,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(deliveryService.getLogs(pageable, channel, status, search));
    }

    @PostMapping("/retry-all")
    public ResponseEntity<Void> retryAllFailed() {
        deliveryService.retryAllFailed24h();
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/retry")
    public ResponseEntity<Void> retrySpecific(@PathVariable UUID id) {
        deliveryService.retry(id);
        return ResponseEntity.noContent().build();
    }
}
