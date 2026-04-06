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
public class ErrorRateResponse {
    private String range;
    private List<ErrorRateItemResponse> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorRateItemResponse {
        private UUID monitorId;
        private String monitorName;
        private double errorRate;
        private long totalChecks;
        private long failedChecks;
        private String severity; // LOW, MEDIUM, HIGH
    }
}
