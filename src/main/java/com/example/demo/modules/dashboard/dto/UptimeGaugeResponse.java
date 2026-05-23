package com.example.demo.modules.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UptimeGaugeResponse {
    private String range;
    private double uptimePercentage;
    private long successfulChecks;
    private long totalChecks;
}
