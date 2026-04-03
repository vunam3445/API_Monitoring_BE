package com.example.demo.modules.monitor.dto;

import com.example.demo.modules.monitor.enums.MonitorStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class MonitorOverviewResponse {
    private ApiResponse baseInfo;
    private MonitorStatus currentStatus;
    private LocalDateTime lastCheckTime;
    private Integer latestLatency;
    private Double uptimePercent;
    private List<MonitoringEventResponse> recentLogs;
    private List<MonitoringChartPoint> chartData;
}
