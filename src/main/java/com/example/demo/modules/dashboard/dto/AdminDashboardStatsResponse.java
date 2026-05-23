package com.example.demo.modules.dashboard.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardStatsResponse {
    private long totalApis;
    private long healthy;
    private long warning;
    private long down;
    private double avgLatencyMs;
    private double checksPerMin;
}
