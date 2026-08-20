package com.example.demo.modules.notification.dto;

import java.util.UUID;

/**
 * Event DTO truyền qua RabbitMQ khi Admin gửi thông báo.
 * Dùng Java Record để đảm bảo immutable và tự sinh getter.
 */
public record NotificationBroadcastEvent(
        UUID notificationId,
        String title,
        String content,
        String targetType,   // ALL | PLAN | SINGLE
        String targetValue,  // email hoặc tên gói, null nếu ALL
        String level,        // INFO | WARNING | SYSTEM
        boolean sendWeb,
        boolean sendEmail
) {}
