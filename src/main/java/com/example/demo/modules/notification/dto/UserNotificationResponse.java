package com.example.demo.modules.notification.dto;

import com.example.demo.modules.notification.entities.UserNotification;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO trả về thông báo kèm trạng thái đọc của User hiện tại.
 */
@Data
@Builder
public class UserNotificationResponse {

    private UUID id;              // ID của UserNotification (dùng để đánh dấu đã đọc)
    private UUID notificationId;
    private String title;
    private String content;
    private String level;
    private boolean isRead;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;

    public static UserNotificationResponse from(UserNotification un) {
        return UserNotificationResponse.builder()
                .id(un.getId())
                .notificationId(un.getNotification().getId())
                .title(un.getNotification().getTitle())
                .content(un.getNotification().getContent())
                .level(un.getNotification().getLevel().name())
                .isRead(un.isRead())
                .readAt(un.getReadAt())
                .createdAt(un.getCreatedAt())
                .build();
    }
}
