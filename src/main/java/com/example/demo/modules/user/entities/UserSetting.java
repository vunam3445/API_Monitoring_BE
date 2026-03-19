package com.example.demo.modules.user.entities;

import jakarta.persistence.*;
import lombok.Data;
import java.util.UUID;

@Entity
@Table(name = "user_settings")
@Data
public class UserSetting {

    @Id
    private UUID userId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    // --- GROUP 1: ALERT RULES (Ngưỡng cảnh báo) ---

    @Column(name = "default_timeout_ms")
    private Integer defaultTimeoutMs = 5000; // Thời gian chờ tối đa (ms) trước khi tính là lỗi

    @Column(name = "default_latency_ms")
    private Integer defaultLatencyMs = 300; // Ngưỡng cảnh báo nếu phản hồi chậm (ms)

    @Column(name = "default_error_rate")
    private Integer defaultErrorRate = 5; // Tỉ lệ lỗi cho phép (%) trước khi báo động

    @Column(name = "default_fail_count")
    private Integer defaultFailCount = 3; // Số lần lỗi liên tiếp để xác nhận web sập (Down)

    // --- GROUP 2: NOTIFICATION SETTINGS (Kênh thông báo) ---

    private boolean emailAlertsEnabled = true; // Bật/tắt gửi thông báo qua Email

    private String alertEmail; // Email nhận cảnh báo (nếu khác email đăng nhập)

    private boolean slackEnabled = false; // Bật/tắt kênh Slack

    @Column(columnDefinition = "TEXT")
    private String slackWebhookUrl; // Đường dẫn Webhook để bắn tin nhắn vào Channel Slack

    private boolean telegramEnabled = false; // Bật/tắt kênh Telegram

    private String telegramChatId; // ID cuộc trò chuyện nhận tin nhắn từ Bot

    // --- GROUP 3: MONITORING SETTINGS (Cấu hình vận hành) ---

    private Integer checkInterval = 300; // Tần suất kiểm tra mặc định (đơn vị: giây)

    private Integer retryAttempts = 2; // Số lần thử lại ngay lập tức khi gặp lỗi mạng tạm thời

    private boolean regionalMonitoringEnabled = false; // Bật/tắt kiểm tra từ nhiều vị trí (Global)

    // --- GROUP 4: API HEALTH CALCULATION (Cách tính toán chỉ số) ---

    private String uptimeWindow = "24h"; // Khoảng thời gian để tính % Uptime (24h, 7d, 30d)

    private String latencyAveraging = "MEAN"; // Thuật toán tính trung bình: MEAN (Trung bình cộng), P95, MEDIAN

    // --- GROUP 5: SYSTEM PREFERENCES (Giao diện & Ngôn ngữ) ---

//    private String interfaceTheme = "LIGHT"; // Giao diện người dùng: LIGHT, DARK, SYSTEM
//
//    private String displayLanguage = "en-US"; // Ngôn ngữ hiển thị hệ thống
//
//    private String timezone = "UTC"; // Múi giờ để hiển thị lịch sử Log chính xác
}