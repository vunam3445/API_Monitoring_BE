package com.example.demo.modules.monitor.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse {
    private UUID id;
    private String name;
    private String url;
    private String method;

    private Map<String, Object> auth;
    private List<Map<String, String>> headers; // Chuyển từ Map (Entity) về List cho UI
    private List<Map<String, String>> queryParams;
    private String body;

    private Integer checkInterval;
    private String expectedStatusCodes;
    private Integer maxResponseTimeMs;

    private Boolean isActive;
    private Boolean isMuted;

    // Các trường trạng thái thời gian thực
    private String lastStatus;
    private Integer lastLatencyMs;
    private String lastErrorMessage;
    private LocalDateTime lastCheckAt;
    private Integer consecutiveFailures;
    private LocalDateTime nextCheckAt;
    private LocalDateTime createdAt;

    // Bạn có thể thêm trường tính toán uptime ở đây
    private Double uptimePercentage;
}