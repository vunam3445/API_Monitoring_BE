package com.example.demo.modules.notification.services;

import com.example.demo.modules.notification.dto.UserNotificationResponse;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.util.UUID;

public interface NotificationSseService {
    /**
     * Đăng ký kết nối SSE cho người dùng đang online.
     */
    SseEmitter subscribe(UUID userId);

    /**
     * Đẩy thông báo thời gian thực đến một người dùng cụ thể nếu họ online.
     */
    void sendNotification(UUID userId, UserNotificationResponse notification);

    /**
     * Ngắt kết nối toàn bộ hệ thống (dọn dẹp).
     */
    void closeAll();
}
