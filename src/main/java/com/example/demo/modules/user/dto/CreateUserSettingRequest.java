package com.example.demo.modules.user.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserSettingRequest {

    @NotNull(message = "UserId không được để trống")
    private UUID userId;

    // --- ALERT RULES ---
    @Min(value = 500, message = "Timeout tối thiểu là 500ms")
    @Max(value = 60000, message = "Timeout tối đa là 60s")
    private Integer defaultTimeoutMs = 5000;

    @PositiveOrZero(message = "Latency không được là số âm")
    private Integer defaultLatencyMs = 300;

    @Min(0) @Max(100)
    private Integer defaultErrorRate = 5;

    @Min(1) @Max(10)
    private Integer defaultFailCount = 3;

    // --- NOTIFICATION SETTINGS ---
    private boolean emailAlertsEnabled = true;

    @Email(message = "Email nhận thông báo không hợp lệ")
    private String alertEmail;

    private boolean slackEnabled = false;

    private String slackWebhookUrl;

    private boolean telegramEnabled = false;

    private String telegramChatId;

    // --- MONITORING SETTINGS ---
    @Min(value = 30, message = "Tần suất kiểm tra tối thiểu là 30 giây")
    private Integer checkInterval = 300;

    @Min(0) @Max(5)
    private Integer retryAttempts = 2;

    private boolean regionalMonitoringEnabled = false;

    // --- API HEALTH CALCULATION ---
    @Pattern(regexp = "^(24h|7d|30d)$", message = "Uptime window chỉ chấp nhận: 24h, 7d, 30d")
    private String uptimeWindow = "24h";

    @Pattern(regexp = "^(MEAN|P95|MEDIAN)$", message = "Thuật toán tính trung bình không hợp lệ")
    private String latencyAveraging = "MEAN";
}