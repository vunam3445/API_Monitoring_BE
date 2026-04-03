package com.example.demo.modules.monitor.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class MonitoringSearchResponse {
    private List<ApiResponse> monitors;
    private List<MonitorLogRow> recentLogs;
}
