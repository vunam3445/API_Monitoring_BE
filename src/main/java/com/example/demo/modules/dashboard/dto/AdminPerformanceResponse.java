package com.example.demo.modules.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminPerformanceResponse {
    private String avgResponseTime;
    private String uptimePercentage;
    private String errorRate;
    private List<Double> chartData;
}
