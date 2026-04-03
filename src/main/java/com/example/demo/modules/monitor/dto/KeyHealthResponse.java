package com.example.demo.modules.monitor.dto;

import com.example.demo.modules.monitor.enums.MonitorStatus;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class KeyHealthResponse {
    private UUID monitorId;
    private String monitorName;
    private String endpoint;
    private MonitorStatus currentStatus;
    private Integer latencyMs;
    private Double uptimePercent;
    private List<Integer> miniTrendData; // Latest latency points for sparkline
}
