package com.example.demo.modules.alert.services;

import com.example.demo.modules.alert.entities.Incident;
import com.example.demo.modules.alert.enums.AlertChannelType;

/**
 * Strategy Pattern: Contract chung cho mọi kênh gửi thông báo.
 *
 * Để thêm kênh mới (Telegram, Discord, SMS...):
 * 1. Implement interface này.
 * 2. Annotate với @Service — Spring tự động đăng ký vào List<NotificationStrategy>.
 * 3. KHÔNG cần sửa AlertNotificationDispatcher.
 */
public interface NotificationStrategy {

    /**
     * Kênh mà strategy này xử lý.
     */
    AlertChannelType supportedChannel();

    /**
     * Gửi thông báo khi có incident mới.
     *
     * @param destination Địa chỉ nhận (email, webhook URL, chat ID...)
     * @param incident    Thông tin sự cố
     */
    void sendIncident(String destination, Incident incident);

    /**
     * Gửi thông báo khi incident được resolve.
     *
     * @param destination Địa chỉ nhận
     * @param incident    Thông tin sự cố đã giải quyết
     */
    void sendRecovery(String destination, Incident incident);
}
