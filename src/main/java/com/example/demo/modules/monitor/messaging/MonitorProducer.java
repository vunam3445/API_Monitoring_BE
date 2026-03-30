package com.example.demo.modules.monitor.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Producer đẩy message vào RabbitMQ khi một monitor đến hạn kiểm tra.
 *
 * Single Responsibility: Chỉ lo việc gửi message, không chứa logic lock hay query DB.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MonitorProducer {

    private final RabbitTemplate rabbitTemplate;

    /**
     * Gửi yêu cầu thực thi kiểm tra monitor vào hàng đợi.
     *
     * @param monitorId ID của monitor cần kiểm tra
     */
    public void sendExecutionJob(String monitorId) {
        MonitorExecutionMessage message = MonitorExecutionMessage.builder()
                .monitorId(monitorId)
                .scheduledAt(LocalDateTime.now())
                .build();

        rabbitTemplate.convertAndSend(
                MonitorMQConfig.EXCHANGE_NAME,
                MonitorMQConfig.ROUTING_KEY,
                message
        );

        log.info("Sent execution job for monitor: {}", monitorId);
    }
}
