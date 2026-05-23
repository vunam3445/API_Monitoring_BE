package com.example.demo.modules.system.config;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cấu hình RabbitMQ Queue dành riêng cho việc thu thập
 * Application Log (log lỗi nội bộ của hệ thống Backend).
 * Queue này hoàn toàn độc lập với MonitorMQConfig (queue cho monitor của user).
 */
@Configuration
public class ApplicationLogMQConfig {

    public static final String QUEUE_NAME = "system.logs.queue";

    @Bean
    public Queue applicationLogQueue() {
        // durable = true: không mất message ngay cả khi RabbitMQ restart
        return new Queue(QUEUE_NAME, true);
    }
}
