package com.example.demo.modules.notification.services;

import com.example.demo.modules.notification.dto.NotificationResponse;
import com.example.demo.modules.notification.dto.SendNotificationRequest;
import com.example.demo.modules.notification.dto.UserNotificationResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface NotificationService {

    /** Admin: Lưu thông báo vào DB và đẩy sự kiện vào RabbitMQ */
    NotificationResponse sendNotification(SendNotificationRequest request);

    /** Admin: Lấy lịch sử thông báo đã phát */
    Page<NotificationResponse> getAdminHistory(Pageable pageable);

    /** User: Lấy danh sách thông báo của mình */
    Page<UserNotificationResponse> getUserNotifications(UUID userId, boolean unreadOnly, Pageable pageable);

    /** User: Đếm số thông báo chưa đọc */
    long countUnread(UUID userId);

    /** User: Đánh dấu một thông báo là đã đọc */
    void markAsRead(UUID userNotificationId, UUID userId);

    /** User: Đánh dấu tất cả thông báo là đã đọc */
    void markAllAsRead(UUID userId);
}
