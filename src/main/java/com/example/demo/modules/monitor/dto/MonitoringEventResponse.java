package com.example.demo.modules.monitor.dto;

import com.example.demo.modules.monitor.enums.MonitorEventType;
import com.example.demo.modules.monitor.enums.MonitorStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class MonitoringEventResponse {
    private LocalDateTime time;
    private String apiName;
    private MonitorEventType eventType;
    private Integer responseTime;
    private MonitorStatus status;
    private String message;
}
