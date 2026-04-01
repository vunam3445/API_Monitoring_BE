package com.example.demo.modules.monitor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonitoringChartPoint {
    private LocalDateTime time;
    private Double avgResponseTimeMs;
    private Double errorRatePercent;
    private Long totalChecks;
    private Long failedChecks;
}
