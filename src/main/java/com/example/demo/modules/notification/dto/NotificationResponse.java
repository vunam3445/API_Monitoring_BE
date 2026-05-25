package com.example.demo.modules.notification.dto;

import com.example.demo.modules.notification.entities.Notification;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO trả về thông tin gốc của thông báo (dành cho Admin xem lịch sử).
 */
@Data
@Builder
public class NotificationResponse {

    private UUID id;
    private String title;
    private String content;
    private String targetType;
    private String targetValue;
    private String level;
    private boolean sendWeb;
    private boolean sendEmail;
    private LocalDateTime createdAt;

    public static NotificationResponse from(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .title(n.getTitle())
                .content(n.getContent())
                .targetType(n.getTargetType().name())
                .targetValue(n.getTargetValue())
                .level(n.getLevel().name())
                .sendWeb(n.isSendWeb())
                .sendEmail(n.isSendEmail())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
