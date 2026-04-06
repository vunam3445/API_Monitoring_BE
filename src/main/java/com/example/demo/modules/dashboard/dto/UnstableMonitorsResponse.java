package com.example.demo.modules.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnstableMonitorsResponse {
    private String range;
    private List<UnstableMonitorItemResponse> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UnstableMonitorItemResponse {
        private int rank;
        private UUID monitorId;
        private String monitorName;
        private long downtimeMinutes;
        private long incidentCount;
        private double errorRate;
        private double stabilityScore;
        private String currentStatus;
        private String statusColor;
    }
}
