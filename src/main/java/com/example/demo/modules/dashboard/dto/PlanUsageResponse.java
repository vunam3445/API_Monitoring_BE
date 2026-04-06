package com.example.demo.modules.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanUsageResponse {
    private String planName;
    private int monitorLimit;
    private int usedMonitors;
    private int remainingMonitors;
    private double usagePercentage;
    private boolean canUpgrade;
}
