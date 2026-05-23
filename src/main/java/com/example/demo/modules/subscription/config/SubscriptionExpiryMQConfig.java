package com.example.demo.modules.subscription.config;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SubscriptionExpiryMQConfig {

    public static final String EXPIRY_QUEUE = "subscription.expiry.queue";

    @Bean
    public Queue subscriptionExpiryQueue() {
        return new Queue(EXPIRY_QUEUE, true); // Durable = true để giữ tin nhắn khi restart RabbitMQ
    }
}
