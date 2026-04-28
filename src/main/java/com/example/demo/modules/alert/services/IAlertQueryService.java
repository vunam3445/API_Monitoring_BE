package com.example.demo.modules.alert.services;

import com.example.demo.modules.alert.dto.AlertDetailResponse;
import com.example.demo.modules.alert.dto.AlertListResponse;
import com.example.demo.modules.alert.enums.IncidentSeverity;
import com.example.demo.modules.alert.enums.IncidentStatus;
import com.example.demo.modules.alert.enums.IncidentType;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.UUID;

public interface IAlertQueryService {
    AlertDetailResponse findById(UUID userId, UUID id);

    AlertListResponse findAll(UUID userId, String search, IncidentStatus status,
            IncidentSeverity severity, IncidentType type,
            LocalDateTime from, LocalDateTime to, Pageable pageable);

    byte[] exportCsv(UUID userId, IncidentStatus status, IncidentSeverity severity,
            IncidentType type, LocalDateTime from, LocalDateTime to);
}
