package com.example.demo.modules.notification.config;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Đăng ký hàng đợi RabbitMQ cho module Notification.
 * Queue durable = true để không mất message khi RabbitMQ restart.
 *
 * Topology đơn giản (default exchange):
 *   Producer → notification.broadcast.queue → NotificationBroadcastConsumer
 */
@Configuration
public class NotificationMQConfig {

    public static final String BROADCAST_QUEUE = "notification.broadcast.queue";

    @Bean
    public Queue notificationBroadcastQueue() {
        return new Queue(BROADCAST_QUEUE, true); // durable = true
    }
}
