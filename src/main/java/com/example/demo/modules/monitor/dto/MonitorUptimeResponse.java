package com.example.demo.modules.monitor.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class MonitorUptimeResponse {
    private UUID monitorId;
    private String range;
    private Double uptimePercent;
    private List<UptimeStatusPoint> statusHistory;
    
    @Data
    @Builder
    public static class UptimeStatusPoint {
        private LocalDateTime time;
        private Boolean isUp;
        private Integer latencyMs;
    }
}
