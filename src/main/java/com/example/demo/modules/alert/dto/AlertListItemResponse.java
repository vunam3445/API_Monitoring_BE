package com.example.demo.modules.alert.dto;

import com.example.demo.modules.alert.enums.IncidentSeverity;
import com.example.demo.modules.alert.enums.IncidentStatus;
import com.example.demo.modules.alert.enums.IncidentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertListItemResponse {
    private UUID id;
    private LocalDateTime time;
    private String apiName;
    private String endpoint;
    private IncidentType alertType;
    private IncidentSeverity severity;
    private IncidentStatus status;
    private String message;
}
