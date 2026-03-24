package com.example.demo.modules.user.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.util.UUID;

@Data
public class UpdateUserSettingRequest {
    private UUID id;
    // --- ALERT RULES ---
    @Min(value = 500, message = "Timeout không được nhỏ hơn 500ms")
    private Integer defaultTimeoutMs;

    private Integer defaultLatencyMs;

    @Min(0) @Max(100)
    private Integer defaultErrorRate;

    private Integer defaultFailCount;

    // --- NOTIFICATION SETTINGS ---
    private Boolean emailAlertsEnabled;

    private String alertEmail;

    private Boolean slackEnabled;

    private String slackWebhookUrl;

    private Boolean telegramEnabled;

    private String telegramChatId;

    // --- MONITORING SETTINGS ---
    @Min(value = 60, message = "Tần suất kiểm tra tối thiểu là 60 giây")
    private Integer checkInterval;

    private Integer retryAttempts;

    private Boolean regionalMonitoringEnabled;

    // --- API HEALTH CALCULATION ---
    private String uptimeWindow;

    private String latencyAveraging;
}