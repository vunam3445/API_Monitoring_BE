package com.example.demo.modules.alert.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertSummaryResponse {
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SummaryItem {
        private Long value;
        private Double changePercent;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActiveItem {
        private Long value;
        private Long urgentCount;
    }

    private SummaryItem totalAlerts;
    private ActiveItem activeAlerts;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CriticalItem {
        private Long value;
        private Boolean actionRequired;
    }
    private CriticalItem criticalAlerts;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResolvedItem {
        private Long value;
        private Double successRate;
    }
    private ResolvedItem resolvedAlerts;
}
