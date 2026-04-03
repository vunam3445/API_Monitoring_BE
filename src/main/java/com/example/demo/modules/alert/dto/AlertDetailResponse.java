package com.example.demo.modules.alert.dto;

import com.example.demo.modules.alert.enums.IncidentSeverity;
import com.example.demo.modules.alert.enums.IncidentStatus;
import com.example.demo.modules.alert.enums.IncidentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertDetailResponse {
    private UUID id;
    private String title;
    private String message;
    private IncidentType alertType;
    private IncidentSeverity severity;
    private IncidentStatus status;
    
    // Monitor info
    private UUID monitorId;
    private String monitorName;
    private String monitorUrl;
    
    // Timeline
    private LocalDateTime triggeredAt;
    private LocalDateTime lastSeenAt;
    private LocalDateTime resolvedAt;
    private Integer consecutiveFailCount;
    private Long avgLatencyMs;
    private Integer lastStatusCode;
    
    // History
    private List<DeliveryLogResponse> deliveryHistory;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeliveryLogResponse {
        private UUID id;
        private String channel;
        private String destination;
        private String status;
        private LocalDateTime sentAt;
        private String errorMessage;
    }
}
