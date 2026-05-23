package com.example.demo.modules.monitor.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserMonitorStatsDto {
    private int totalMonitor;
    private int totalActiveMonitor;
}
