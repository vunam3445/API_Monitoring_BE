package com.example.demo.modules.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSettingResponse {

    private UUID userId;

    // --- ALERT RULES ---
    private Integer defaultTimeoutMs;
    private Integer defaultLatencyMs;
    private Integer defaultErrorRate;
    private Integer defaultFailCount;

    // --- NOTIFICATION SETTINGS ---
    private boolean emailAlertsEnabled;
    private String alertEmail;
    private boolean slackEnabled;
    private String slackWebhookUrl;
    private boolean telegramEnabled;
    private String telegramChatId;

    // --- MONITORING SETTINGS ---
    private Integer checkInterval;
    private Integer retryAttempts;
    private boolean regionalMonitoringEnabled;

    // --- API HEALTH CALCULATION ---
    private String uptimeWindow;
    private String latencyAveraging;
}