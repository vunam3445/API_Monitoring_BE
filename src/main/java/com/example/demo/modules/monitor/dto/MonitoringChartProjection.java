package com.example.demo.modules.monitor.dto;

import java.time.LocalDateTime;

public interface MonitoringChartProjection {
    LocalDateTime getTime();
    Double getAvgResponseTimeMs();
    Double getErrorRatePercent();
    Long getTotalChecks();
    Long getFailedChecks();
}
