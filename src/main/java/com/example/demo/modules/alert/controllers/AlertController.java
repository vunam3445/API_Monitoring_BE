package com.example.demo.modules.alert.controllers;

import com.example.demo.common.security.ISecurityContextService;
import com.example.demo.modules.alert.dto.AdminUserMonitorAlertResponse;
import com.example.demo.modules.alert.dto.AlertListResponse;
import com.example.demo.modules.alert.dto.AlertSummaryResponse;
import com.example.demo.modules.alert.enums.IncidentSeverity;
import com.example.demo.modules.alert.enums.IncidentStatus;
import com.example.demo.modules.alert.enums.IncidentType;
import com.example.demo.modules.alert.services.IAlertQueryService;
import com.example.demo.modules.alert.services.IAlertSummaryService;
import com.example.demo.modules.alert.services.IIncidentService;
import com.example.demo.modules.user.entities.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final IAlertQueryService alertQueryService;
    private final IAlertSummaryService alertSummaryService;
    private final IIncidentService incidentService;
    private final ISecurityContextService securityContextService;

    @GetMapping("/summary")
    public ResponseEntity<AlertSummaryResponse> getSummary(@RequestParam(required = false) String range) {
        UUID userId = getCurrentUserId();
        return ResponseEntity.ok(alertSummaryService.getSummary(userId, range));
    }

    @GetMapping
    public ResponseEntity<AlertListResponse> getAll(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) IncidentStatus status,
            @RequestParam(required = false) IncidentSeverity severity,
            @RequestParam(required = false) IncidentType type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @PageableDefault(size = 10, sort = "triggeredAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {

        UUID userId = getCurrentUserId();
        return ResponseEntity.ok(alertQueryService.findAll(userId, search, status, severity, type, from, to, pageable));
    }

    @GetMapping("/users/{userId}")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AlertListResponse> getAlertsByAdmin(
            @PathVariable UUID userId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) IncidentStatus status,
            @RequestParam(required = false) IncidentSeverity severity,
            @RequestParam(required = false) IncidentType type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @PageableDefault(size = 10, sort = "triggeredAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {

        return ResponseEntity.ok(alertQueryService.findAll(userId, search, status, severity, type, from, to, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<com.example.demo.modules.alert.dto.AlertDetailResponse> getById(@PathVariable UUID id) {
        UUID userId = getCurrentUserId();
        return ResponseEntity.ok(alertQueryService.findById(userId, id));
    }

    @PatchMapping("/{id}/acknowledge")
    public ResponseEntity<Void> acknowledge(@PathVariable UUID id) {
        UUID userId = getCurrentUserId();
        incidentService.acknowledge(userId, id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/resolve")
    public ResponseEntity<Void> resolve(@PathVariable UUID id) {
        UUID userId = getCurrentUserId();
        incidentService.resolve(userId, id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        UUID userId = getCurrentUserId();
        incidentService.delete(userId, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
            @RequestParam(required = false) IncidentStatus status,
            @RequestParam(required = false) IncidentSeverity severity,
            @RequestParam(required = false) IncidentType type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {

        UUID userId = getCurrentUserId();
        byte[] csv = alertQueryService.exportCsv(userId, status, severity, type, from, to);

        return ResponseEntity.ok()
                .header("Content-Type", "text/csv")
                .header("Content-Disposition", "attachment; filename=\"alerts_export.csv\"")
                .body(csv);
    }

    private UUID getCurrentUserId() {
        return securityContextService.getCurrentUser()
                .map(User::getId)
                .orElseThrow(() -> new RuntimeException("User not authenticated"));
    }

    @GetMapping("/users/{userId}/monthly-stats")
    public ResponseEntity<AdminUserMonitorAlertResponse> countAlertsAndIncidentsInMonth(@PathVariable UUID userId) {
        return ResponseEntity.ok(alertSummaryService.countAlertsAndIncidentsInMonth(userId));
    }
}
