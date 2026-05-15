package com.example.demo.modules.alert.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertDeliveryStatsDTO {
    private double successRate;
    private long totalFailures24h;
    private String mostCommonError;
    private long queueDepth; // PENDING count
    private double avgLatencyMs;
    private long totalSent24h;
}
