package com.example.demo.modules.subscription.services;

import com.example.demo.modules.alert.services.EmailSenderService;
import com.example.demo.modules.subscription.config.SubscriptionExpiryMQConfig;
import com.example.demo.modules.subscription.dto.SubscriptionExpiryEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionExpiryConsumer {

    private final EmailSenderService emailSenderService;

    @RabbitListener(queues = SubscriptionExpiryMQConfig.EXPIRY_QUEUE)
    public void consumeExpiryEvent(SubscriptionExpiryEvent event) {
        log.info("[SubscriptionExpiryConsumer] Nhận sự kiện nhắc gia hạn gửi cho email: {}", event.userEmail());
        try {
            emailSenderService.sendSubscriptionExpiryEmail(
                    event.userEmail(),
                    event.userName(),
                    event.planName(),
                    event.expiryDate()
            );
            log.info("[SubscriptionExpiryConsumer] Đã gửi mail nhắc gia hạn thành công cho email: {}", event.userEmail());
        } catch (Exception e) {
            log.error("[SubscriptionExpiryConsumer] Lỗi khi xử lý gửi email cho {}: {}", event.userEmail(), e.getMessage());
            throw e; // Throw lỗi để kích hoạt cơ chế retry của RabbitMQ
        }
    }
}
