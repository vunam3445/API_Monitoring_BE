package com.example.demo.modules.monitor.dto;

import com.example.demo.modules.monitor.enums.MonitorStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonitoringSummaryResponse {
    private long totalMonitors;
    private long healthyCount;
    private long warningCount;
    private long downCount;
    private long pausedCount;
    private long upCount;
    private double uptimePercentOverall;
}
