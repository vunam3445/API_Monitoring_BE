package com.example.demo.modules.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminInfrastructureResponse {
    private WorkerStatus workers;
    private String dbLoad;
    private String serverUptime;
    private QueueStatus queueStatus;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WorkerStatus {
        private int active;
        private int total;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QueueStatus {
        private String label;
        private String type; // HEALTHY, WARNING, CRITICAL
    }
}
