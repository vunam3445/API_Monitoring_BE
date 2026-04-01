package com.example.demo.modules.monitor.dto;

import com.example.demo.modules.monitor.enums.MonitorStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MonitoringSummaryResponse {
    private long totalMonitors;
    private long healthyCount;
    private long warningCount;
    private long downCount;
    private long pausedCount;
    private long upCount;
    private double uptimePercentOverall;
}
