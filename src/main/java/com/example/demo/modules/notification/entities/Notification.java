package com.example.demo.modules.notification.entities;

import com.example.demo.modules.notification.enums.NotificationLevel;
import com.example.demo.modules.notification.enums.TargetType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Lưu trữ thông tin gốc của một thông báo mà Admin đã phát.
 * Mỗi bản ghi tương ứng với một lần Admin gửi thông báo.
 */
@Entity
@Table(name = "notifications")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 20)
    private TargetType targetType;

    /**
     * Giá trị đích:
     * - Nếu targetType = SINGLE: email người nhận
     * - Nếu targetType = PLAN: tên gói (FREE, PRO, ENTERPRISE)
     * - Nếu targetType = ALL: null
     */
    @Column(name = "target_value", length = 255)
    private String targetValue;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationLevel level;

    /** Có gửi kênh Web (lưu vào bảng user_notifications để hiển thị trong app) không */
    @Column(name = "send_web", nullable = false)
    @Builder.Default
    private boolean sendWeb = true;

    /** Có gửi kênh Email (qua SMTP Brevo) không */
    @Column(name = "send_email", nullable = false)
    @Builder.Default
    private boolean sendEmail = true;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
