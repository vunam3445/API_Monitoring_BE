package com.example.demo.modules.monitor.dto;

import com.example.demo.modules.monitor.enums.MonitorEventType;
import com.example.demo.modules.monitor.enums.MonitorStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class MonitorLogRow {
    private Long logId;
    private UUID monitorId;
    private String monitorName;
    private LocalDateTime checkedAt;
    private Integer responseTimeMs;
    private Integer statusCode;
    private MonitorStatus status;
    private MonitorEventType eventType;
    private String message;
    private String errorMessage;
}
